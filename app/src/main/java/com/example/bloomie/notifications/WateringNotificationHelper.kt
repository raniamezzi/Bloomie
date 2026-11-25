package com.example.bloomie.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.bloomie.R
import com.example.bloomie.data.db.DatabaseHelper
import com.example.bloomie.ui.assistant.ScheduleActivity
import java.util.Calendar

object WateringNotificationHelper {

    const val CHANNEL_ID = "bloomie_watering_reminders"
    private const val CHANNEL_NAME = "Watering Reminders"
    private const val NOTIFICATION_ID = 1001
    private const val ALARM_REQUEST_CODE = 2001

    /** Creates the notification channel (required on Android 8.0 / API 26+). */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily reminders when your plants need water"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    /** Schedules a daily reminder at 9:00 AM using AlarmManager. */
    fun scheduleDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildAlarmPendingIntent(context)

        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            target.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    /** Schedules a one-off test alarm [delaySeconds] from now (handy for live demos). */
    fun scheduleTestReminder(context: Context, delaySeconds: Int = 5) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WateringAlarmReceiver::class.java).apply {
            putExtra(WateringAlarmReceiver.EXTRA_IS_TEST, true)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE + 1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + delaySeconds * 1000L,
            pendingIntent
        )
    }

    /** Checks SQLite for due/overdue plants and displays a system notification if any exist. */
    fun showReminderIfNeeded(context: Context, forceForTest: Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        createChannel(context)

        val duePlants = DatabaseHelper(context).use { db -> db.getPlantsNeedingWater() }
        if (duePlants.isEmpty() && !forceForTest) return

        val title = when {
            duePlants.isEmpty() -> "Bloomie Watering Reminder (Test)"
            duePlants.size == 1 -> "${duePlants.first().displayName} needs water!"
            else -> "${duePlants.size} plants need water today"
        }

        val body = if (duePlants.isEmpty()) {
            "All your plants are currently watered and happy."
        } else {
            val names = duePlants.take(3).joinToString(", ") { it.displayName }
            if (duePlants.size > 3) "$names, and more are waiting for water." else "Time to water $names."
        }

        val openScheduleIntent = Intent(context, ScheduleActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            openScheduleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_water)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun buildAlarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, WateringAlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}