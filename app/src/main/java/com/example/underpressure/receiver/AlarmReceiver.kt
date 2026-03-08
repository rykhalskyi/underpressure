package com.example.underpressure.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.underpressure.MainActivity
import com.example.underpressure.R
import com.example.underpressure.alarm.AlarmScheduler

/**
 * BroadcastReceiver that handles alarm triggers and displays notifications.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val slotIndex = intent.getIntExtra(AlarmScheduler.EXTRA_SLOT_INDEX, -1)
        val timeStr = intent.getStringExtra(AlarmScheduler.EXTRA_TIME_STR)

        if (slotIndex != -1) {
            showNotification(context, slotIndex)
            
            // Reschedule for tomorrow
            if (timeStr != null) {
                AlarmScheduler(context).scheduleAlarm(slotIndex, timeStr)
            }
        }
    }

    private fun showNotification(context: Context, slotIndex: Int) {
        val channelId = "measurement_reminders"
        createNotificationChannel(context, channelId)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val slotNumber = slotIndex + 1
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Using existing icon
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_message, slotNumber))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            if (!areNotificationsEnabled()) {
                android.util.Log.e("AlarmReceiver", "Notifications are BLOCKED by the system settings")
                return
            }

            try {
                notify(slotIndex, builder.build())
            } catch (e: SecurityException) {
                android.util.Log.e("AlarmReceiver", "Failed to show notification: Missing permission", e)
            }
        }
    }

    private fun createNotificationChannel(context: Context, channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_name)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance)
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
