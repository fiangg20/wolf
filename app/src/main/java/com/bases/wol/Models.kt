package com.bases.wol

import org.json.JSONObject
import java.util.UUID

data class Device(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val mac: String = "",
    val broadcast: String = "",
    val port: Int = 9,
    val host: String = "",
    val checkPort: Int = 0,
    val group: String = "",
    val secureOn: String = "",
    val icon: Int = 0,
    val favorite: Boolean = false,
    val lastWoken: Long = 0L,
    val notes: String = "",
) {
    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("id", id)
        o.put("name", name)
        o.put("mac", mac)
        o.put("broadcast", broadcast)
        o.put("port", port)
        o.put("host", host)
        o.put("checkPort", checkPort)
        o.put("group", group)
        o.put("secureOn", secureOn)
        o.put("icon", icon)
        o.put("favorite", favorite)
        o.put("lastWoken", lastWoken)
        o.put("notes", notes)
        return o
    }

    companion object {
        fun fromJson(o: JSONObject): Device {
            val rawId = o.optString("id", "")
            return Device(
                id = if (rawId.isBlank()) UUID.randomUUID().toString() else rawId,
                name = o.optString("name", ""),
                mac = o.optString("mac", ""),
                broadcast = o.optString("broadcast", ""),
                port = o.optInt("port", 9),
                host = o.optString("host", ""),
                checkPort = o.optInt("checkPort", 0),
                group = o.optString("group", ""),
                secureOn = o.optString("secureOn", ""),
                icon = o.optInt("icon", 0),
                favorite = o.optBoolean("favorite", false),
                lastWoken = o.optLong("lastWoken", 0L),
                notes = o.optString("notes", ""),
            )
        }
    }
}

data class AppSettings(
    val themeMode: Int = 0,          // 0 system, 1 light, 2 dark
    val dynamicColor: Boolean = true,
    val palette: Int = 0,
    val pureBlack: Boolean = false,
    val defaultPort: Int = 9,
    val defaultBroadcast: String = "",
    val autoBroadcast: Boolean = true,
    val packetCount: Int = 3,
    val packetDelayMs: Int = 100,
    val alsoPort7: Boolean = false,
    val burstCount: Int = 10,
    val waitTimeoutSec: Int = 120,
    val pingTimeoutMs: Int = 1500,
    val autoRefreshSec: Int = 0,
    val refreshOnOpen: Boolean = true,
    val haptics: Boolean = true,
    val confirmWakeAll: Boolean = true,
    val compactCards: Boolean = false,
    val sort: Int = 0,               // 0 manual, 1 name, 2 favorites first, 3 recently woken
    val maxLog: Int = 200,
) {
    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("themeMode", themeMode)
        o.put("dynamicColor", dynamicColor)
        o.put("palette", palette)
        o.put("pureBlack", pureBlack)
        o.put("defaultPort", defaultPort)
        o.put("defaultBroadcast", defaultBroadcast)
        o.put("autoBroadcast", autoBroadcast)
        o.put("packetCount", packetCount)
        o.put("packetDelayMs", packetDelayMs)
        o.put("alsoPort7", alsoPort7)
        o.put("burstCount", burstCount)
        o.put("waitTimeoutSec", waitTimeoutSec)
        o.put("pingTimeoutMs", pingTimeoutMs)
        o.put("autoRefreshSec", autoRefreshSec)
        o.put("refreshOnOpen", refreshOnOpen)
        o.put("haptics", haptics)
        o.put("confirmWakeAll", confirmWakeAll)
        o.put("compactCards", compactCards)
        o.put("sort", sort)
        o.put("maxLog", maxLog)
        return o
    }

    companion object {
        fun fromJson(o: JSONObject): AppSettings {
            val d = AppSettings()
            return AppSettings(
                themeMode = o.optInt("themeMode", d.themeMode),
                dynamicColor = o.optBoolean("dynamicColor", d.dynamicColor),
                palette = o.optInt("palette", d.palette),
                pureBlack = o.optBoolean("pureBlack", d.pureBlack),
                defaultPort = o.optInt("defaultPort", d.defaultPort),
                defaultBroadcast = o.optString("defaultBroadcast", d.defaultBroadcast),
                autoBroadcast = o.optBoolean("autoBroadcast", d.autoBroadcast),
                packetCount = o.optInt("packetCount", d.packetCount),
                packetDelayMs = o.optInt("packetDelayMs", d.packetDelayMs),
                alsoPort7 = o.optBoolean("alsoPort7", d.alsoPort7),
                burstCount = o.optInt("burstCount", d.burstCount),
                waitTimeoutSec = o.optInt("waitTimeoutSec", d.waitTimeoutSec),
                pingTimeoutMs = o.optInt("pingTimeoutMs", d.pingTimeoutMs),
                autoRefreshSec = o.optInt("autoRefreshSec", d.autoRefreshSec),
                refreshOnOpen = o.optBoolean("refreshOnOpen", d.refreshOnOpen),
                haptics = o.optBoolean("haptics", d.haptics),
                confirmWakeAll = o.optBoolean("confirmWakeAll", d.confirmWakeAll),
                compactCards = o.optBoolean("compactCards", d.compactCards),
                sort = o.optInt("sort", d.sort),
                maxLog = o.optInt("maxLog", d.maxLog),
            )
        }
    }
}

data class Schedule(
    val id: Int,
    val deviceId: String,
    val triggerAt: Long,
    val repeatDaily: Boolean,
) {
    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("id", id)
        o.put("deviceId", deviceId)
        o.put("triggerAt", triggerAt)
        o.put("repeatDaily", repeatDaily)
        return o
    }

    companion object {
        fun fromJson(o: JSONObject): Schedule = Schedule(
            id = o.optInt("id", 0),
            deviceId = o.optString("deviceId", ""),
            triggerAt = o.optLong("triggerAt", 0L),
            repeatDaily = o.optBoolean("repeatDaily", false),
        )
    }
}

data class LogEntry(
    val time: Long,
    val device: String,
    val action: String,
    val ok: Boolean,
    val message: String,
) {
    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("time", time)
        o.put("device", device)
        o.put("action", action)
        o.put("ok", ok)
        o.put("message", message)
        return o
    }

    companion object {
        fun fromJson(o: JSONObject): LogEntry = LogEntry(
            time = o.optLong("time", 0L),
            device = o.optString("device", ""),
            action = o.optString("action", ""),
            ok = o.optBoolean("ok", false),
            message = o.optString("message", ""),
        )
    }
}

enum class DevState { UNKNOWN, CHECKING, ONLINE, OFFLINE, WAKING }

data class DeviceStatus(
    val state: DevState = DevState.UNKNOWN,
    val latency: Long = -1L,
    val waited: Int = 0,
)

data class FoundHost(
    val ip: String,
    val name: String,
    val mac: String,
    val latency: Long,
)

data class ScanState(
    val running: Boolean = false,
    val finished: Boolean = false,
    val done: Int = 0,
    val total: Int = 0,
    val found: List<FoundHost> = emptyList(),
    val error: String? = null,
    val network: String? = null,
)
