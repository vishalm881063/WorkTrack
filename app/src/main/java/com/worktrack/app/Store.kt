package com.worktrack.app

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class Snapshot(
    val totalMs: Long,
    val running: Boolean,
    val sessionMs: Long,
    val targetMs: Long
) {
    val remainingMs: Long get() = (targetMs - totalMs).coerceAtLeast(0L)
    val fraction: Float get() = if (targetMs <= 0L) 0f else (totalMs.toFloat() / targetMs.toFloat()).coerceIn(0f, 1f)
    val percent: Int get() = if (targetMs <= 0L) 0 else ((totalMs * 100L) / targetMs).toInt().coerceAtMost(100)
    val done: Boolean get() = totalMs >= targetMs
}

object Store {
    private fun p(c: Context): SharedPreferences =
        c.applicationContext.getSharedPreferences("worktrack", Context.MODE_PRIVATE)

    fun dayKey(ms: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(ms))

    private fun endOfDay(ms: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = ms
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    // ---- settings ----
    fun targetMin(c: Context): Int = p(c).getInt("targetMin", 390)
    fun setTargetMin(c: Context, m: Int) { p(c).edit().putInt("targetMin", m).commit() }
    fun reminderHour(c: Context): Int = p(c).getInt("remH", 9)
    fun reminderMin(c: Context): Int = p(c).getInt("remM", 0)
    fun setReminder(c: Context, h: Int, m: Int) { p(c).edit().putInt("remH", h).putInt("remM", m).commit() }

    // ---- history ----
    private fun readHistory(sp: SharedPreferences): MutableMap<String, Long> {
        val out = mutableMapOf<String, Long>()
        val raw = sp.getString("hist", "") ?: ""
        for (part in raw.split(";")) {
            val kv = part.split("=")
            if (kv.size == 2) out[kv[0]] = kv[1].toLongOrNull() ?: 0L
        }
        return out
    }

    private fun writeHistory(sp: SharedPreferences, day: String, ms: Long) {
        val map = readHistory(sp)
        map[day] = ms
        val text = map.entries.sortedBy { it.key }.takeLast(365)
            .joinToString(";") { it.key + "=" + it.value }
        sp.edit().putString("hist", text).commit()
    }

    fun history(c: Context): Map<String, Long> = readHistory(p(c))

    fun notifiedToday(c: Context, now: Long): Boolean =
        p(c).getString("notifiedDay", "") == dayKey(now)

    // ---- timer state ----
    // New day: any running session is stopped at the end of the previous day.
    @Synchronized
    fun sync(c: Context, now: Long = System.currentTimeMillis()) {
        val sp = p(c)
        val today = dayKey(now)
        val stored = sp.getString("day", null)
        if (stored == today) return
        if (stored != null) {
            var acc = sp.getLong("acc", 0L)
            val start = sp.getLong("start", 0L)
            if (start > 0L) acc += (endOfDay(start) - start).coerceAtLeast(0L)
            writeHistory(sp, stored, acc)
        }
        sp.edit().putString("day", today).putLong("acc", 0L).putLong("start", 0L).commit()
    }

    @Synchronized
    fun snapshot(c: Context, now: Long = System.currentTimeMillis()): Snapshot {
        sync(c, now)
        val sp = p(c)
        val acc = sp.getLong("acc", 0L)
        val start = sp.getLong("start", 0L)
        val running = start > 0L
        val session = if (running) (now - start).coerceAtLeast(0L) else 0L
        return Snapshot(acc + session, running, session, targetMin(c) * 60_000L)
    }

    @Synchronized
    fun login(c: Context, now: Long = System.currentTimeMillis()) {
        sync(c, now)
        val sp = p(c)
        if (sp.getLong("start", 0L) == 0L) sp.edit().putLong("start", now).commit()
    }

    @Synchronized
    fun logout(c: Context, now: Long = System.currentTimeMillis()) {
        sync(c, now)
        val sp = p(c)
        val start = sp.getLong("start", 0L)
        if (start > 0L) {
            val acc = sp.getLong("acc", 0L) + (now - start).coerceAtLeast(0L)
            sp.edit().putLong("acc", acc).putLong("start", 0L).commit()
            writeHistory(sp, dayKey(now), acc)
        }
    }

    // Sends the "target completed" notification at most once per day.
    @Synchronized
    fun checkCompletion(c: Context, now: Long = System.currentTimeMillis()): Boolean {
        val s = snapshot(c, now)
        if (!s.running || !s.done) return false
        val sp = p(c)
        val today = dayKey(now)
        if (sp.getString("notifiedDay", "") == today) return false
        sp.edit().putString("notifiedDay", today).commit()
        writeHistory(sp, today, s.totalMs)
        Notifier.completed(c, s.targetMs)
        return true
    }
}
