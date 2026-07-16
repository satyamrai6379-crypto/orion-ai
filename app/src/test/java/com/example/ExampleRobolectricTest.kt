package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.viewmodel.ChatViewModel
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Orion AI", appName)
  }

  @Test
  fun `verify AI mode display names`() {
    assertEquals("General Assistant", ChatViewModel.AiMode.GENERAL.displayName)
    assertEquals("Coding Assistant", ChatViewModel.AiMode.CODING.displayName)
    assertEquals("Study Assistant", ChatViewModel.AiMode.STUDY.displayName)
    assertEquals("Writing Assistant", ChatViewModel.AiMode.WRITING.displayName)
  }
}
