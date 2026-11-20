package com.bases.wol

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONArray
import org.json.JSONObject

class Repo private constructor(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("wolf", Context.MODE_PRIVATE)

    val devices = MutableStateFlow(loadDevices())
    val settings = MutableStateFlow(loadSettings())
    val log = MutableStateFlow(loadLog())
    val schedules = MutableStateFlow(loadSchedules())

    private fun loadDevices(): List<Device> = try {
        val arr = JSONArray(prefs.getString("devices", "[]"))
        (0 until arr.length()).map { Device.fromJson(arr.getJSONObject(it)) }
    } catch (e: Exception) {
        emptyList()
    }

    private fun loadSettings(): AppSettings = try {
        AppSettings.fromJson(JSONObject(prefs.getString("settings", "{}")))
    } catch (e: Exception) {
        AppSettings()
    }

    private fun loadLog(): List<LogEntry> = try {
        val arr = JSONArray(prefs.getString("log", "[]"))
        (0 until arr.length()).map { LogEntry.fromJson(arr.getJSONObject(it)) }
    } catch (e: Exception) {
        emptyList()
    }

    private fun loadSchedules(): List<Schedule> = try {
        val arr = JSONArray(prefs.getString("schedules", "[]"))
        (0 until arr.length()).map { Schedule.fromJson(arr.getJSONObject(it)) }
    } catch (e: Exception) {
        emptyList()
    }

    @Synchronized
    fun setDevices(list: List<Device>) {
        devices.value = list
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString("devices", arr.toString()).apply()
    }

    @Synchronized
    fun updateDevices(f: (List<Device>) -> List<Device>) {
        setDevices(f(devices.value))
    }

    @Synchronized
    fun updateSettings(f: (AppSettings) -> AppSettings) {
        val s = f(settings.value)
        settings.value = s
        prefs.edit().putString("settings", s.toJson().toString()).apply()
    }

    @Synchronized
    fun addLog(e: LogEntry) {
        val max = settings.value.maxLog.coerceAtLeast(10)
        val l = (listOf(e) + log.value).take(max)
        log.value = l
        val arr = JSONArray()
        l.forEach { arr.put(it.toJson()) }
        prefs.edit().putString("log", arr.toString()).apply()
    }

    @Synchronized
    fun clearLog() {
        log.value = emptyList()
        prefs.edit().putString("log", "[]").apply()
    }

    @Synchronized
    fun setSchedules(list: List<Schedule>) {
        schedules.value = list
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString("schedules", arr.toString()).apply()
    }

    @Synchronized
    fun nextScheduleId(): Int {
        val n = prefs.getInt("sid", 0) + 1
        prefs.edit().putInt("sid", n).apply()
        return n
    }

    companion object {
        @Volatile
        private var inst: Repo? = null

        fun get(ctx: Context): Repo {
            val cur = inst
            if (cur != null) return cur
            return synchronized(this) {
                inst ?: Repo(ctx.applicationContext).also { inst = it }
            }
        }
    }
}
