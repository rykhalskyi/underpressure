package com.example.underpressure.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.underpressure.alarm.AlarmScheduler
import com.example.underpressure.data.local.database.AppDatabase
import com.example.underpressure.data.repository.SettingsRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that reschedules alarms when the device finishes booting.
 */
class BootReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            rescheduleAlarms(context)
        }
    }

    private fun rescheduleAlarms(context: Context) {
        val database = AppDatabase.getDatabase(context.applicationContext)
        val repository = SettingsRepositoryImpl(database.appSettingsDao())
        val alarmScheduler = AlarmScheduler(context.applicationContext)

        scope.launch {
            val settings = repository.getSettingsSync()
            if (settings != null) {
                alarmScheduler.updateAlarms(settings)
            }
        }
    }
}
