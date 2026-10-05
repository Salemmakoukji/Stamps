package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.DailyStamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Daily Stamp", appName)
  }

  @Test
  fun `verify stamp creation`() {
    val stamp = DailyStamp(
      imageUri = "file:///dummy/path.jpg",
      caption = "Quiet morning coffee",
      dateFormatted = "Mon, Oct 5, 12:00 PM",
      locationOrTag = "Home",
      shapeType = "PORTRAIT"
    )
    assertNotNull(stamp)
    assertEquals("Quiet morning coffee", stamp.caption)
    assertEquals("Mon, Oct 5, 12:00 PM", stamp.dateFormatted)
  }
}
