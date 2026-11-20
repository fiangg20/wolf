package com.bases.wol

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.net.ConnectException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

data class SendResult(val ok: Boolean, val message: String)

data class Probe(val online: Boolean, val latencyMs: Long)

class SubnetInfo(val ip: ByteArray, val prefix: Int)

object Wol {

    // ---------- MAC / packet ----------

    fun parseMac(s: String): ByteArray? {
        val hex = s.filter { it.isLetterOrDigit() }
        if (hex.length != 12) return null
        if (!hex.all { it in "0123456789abcdefABCDEF" }) return null
        return ByteArray(6) { i -> hex.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
    }

    fun formatMac(s: String): String {
        val b = parseMac(s) ?: return s
        return b.joinToString(":") { String.format("%02X", it.toInt() and 0xFF) }
    }

    fun buildPacket(mac: ByteArray, secure: ByteArray?): ByteArray {
        val extra = secure?.size ?: 0
        val out = ByteArray(6 + 16 * 6 + extra)
        for (i in 0 until 6) out[i] = 0xFF.toByte()
        for (r in 0 until 16) System.arraycopy(mac, 0, out, 6 + r * 6, 6)
        if (secure != null) System.arraycopy(secure, 0, out, 6 + 16 * 6, secure.size)
        return out
    }

    // ---------- Network info ----------

    @Suppress("DEPRECATION")
    fun lanNetwork(ctx: Context): Network? {
        val cm = ctx.getSystemService(ConnectivityManager::class.java) ?: return null
        try {
            for (n in cm.allNetworks) {
                val c = cm.getNetworkCapabilities(n) ?: continue
                if (c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                ) return n
            }
        } catch (e: Exception) {
            // ignore
        }
        return null
    }

    fun subnetInfo(ctx: Context): SubnetInfo? {
        val cm = ctx.getSystemService(ConnectivityManager::class.java) ?: return null
        val net = lanNetwork(ctx) ?: cm.activeNetwork ?: return null
        val lp = cm.getLinkProperties(net) ?: return null
        for (la in lp.linkAddresses) {
            val a = la.address
            if (a is Inet4Address && !a.isLoopbackAddress) {
                return SubnetInfo(a.address, la.prefixLength)
            }
        }
        return null
    }

    private fun intToIp(i: Int): String =
        "${(i ushr 24) and 255}.${(i ushr 16) and 255}.${(i ushr 8) and 255}.${i and 255}"

    fun broadcastOf(ip: ByteArray, prefix: Int): String {
        val ipInt = ByteBuffer.wrap(ip).int
        val mask = if (prefix <= 0) 0 else (-1 shl (32 - prefix))
        return intToIp(ipInt or mask.inv())
    }

    fun describeNetwork(ctx: Context): String? {
        val info = subnetInfo(ctx) ?: return null
        val ip = InetAddress.getByAddress(info.ip).hostAddress ?: return null
        return "$ip/${info.prefix}"
    }

    fun targetFor(ctx: Context, d: Device, s: AppSettings): String {
        if (d.broadcast.isNotBlank()) return d.broadcast.trim()
        if (s.defaultBroadcast.isNotBlank()) return s.defaultBroadcast.trim()
        if (s.autoBroadcast) {
            val info = subnetInfo(ctx)
            if (info != null) return broadcastOf(info.ip, info.prefix)
        }
        return "255.255.255.255"
    }

    // ---------- Send magic packet ----------

    fun send(ctx: Context, d: Device, s: AppSettings, count: Int): SendResult {
        val mac = parseMac(d.mac) ?: return SendResult(false, "Invalid MAC address")
        var secure: ByteArray? = null
        if (d.secureOn.isNotBlank()) {
            secure = parseMac(d.secureOn) ?: return SendResult(false, "Invalid SecureOn password")
        }
        val packet = buildPacket(mac, secure)
        val target = targetFor(ctx, d, s)
        val n = count.coerceAtLeast(1)
        return try {
            val addr = InetAddress.getByName(target)
            val ports = if (s.alsoPort7 && d.port != 7) listOf(d.port, 7) else listOf(d.port)
            val sock = DatagramSocket()
            try {
                sock.broadcast = true
                val lan = lanNetwork(ctx)
                if (lan != null) {
                    try {
                        lan.bindSocket(sock)
                    } catch (e: Exception) {
                        // fall back to default route
                    }
                }
                for (i in 0 until n) {
                    for (p in ports) {
                        sock.send(DatagramPacket(packet, packet.size, addr, p))
                    }
                    if (i < n - 1) Thread.sleep(s.packetDelayMs.toLong().coerceAtLeast(0L))
                }
            } finally {
                sock.close()
            }
            SendResult(true, "Sent $n packet(s) to $target:${d.port}")
        } catch (e: Exception) {
            SendResult(false, e.message ?: e.javaClass.simpleName)
        }
    }

    // ---------- Probing ----------

    private fun tcpAlive(host: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            val sock = Socket()
            try {
                sock.connect(InetSocketAddress(host, port), timeoutMs)
            } finally {
                try {
                    sock.close()
                } catch (e: Exception) {
                    // ignore
                }
            }
            true
        } catch (e: ConnectException) {
            // "connection refused" still means the machine answered
            e.message?.contains("refused", ignoreCase = true) == true
        } catch (e: Exception) {
            false
        }
    }

