package com.worktrack.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Alarms.scheduleReminder(context) // next day
        val s = Store.snapshot(context)
        if (!s.running && !s.done) Notifier.reminder(context)
    }
}

class CompletionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sent = Store.checkCompletion(context)
        if (!sent) Alarms.scheduleCompletion(context) // fired early or state changed: re-arm if needed
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Alarms.restoreAll(context)
    }
}
