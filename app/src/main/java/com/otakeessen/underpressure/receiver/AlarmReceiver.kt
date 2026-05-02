package com.otakeessen.underpressure.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.otakeessen.underpressure.MainActivity
import com.otakeessen.underpressure.R
import com.otakeessen.underpressure.alarm.AlarmScheduler
import com.otakeessen.underpressure.data.local.database.AppDatabase
import com.otakeessen.underpressure.data.repository.MeasurementRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * BroadcastReceiver that handles alarm triggers and displays notifications.
 * Skips notification if measurements for the slot are already recorded for today.
 */
class AlarmReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val slotIndex = intent.getIntExtra(AlarmScheduler.EXTRA_SLOT_INDEX, -1)
        val timeStr = intent.getStringExtra(AlarmScheduler.EXTRA_TIME_STR)

        if (slotIndex == -1) {
            return
        }

        val pendingResult = goAsync()
        scope.launch {
            try {
                val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val database = AppDatabase.getDatabase(context.applicationContext)
                val repository = MeasurementRepositoryImpl(database.measurementDao())
                
                val measurements = repository.getMeasurementsByDateSync(todayStr)
                val alreadyFilled = measurements.any { it.slotIndex == slotIndex }

                if (!alreadyFilled) {
                    showNotification(context, slotIndex)
                }
                
                // Reschedule for tomorrow
                if (timeStr != null) {
                    AlarmScheduler(context.applicationContext).scheduleAlarm(slotIndex, timeStr)
                }
            } catch (e: Exception) {
                // If anything fails, try one last time to reschedule for tomorrow to avoid losing the alarm chain
                try {
                    if (timeStr != null) {
                        AlarmScheduler(context.applicationContext).scheduleAlarm(slotIndex, timeStr)
                    }
                } catch (reschedError: Exception) {
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, slotIndex: Int) {
        val channelId = "measurement_reminders"
        createNotificationChannel(context, channelId)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        // Use the same offset for notification requestCode to avoid conflicts
        val requestCode = 100 + slotIndex
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            requestCode, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val slotNumber = slotIndex + 1
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_message, slotNumber))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            val enabled = areNotificationsEnabled()

            if (!enabled) {
                return
            }

            try {
                notify(slotIndex, builder.build())
            } catch (e: SecurityException) {
                Log.e("AlarmReceiver", "SecurityException while posting notification", e)
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "Unexpected error while posting notification", e)
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