    fun probe(d: Device, timeoutMs: Int): Probe {
        val host = d.host.trim()
        if (host.isEmpty()) return Probe(false, -1L)
        val start = System.nanoTime()
        val elapsed = { (System.nanoTime() - start) / 1_000_000L }
        try {
            if (d.checkPort > 0) {
                return if (tcpAlive(host, d.checkPort, timeoutMs)) Probe(true, elapsed()) else Probe(false, -1L)
            }
            val addr = InetAddress.getByName(host)
            if (addr.isReachable(timeoutMs)) return Probe(true, elapsed())
            for (p in intArrayOf(445, 3389, 22, 80, 443)) {
                if (tcpAlive(host, p, 300)) return Probe(true, elapsed())
            }
            return Probe(false, -1L)
        } catch (e: Exception) {
            return Probe(false, -1L)
        }
    }

    // ---------- Scan ----------

    private fun alive(ip: String): Long {
        val start = System.nanoTime()
        try {
            if (InetAddress.getByName(ip).isReachable(300)) return (System.nanoTime() - start) / 1_000_000L
        } catch (e: Exception) {
            // ignore
        }
        for (p in intArrayOf(445, 80, 22, 443, 3389, 8080, 62078)) {
            if (tcpAlive(ip, p, 200)) return (System.nanoTime() - start) / 1_000_000L
        }
        return -1L
    }

    private fun hostName(ip: String): String {
        val ex = Executors.newSingleThreadExecutor()
        return try {
            val name = ex.submit(Callable<String> { InetAddress.getByName(ip).hostName })
                .get(1500, TimeUnit.MILLISECONDS)
            if (name == ip) "" else name
        } catch (e: Exception) {
            ""
        } finally {
            ex.shutdownNow()
        }
    }

    private fun readArp(): Map<String, String> {
        return try {
            File("/proc/net/arp").readLines().drop(1).mapNotNull { line ->
                val p = line.trim().split(Regex("\\s+"))
                if (p.size >= 4 && p[3].contains(":") && p[3] != "00:00:00:00:00:00") p[0] to p[3].uppercase() else null
            }.toMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun sweep(
        ctx: Context,
        onProgress: (Int, Int) -> Unit,
        onFound: (FoundHost) -> Unit,
    ): List<FoundHost> = coroutineScope {
        val info = subnetInfo(ctx) ?: throw IllegalStateException("Connect to Wi-Fi or Ethernet first")
        val eff = maxOf(info.prefix, 24)
        val size = (1 shl (32 - eff)) - 2
        if (size < 1) return@coroutineScope emptyList<FoundHost>()
        val ipInt = ByteBuffer.wrap(info.ip).int
        val mask = -1 shl (32 - eff)
        val net = ipInt and mask
        val done = AtomicInteger(0)
        val sem = Semaphore(48)
        val found = java.util.Collections.synchronizedList(mutableListOf<FoundHost>())
        val jobs = (1..size).map { h ->
            async(Dispatchers.IO) {
                val addr = net + h
                if (addr != ipInt) {
                    val ip = intToIp(addr)
                    sem.withPermit {
                        val lat = alive(ip)
                        if (lat >= 0) {
                            val fh = FoundHost(ip, hostName(ip), "", lat)
                            found.add(fh)
                            onFound(fh)
                        }
                    }
                }
                onProgress(done.incrementAndGet(), size)
            }
        }
        jobs.awaitAll()
        val arp = readArp()
        found.map { it.copy(mac = arp[it.ip] ?: "") }
            .sortedBy { it.ip.substringAfterLast('.').toIntOrNull() ?: 0 }
    }
}
