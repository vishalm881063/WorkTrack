package com.worktrack.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

val Navy = Color(0xFF0A1128)
val Card = Color(0xFF121C36)
val Card2 = Color(0xFF1B2748)
val Mint = Color(0xFF3DF5C0)
val Muted = Color(0xFF8E9BB8)
val Warn = Color(0xFFFFB454)

@Composable
fun WorkTrackTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(primary = Mint, background = Navy, surface = Card, onSurface = Color.White),
        content = content
    )
}

fun fmtHMS(ms: Long): String {
    val t = ms / 1000
    return "%02d:%02d:%02d".format(t / 3600, (t % 3600) / 60, t % 60)
}

fun fmtHM(ms: Long): String {
    val m = ms / 60_000
    return "${m / 60}h ${m % 60}m"
}

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(24.dp)).background(Card).padding(18.dp),
        content = content
    )
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Panel(modifier) {
        Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(label, color = Muted, fontSize = 12.sp)
    }
}

@Composable
fun Dashboard(ctx: Context, s: Snapshot, now: Long, modifier: Modifier, onToggle: () -> Unit) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good morning!"
        hour < 17 -> "Good afternoon!"
        else -> "Good evening!"
    }
    val status = when {
        s.running && s.done -> "● Target reached"
        s.running -> "● Tracking"
        s.done -> "✓ Target reached"
        s.totalMs > 0 -> "Paused"
        else -> "Not started"
    }
    val label = when {
        s.running -> "Logout"
        s.totalMs > 0 -> "Resume"
        else -> "Login"
    }
    val hist = Store.history(ctx)

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(greeting, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("Here's your daily overview", color = Muted, fontSize = 15.sp)
        }

        PermissionBanner(ctx)

        Panel(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(240.dp)) {
                        val stroke = 20.dp.toPx()
                        val sz = Size(size.width - stroke, size.height - stroke)
                        val tl = Offset(stroke / 2, stroke / 2)
                        drawArc(Card2, 0f, 360f, false, tl, sz, style = Stroke(stroke))
                        if (s.fraction > 0f) {
                            drawArc(Mint, -90f, 360f * s.fraction, false, tl, sz,
                                style = Stroke(stroke, cap = StrokeCap.Round))
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(fmtHMS(s.totalMs), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                        Text("worked today", color = Muted, fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("${s.percent}%", color = Mint, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                status,
                color = if (s.running || s.done) Mint else Warn,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Remaining", fmtHM(s.remainingMs), Modifier.weight(1f))
            StatCard("This session", fmtHMS(s.sessionMs), Modifier.weight(1f))
            StatCard("Daily target", fmtHM(s.targetMs), Modifier.weight(1f))
        }

        Button(
            onClick = onToggle,
            modifier = Modifier.fillMaxWidth().height(68.dp),
            shape = RoundedCornerShape(34.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (s.running) Card2 else Mint,
                contentColor = if (s.running) Mint else Navy
            ),
            border = if (s.running) BorderStroke(1.5.dp, Mint) else null
        ) {
            Text(label, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Panel(Modifier.fillMaxWidth()) {
            Text("Productivity Trends", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text("Last 7 days", color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().height(170.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                for (i in 6 downTo 0) {
                    val t = now - i * 86_400_000L
                    val key = Store.dayKey(t)
                    val ms = if (i == 0) s.totalMs else (hist[key] ?: 0L)
                    val frac = if (s.targetMs > 0) (ms.toFloat() / s.targetMs).coerceIn(0f, 1f) else 0f
                    val cal = Calendar.getInstance().apply { timeInMillis = t }
                    val dayLetter = SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time).take(3)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.width(26.dp).height((16 + 120 * frac).dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (i == 0) Mint else Mint.copy(alpha = 0.35f))
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(dayLetter, color = if (i == 0) Mint else Muted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionBanner(ctx: Context) {
    val exactOk = Alarms.canExact(ctx)
    val notifOk = Notifier.enabled(ctx)
    if (exactOk && notifOk) return
    Panel(Modifier.fillMaxWidth()) {
        Text("Action needed", color = Warn, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            if (!notifOk) "Notifications are off. Turn them on to get reminders."
            else "Allow exact alarms so reminders arrive on time.",
            color = Muted, fontSize = 13.sp
        )
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                if (!notifOk) openNotificationSettings(ctx) else openExactAlarmSettings(ctx)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Navy)
        ) { Text("Fix now") }
    }
}

fun openNotificationSettings(ctx: Context) {
    val i = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try { ctx.startActivity(i) } catch (e: Exception) {}
}

fun openExactAlarmSettings(ctx: Context) {
    if (Build.VERSION.SDK_INT >= 31) {
        val i = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + ctx.packageName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { ctx.startActivity(i) } catch (e: Exception) {}
    }
}

fun openBatterySettings(ctx: Context) {
    val i = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + ctx.packageName))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        ctx.startActivity(i)
    } catch (e: Exception) {
        try {
            ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e2: Exception) {}
    }
}

@Composable
fun HistoryScreen(ctx: Context, s: Snapshot, now: Long, modifier: Modifier) {
    val map = Store.history(ctx).toMutableMap()
    map[Store.dayKey(now)] = s.totalMs
    val rows = map.entries.sortedByDescending { it.key }.filter { it.value > 0L }
    val parse = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val show = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())

    Column(modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text("Work History", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        if (rows.isEmpty()) {
            Text("No work logged yet. Tap Login on the Today tab to start.", color = Muted)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(rows) { e ->
                    val pretty = try { show.format(parse.parse(e.key)!!) } catch (ex: Exception) { e.key }
                    val met = e.value >= s.targetMs
                    val pct = if (s.targetMs > 0) ((e.value * 100) / s.targetMs).toInt().coerceAtMost(100) else 0
                    Panel(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(pretty, color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text(fmtHM(e.value) + " worked", color = Muted, fontSize = 13.sp)
                            }
                            Text(
                                if (met) "✓ Target met" else "$pct%",
                                color = if (met) Mint else Warn, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RoundBtn(text: String, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(Card2).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Text(text, color = Mint, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, modifier = Modifier.weight(1f))
        RoundBtn("−", onMinus)
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(70.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        RoundBtn("+", onPlus)
    }
}

@Composable
fun SettingsScreen(ctx: Context, modifier: Modifier, tick: Int, onChange: () -> Unit) {
    val rh = Store.reminderHour(ctx)
    val rm = Store.reminderMin(ctx)
    val tm = Store.targetMin(ctx)
    @Suppress("UNUSED_VARIABLE") val unused = tick

    fun setRem(h: Int, m: Int) {
        Store.setReminder(ctx, (h + 24) % 24, (m + 60) % 60)
        Alarms.scheduleReminder(ctx)
        onChange()
    }
    fun setTarget(m: Int) {
        Store.setTargetMin(ctx, m.coerceIn(30, 960))
        Alarms.scheduleCompletion(ctx)
        onChange()
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)

        Panel(Modifier.fillMaxWidth()) {
            Text("Daily login reminder", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text("Reminder at %02d:%02d every day".format(rh, rm), color = Mint, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Stepper("Hour", "%02d".format(rh), { setRem(rh - 1, rm) }, { setRem(rh + 1, rm) })
            Stepper("Minute", "%02d".format(rm), { setRem(rh, rm - 5) }, { setRem(rh, rm + 5) })
        }

        Panel(Modifier.fillMaxWidth()) {
            Text("Daily work target", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text("Target: ${tm / 60}h ${tm % 60}m", color = Mint, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Stepper("Hours", "${tm / 60} h", { setTarget(tm - 60) }, { setTarget(tm + 60) })
            Stepper("Minutes", "${tm % 60} min", { setTarget(tm - 5) }, { setTarget(tm + 5) })
        }

        Panel(Modifier.fillMaxWidth()) {
            Text("Reliable alerts", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "For on-time reminders, allow exact alarms and disable battery optimization for WorkTrack.",
                color = Muted, fontSize = 13.sp
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { openExactAlarmSettings(ctx) },
                    colors = ButtonDefaults.buttonColors(containerColor = Card2, contentColor = Mint)
                ) { Text(if (Alarms.canExact(ctx)) "Alarms ✓" else "Allow alarms") }
                Button(
                    onClick = { openBatterySettings(ctx) },
                    colors = ButtonDefaults.buttonColors(containerColor = Card2, contentColor = Mint)
                ) { Text("Battery") }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { openNotificationSettings(ctx) },
                colors = ButtonDefaults.buttonColors(containerColor = Card2, contentColor = Mint)
            ) { Text(if (Notifier.enabled(ctx)) "Notifications ✓" else "Enable notifications") }
        }
    }
}
