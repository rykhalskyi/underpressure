package com.example.underpressure.alarm

import android.app.AlarmManager
import android.content.Context
import io.mockk.every
import io.mockk.mockk
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
        context = mockk(relaxed = true)
        alarmManager = mockk(relaxed = true)
        every { context.getSystemService(Context.ALARM_SERVICE) } returns alarmManager
        alarmScheduler = AlarmScheduler(context)
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
}
