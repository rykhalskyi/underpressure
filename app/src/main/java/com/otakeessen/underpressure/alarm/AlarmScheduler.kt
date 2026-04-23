package com.otakeessen.underpressure.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.otakeessen.underpressure.data.local.entities.AppSettingsEntity
import com.otakeessen.underpressure.receiver.AlarmReceiver
import java.util.Calendar

/**
 * Utility class for scheduling and canceling measurement slot alarms.
 *
 * @property context The application context.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Checks if the app can schedule exact alarms.
     * Required for Android 12 (API 31) and higher.
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * Reconciles all slot alarms based on current settings.
     * Schedules alarms for active slots with enabled alarms, and cancels the rest.
     *
     * @param settings Current application settings.
     */
    fun updateAlarms(settings: AppSettingsEntity) {
        for (i in 0 until 4) {
            val isActive = settings.slotActiveFlags.getOrElse(i) { i == 0 }
            val isAlarmEnabled = settings.slotAlarmsEnabled.getOrElse(i) { false }
            val time = settings.slotTimes.getOrElse(i) { "07:00" }

            if (settings.masterAlarmEnabled && isActive && isAlarmEnabled) {
                scheduleAlarm(i, time)
            } else {
                cancelAlarm(i)
            }
        }
    }

    /**
     * Schedules a daily repeating alarm for a specific slot.
     *
     * @param slotIndex The index of the slot (0-3).
     * @param timeStr The time in "HH:mm" format.
     */
    fun scheduleAlarm(slotIndex: Int, timeStr: String) {
        val intent = createPendingIntent(slotIndex, timeStr)
        val triggerAtMillis = calculateTriggerTime(timeStr)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                intent
            )
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                intent
            )
        }
    }

    /**
     * Cancels the alarm for a specific slot.
     *
     * @param slotIndex The index of the slot (0-3).
     */
    fun cancelAlarm(slotIndex: Int) {
        val intent = createPendingIntent(slotIndex)
        alarmManager.cancel(intent)
    }

    /**
     * Dismisses any active notification for a specific slot.
     *
     * @param slotIndex The index of the slot (0-3).
     */
    fun dismissNotification(slotIndex: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.cancel(slotIndex)
    }

    private fun createPendingIntent(slotIndex: Int, timeStr: String? = null): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_SLOT_INDEX, slotIndex)
            timeStr?.let { putExtra(EXTRA_TIME_STR, it) }
        }
        return PendingIntent.getBroadcast(
            context,
            slotIndex,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    @androidx.annotation.VisibleForTesting(otherwise = androidx.annotation.VisibleForTesting.PRIVATE)
    internal fun calculateTriggerTime(timeStr: String): Long {
        val parts = timeStr.split(":")
        val hour = parts[0].toIntOrNull() ?: 7
        val minute = parts[1].toIntOrNull() ?: 0

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If time is in the past, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return calendar.timeInMillis
    }

    companion object {
        const val EXTRA_SLOT_INDEX = "extra_slot_index"
        const val EXTRA_TIME_STR = "extra_time_str"
    }
}

