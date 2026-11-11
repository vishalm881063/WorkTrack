package com.worktrack.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object Alarms {
    private fun am(c: Context) = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canExact(c: Context): Boolean =
        Build.VERSION.SDK_INT < 31 || am(c).canScheduleExactAlarms()

    private fun pending(c: Context, cls: Class<*>, code: Int): PendingIntent =
        PendingIntent.getBroadcast(
            c, code, Intent(c, cls),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun setAlarm(c: Context, at: Long, pi: PendingIntent) {
        val a = am(c)
        try {
            if (canExact(c)) a.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            else a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } catch (e: SecurityException) {
            a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun scheduleReminder(c: Context) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, Store.reminderHour(c))
        cal.set(Calendar.MINUTE, Store.reminderMin(c))
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= now + 60_000L) cal.add(Calendar.DAY_OF_YEAR, 1)
        setAlarm(c, cal.timeInMillis, pending(c, ReminderReceiver::class.java, 1))
    }

    fun cancelCompletion(c: Context) {
        am(c).cancel(pending(c, CompletionReceiver::class.java, 2))
    }

    fun scheduleCompletion(c: Context) {
        cancelCompletion(c)
        val now = System.currentTimeMillis()
        val s = Store.snapshot(c, now)
        if (!s.running || Store.notifiedToday(c, now)) return
        setAlarm(c, now + s.remainingMs + 1000L, pending(c, CompletionReceiver::class.java, 2))
    }

    fun restoreAll(c: Context) {
        scheduleReminder(c)
        scheduleCompletion(c)
    }
}
