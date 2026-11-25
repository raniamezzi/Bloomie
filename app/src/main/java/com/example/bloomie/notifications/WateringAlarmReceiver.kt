package com.example.bloomie.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class WateringAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val isTest = intent?.getBooleanExtra(EXTRA_IS_TEST, false) ?: false
        WateringNotificationHelper.showReminderIfNeeded(context, forceForTest = isTest)
    }

    companion object {
        const val EXTRA_IS_TEST = "extra_is_test"
    }
}