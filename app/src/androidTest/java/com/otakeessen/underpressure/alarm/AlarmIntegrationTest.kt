package com.otakeessen.underpressure.alarm

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.otakeessen.underpressure.receiver.AlarmReceiver
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmIntegrationTest {

    private lateinit var device: UiDevice
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                "pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS"
            )
        }
    }

    @Test
    fun testNotificationAppearance() {
        // Arrange
        val slotIndex = 0
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_SLOT_INDEX, slotIndex)
            putExtra(AlarmScheduler.EXTRA_TIME_STR, "07:00")
        }

        // Act
        context.sendBroadcast(intent)

        // Assert
        // Open notification shade
        device.openNotification()
        
        // Wait for notification to appear
        val expectedTitle = "Time for Measurement"
        val notificationFound = device.wait(Until.hasObject(By.text(expectedTitle)), 5000)
        
        assertTrue("Notification with title '$expectedTitle' should be visible", notificationFound)
        
        // Cleanup: Clear notifications and close shade
        device.pressBack() 
    }
}

