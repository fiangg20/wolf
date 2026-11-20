package com.bases.wol

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val FILTER_ALL = ""
const val FILTER_FAV = "\u0000fav"

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx: Context = app
    val repo: Repo = Repo.get(app)

    val devices = repo.devices
    val settings = repo.settings
    val log = repo.log
    val schedules = repo.schedules

    val statuses = MutableStateFlow<Map<String, DeviceStatus>>(emptyMap())
    val filter = MutableStateFlow(FILTER_ALL)
    val editing = MutableStateFlow<Device?>(null)
    val editingIsNew = MutableStateFlow(true)
    val scan = MutableStateFlow(ScanState())
    val events = MutableSharedFlow<String>(extraBufferCapacity = 16)

    private val waitJobs = HashMap<String, Job>()
    private var scanJob: Job? = null

    private fun say(s: String) {
        events.tryEmit(s)
    }

    private fun setStatus(id: String, st: DeviceStatus) {
        statuses.update { it + (id to st) }
    }

    private fun logIt(device: String, action: String, ok: Boolean, message: String) {
        repo.addLog(LogEntry(System.currentTimeMillis(), device, action, ok, message))
    }

    // ---------- Waking ----------

    fun wake(d: Device, count: Int? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val s = repo.settings.value
            val n = count ?: s.packetCount
            val r = Wol.send(ctx, d, s, n)
            if (r.ok) markWoken(d.id)
            logIt(d.name, if (count != null) "Burst x$n" else "Wake", r.ok, r.message)
            say(if (r.ok) "Magic packet sent to ${d.name}" else "Failed: ${r.message}")
        }
    }

    fun wakeMany(list: List<Device>) {
        if (list.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val s = repo.settings.value
            var ok = 0
            for (d in list) {
                val r = Wol.send(ctx, d, s, s.packetCount)
                if (r.ok) {
                    ok++
                    markWoken(d.id)
                }
                logIt(d.name, "Wake all", r.ok, r.message)
                delay(50)
            }
            say("Sent to $ok of ${list.size} devices")
        }
    }

    private fun markWoken(id: String) {
        val now = System.currentTimeMillis()
        repo.updateDevices { l -> l.map { if (it.id == id) it.copy(lastWoken = now) else it } }
    }

    fun wakeAndWait(d: Device) {
        waitJobs[d.id]?.cancel()
        waitJobs[d.id] = viewModelScope.launch {
            val s = repo.settings.value
            val r = withContext(Dispatchers.IO) { Wol.send(ctx, d, s, s.packetCount) }
            if (r.ok) markWoken(d.id)
            logIt(d.name, "Wake & wait", r.ok, r.message)
            if (!r.ok) {
                say("Failed: ${r.message}")
                return@launch
            }
            if (d.host.isBlank()) {
                say("Packet sent. Add a host/IP to this device to wait for it.")
                return@launch
            }
            val start = SystemClock.elapsedRealtime()
            while (isActive) {
                val el = ((SystemClock.elapsedRealtime() - start) / 1000L).toInt()
                if (el > s.waitTimeoutSec) {
                    setStatus(d.id, DeviceStatus(DevState.OFFLINE))
                    logIt(d.name, "Wait", false, "Not online after ${s.waitTimeoutSec}s")
                    say("${d.name} did not come online in ${s.waitTimeoutSec}s")
                    return@launch
                }
                setStatus(d.id, DeviceStatus(DevState.WAKING, -1L, el))
                val p = withContext(Dispatchers.IO) { Wol.probe(d, s.pingTimeoutMs) }
                if (p.online) {
                    setStatus(d.id, DeviceStatus(DevState.ONLINE, p.latencyMs, el))
                    logIt(d.name, "Wait", true, "Online after ${el}s")
                    say("${d.name} is online")
                    return@launch
                }
                delay(2000)
            }
        }
    }

    fun stopWait(d: Device) {
        waitJobs[d.id]?.cancel()
        waitJobs.remove(d.id)
        setStatus(d.id, DeviceStatus(DevState.UNKNOWN))
    }

    // ---------- Status ----------

    fun checkStatus(d: Device, silent: Boolean = false) {
        if (d.host.isBlank()) {
            if (!silent) say("Add a host/IP to check status")
            return
        }
        if (statuses.value[d.id]?.state == DevState.WAKING) return
        if (!silent) setStatus(d.id, DeviceStatus(DevState.CHECKING))
        viewModelScope.launch(Dispatchers.IO) {
            val p = Wol.probe(d, repo.settings.value.pingTimeoutMs)
            if (statuses.value[d.id]?.state == DevState.WAKING) return@launch
            setStatus(d.id, DeviceStatus(if (p.online) DevState.ONLINE else DevState.OFFLINE, p.latencyMs))
            if (!silent) say(if (p.online) "${d.name} is online (${p.latencyMs} ms)" else "${d.name} is offline")
        }
    }

    fun refreshAll() {
        devices.value.filter { it.host.isNotBlank() }.forEach { checkStatus(it, silent = true) }
    }

    // ---------- Device CRUD ----------

    fun startEdit(d: Device, isNew: Boolean) {
        editingIsNew.value = isNew
        editing.value = d
    }

    fun newDevice() {
        val s = repo.settings.value
        startEdit(Device(port = s.defaultPort), true)
    }

    fun closeEdit() {
        editing.value = null
    }

    fun saveDevice(d: Device) {
        repo.updateDevices { l ->
            if (l.any { it.id == d.id }) l.map { if (it.id == d.id) d else it } else l + d
        }
        editing.value = null
    }

    fun deleteDevice(d: Device) {
        waitJobs[d.id]?.cancel()
        waitJobs.remove(d.id)
        repo.updateDevices { l -> l.filter { it.id != d.id } }
        val mine = repo.schedules.value.filter { it.deviceId == d.id }
        mine.forEach { Scheduler.cancel(ctx, it.id) }
        repo.setSchedules(repo.schedules.value.filter { it.deviceId != d.id })
        statuses.update { it - d.id }
        say("Deleted ${d.name}")
    }

    fun duplicateDevice(d: Device) {
        val copy = d.copy(
            id = java.util.UUID.randomUUID().toString(),
            name = d.name + " copy",
            lastWoken = 0L,
        )
        repo.updateDevices { it + copy }
        say("Duplicated ${d.name}")
    }

    fun toggleFavorite(d: Device) {
        repo.updateDevices { l -> l.map { if (it.id == d.id) it.copy(favorite = !it.favorite) else it } }
    }

    fun move(d: Device, delta: Int) {
        repo.updateDevices { l ->
            val i = l.indexOfFirst { it.id == d.id }
            val j = i + delta
            if (i < 0 || j < 0 || j >= l.size) {
                l
            } else {
                val m = l.toMutableList()
                val t = m[i]
                m[i] = m[j]
                m[j] = t
                m
            }
        }
    }

    fun setFilter(f: String) {
        filter.value = f
    }

    fun updateSettings(f: (AppSettings) -> AppSettings) {
        repo.updateSettings(f)
    }

    fun clearLog() {
        repo.clearLog()
    }

    fun message(s: String) {
        say(s)
    }

    // ---------- Schedules ----------

    private val timeFmt = SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault())

    fun addSchedule(d: Device, triggerAt: Long, daily: Boolean) {
        val s = Schedule(repo.nextScheduleId(), d.id, triggerAt, daily)
        repo.setSchedules(repo.schedules.value + s)
        Scheduler.schedule(ctx, s)
        say("Will wake ${d.name} on ${timeFmt.format(Date(triggerAt))}" + if (daily) " (daily)" else "")
    }

    fun cancelSchedule(id: Int) {
        Scheduler.cancel(ctx, id)
        repo.setSchedules(repo.schedules.value.filter { it.id != id })
        say("Schedule cancelled")
    }

    // ---------- Backup ----------

    fun exportJson(): String {
        val root = JSONObject()
        root.put("app", "wolf")
        val arr = JSONArray()
        devices.value.forEach { arr.put(it.toJson()) }
        root.put("devices", arr)
        return root.toString(2)
    }

    fun importJson(text: String, replace: Boolean) {
        try {
            val t = text.trim()
            val arr = if (t.startsWith("[")) JSONArray(t) else JSONObject(t).getJSONArray("devices")
            val incoming = (0 until arr.length()).map { Device.fromJson(arr.getJSONObject(it)) }
            if (replace) {
                repo.setDevices(incoming)
                say("Imported ${incoming.size} devices")
            } else {
                val have = devices.value
                val fresh = incoming.filter { n ->
                    have.none { it.id == n.id || (n.mac.isNotBlank() && Wol.formatMac(it.mac) == Wol.formatMac(n.mac)) }
                }
                repo.setDevices(have + fresh)
                say("Imported ${fresh.size} new devices")
            }
        } catch (e: Exception) {
            say("That doesn't look like a Wolf backup")
        }
    }

    // ---------- Scan ----------

    fun startScan() {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            scan.value = ScanState(running = true, network = Wol.describeNetwork(ctx))
            try {
                val result = Wol.sweep(
                    ctx,
                    onProgress = { d, t -> scan.update { it.copy(done = d, total = t) } },
                    onFound = { h ->
                        scan.update { st ->
                            st.copy(found = (st.found + h).sortedBy { x -> x.ip.substringAfterLast('.').toIntOrNull() ?: 0 })
                        }
                    },
                )
                scan.update { it.copy(running = false, finished = true, found = result) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                scan.update { it.copy(running = false, error = e.message ?: "Scan failed") }
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scan.update { it.copy(running = false) }
    }
}
