package com.otakeessen.underpressure.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.otakeessen.underpressure.alarm.AlarmScheduler
import com.otakeessen.underpressure.data.local.database.AppDatabase
import com.otakeessen.underpressure.data.repository.SettingsRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that reschedules alarms when the device finishes booting, 
 * app is updated, or system time changes.
 */
class BootReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        
        if (action == Intent.ACTION_BOOT_COMPLETED || 
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == "android.intent.action.QUICKBOOT_POWERON") {
            
            val pendingResult = goAsync()
            scope.launch {
                try {
                    rescheduleAlarms(context)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private suspend fun rescheduleAlarms(context: Context) {
        try {
            val database = AppDatabase.getDatabase(context.applicationContext)
            val repository = SettingsRepositoryImpl(database.appSettingsDao())
            val alarmScheduler = AlarmScheduler(context.applicationContext)

            val settings = repository.getSettingsSync()
            if (settings != null) {
                alarmScheduler.updateAlarms(settings)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
}
