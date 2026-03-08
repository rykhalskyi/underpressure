package com.example.underpressure.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import com.example.underpressure.data.local.entities.AppSettingsEntity
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class AlarmSchedulerTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private lateinit var alarmScheduler: AlarmScheduler

    @Before
    fun setUp() {
        mockkStatic(PendingIntent::class)
        every { PendingIntent.getBroadcast(any(), any(), any(), any()) } returns mockk(relaxed = true)
        
        context = mockk(relaxed = true)
        alarmManager = mockk(relaxed = true)
        every { context.getSystemService(Context.ALARM_SERVICE) } returns alarmManager
        alarmScheduler = AlarmScheduler(context)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `calculateTriggerTime for future time today`() {
        // Arrange
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR_OF_DAY, 1) // 1 hour in the future
        val futureHour = calendar.get(Calendar.HOUR_OF_DAY)
        val futureMinute = calendar.get(Calendar.MINUTE)
        val timeStr = String.format("%02d:%02d", futureHour, futureMinute)

        // Act
        val triggerTime = alarmScheduler.calculateTriggerTime(timeStr)

        // Assert
        val resultCalendar = Calendar.getInstance().apply { timeInMillis = triggerTime }
        assertTrue("Trigger time should be today", 
            resultCalendar.get(Calendar.DAY_OF_YEAR) == Calendar.getInstance().get(Calendar.DAY_OF_YEAR))
        assertTrue("Hour should match", resultCalendar.get(Calendar.HOUR_OF_DAY) == futureHour)
        assertTrue("Minute should match", resultCalendar.get(Calendar.MINUTE) == futureMinute)
    }

    @Test
    fun `calculateTriggerTime for past time (should be tomorrow)`() {
        // Arrange
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR_OF_DAY, -1) // 1 hour in the past
        val pastHour = calendar.get(Calendar.HOUR_OF_DAY)
        val pastMinute = calendar.get(Calendar.MINUTE)
        val timeStr = String.format("%02d:%02d", pastHour, pastMinute)

        // Act
        val triggerTime = alarmScheduler.calculateTriggerTime(timeStr)

        // Assert
        val resultCalendar = Calendar.getInstance().apply { timeInMillis = triggerTime }
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        
        assertTrue("Trigger time should be tomorrow", 
            resultCalendar.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR))
        assertTrue("Hour should match", resultCalendar.get(Calendar.HOUR_OF_DAY) == pastHour)
        assertTrue("Minute should match", resultCalendar.get(Calendar.MINUTE) == pastMinute)
    }

    @Test
    fun `updateAlarms cancels all when masterAlarmEnabled is false`() {
        // Arrange
        val settings = AppSettingsEntity(
            masterAlarmEnabled = false,
            slotActiveFlags = listOf(true, true, true, true),
            slotAlarmsEnabled = listOf(true, true, true, true)
        )

        // Act
        alarmScheduler.updateAlarms(settings)

        // Assert
        // verify that alarmManager.cancel was called for all 4 slots
        // pending intents are created internally so we check for cancel call
        verify(exactly = 4) { alarmManager.cancel(any<android.app.PendingIntent>()) }
    }

    @Test
    fun `updateAlarms schedules active slots when masterAlarmEnabled is true`() {
        // Arrange
        val settings = AppSettingsEntity(
            masterAlarmEnabled = true,
            slotActiveFlags = listOf(true, false, true, false),
            slotAlarmsEnabled = listOf(true, true, true, true) // enabled but depends on active
        )

        // Act
        alarmScheduler.updateAlarms(settings)

        // Assert
        // Slot 0 and 2 should be scheduled (2 times)
        // Slot 1 and 3 should be canceled (2 times)
        verify(exactly = 2) { alarmManager.setExactAndAllowWhileIdle(any(), any<Long>(), any<android.app.PendingIntent>()) }
        verify(exactly = 2) { alarmManager.cancel(any<android.app.PendingIntent>()) }
    }
}
