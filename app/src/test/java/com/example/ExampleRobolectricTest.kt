package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.assistant.actions.AlarmActionHandler
import com.example.assistant.actions.SmsActionHandler
import com.example.data.preferences.EncryptedPreferencesManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `verify app name resource`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Salim Assistant", appName)
  }

  @Test
  fun `verify encrypted preferences store and retrieve`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = EncryptedPreferencesManager(context)

    prefs.saveGroqApiKey("gsk_test_mock_key_12345")
    assertEquals("gsk_test_mock_key_12345", prefs.getGroqApiKey())
    assertTrue(prefs.hasGroqApiKey())

    prefs.deleteGroqApiKey()
    assertFalse(prefs.hasGroqApiKey())
  }

  @Test
  fun `verify alarm parse rollover logic`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val handler = AlarmActionHandler(context)

    val parsed = handler.parseAlarmTime(
      rawHour = 6,
      rawMinute = 30,
      rawAmPm = "AM",
      title = "Morning Workout",
      userPrompt = "Set an alarm for 6:30 AM tomorrow"
    )

    assertEquals(6, parsed.hour)
    assertEquals(30, parsed.minute)
    assertEquals("Morning Workout", parsed.message)
    assertTrue(parsed.isTomorrowRollover)
  }

  @Test
  fun `verify phone number validator`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val handler = SmsActionHandler(context)

    assertTrue(handler.isValidPhoneNumber("+14155552671"))
    assertTrue(handler.isValidPhoneNumber("5551234"))
    assertFalse(handler.isValidPhoneNumber("abc"))
  }
}
