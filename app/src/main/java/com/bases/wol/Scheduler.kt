package com.bases.wol

import android.app.Activity
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast

object Notif {
    private const val CHANNEL = "wake_events"

    fun show(ctx: Context, title: String, text: String) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Wake events", NotificationManager.IMPORTANCE_DEFAULT)
        )
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_wol)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        nm.notify((System.currentTimeMillis() % 100000L).toInt(), n)
    }
}

object Scheduler {
    private fun pending(ctx: Context, id: Int): PendingIntent {
        val i = Intent(ctx, WakeReceiver::class.java).putExtra("sid", id)
        return PendingIntent.getBroadcast(
            ctx, id, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun schedule(ctx: Context, s: Schedule) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, s.triggerAt, pending(ctx, s.id))
    }

    fun cancel(ctx: Context, id: Int) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(pending(ctx, id))
    }

    /** Re-create alarms after a reboot. Missed one-off alarms are dropped. */
    fun restore(ctx: Context) {
        val repo = Repo.get(ctx)
        val now = System.currentTimeMillis()
        val day = 24L * 60L * 60L * 1000L
        val kept = mutableListOf<Schedule>()
        for (s in repo.schedules.value) {
            var t = s.triggerAt
            if (t <= now) {
                if (!s.repeatDaily) continue
                while (t <= now) t += day
            }
            val ns = s.copy(triggerAt = t)
            kept.add(ns)
            schedule(ctx, ns)
        }
        repo.setSchedules(kept)
    }
}

class WakeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sid = intent.getIntExtra("sid", -1)
        val app = context.applicationContext
        val pending = goAsync()
        Thread {
            try {
                val repo = Repo.get(app)
                val sch = repo.schedules.value.firstOrNull { it.id == sid }
                if (sch != null) {
                    val dev = repo.devices.value.firstOrNull { it.id == sch.deviceId }
                    if (dev != null) {
                        val r = Wol.send(app, dev, repo.settings.value, repo.settings.value.packetCount)
                        if (r.ok) {
                            repo.updateDevices { l ->
                                l.map { if (it.id == dev.id) it.copy(lastWoken = System.currentTimeMillis()) else it }
                            }
                        }
                        repo.addLog(LogEntry(System.currentTimeMillis(), dev.name, "Scheduled wake", r.ok, r.message))
                        Notif.show(
                            app,
                            if (r.ok) "Woke ${dev.name}" else "Could not wake ${dev.name}",
                            r.message
                        )
                    }
                    if (sch.repeatDaily) {
                        val day = 24L * 60L * 60L * 1000L
                        var t = sch.triggerAt
                        val now = System.currentTimeMillis()
                        while (t <= now) t += day
                        val ns = sch.copy(triggerAt = t)
                        repo.setSchedules(repo.schedules.value.map { if (it.id == sid) ns else it })
                        Scheduler.schedule(app, ns)
                    } else {
                        repo.setSchedules(repo.schedules.value.filter { it.id != sid })
                    }
                }
            } finally {
                pending.finish()
            }
        }.start()
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val app = context.applicationContext
            val pending = goAsync()
            Thread {
                try {
                    Scheduler.restore(app)
                } finally {
                    pending.finish()
                }
            }.start()
        }
    }
}

/** Invisible activity used by home-screen shortcuts: sends the packet, shows a toast, closes. */
class WakeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getStringExtra("id")
        val app = applicationContext
        val handler = Handler(Looper.getMainLooper())
        Thread {
            val repo = Repo.get(app)
            val dev = repo.devices.value.firstOrNull { it.id == id }
            val msg: String
            if (dev == null) {
                msg = "Device not found"
            } else {
                val r = Wol.send(app, dev, repo.settings.value, repo.settings.value.packetCount)
                if (r.ok) {
                    repo.updateDevices { l ->
                        l.map { if (it.id == dev.id) it.copy(lastWoken = System.currentTimeMillis()) else it }
                    }
                }
                repo.addLog(LogEntry(System.currentTimeMillis(), dev.name, "Shortcut wake", r.ok, r.message))
                msg = if (r.ok) "Magic packet sent to ${dev.name}" else "Failed: ${r.message}"
            }
            handler.post {
                Toast.makeText(app, msg, Toast.LENGTH_SHORT).show()
                finish()
            }
        }.start()
    }
}
