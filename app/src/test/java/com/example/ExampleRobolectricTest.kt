package com.example

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp", sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AI Senior Eng", appName)
  }

  @Test
  fun `launch main activity`() {
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      scenario.onActivity { activity ->
        assert(activity != null)
      }
    }
  }

  @Test
  fun `test tab navigation and workspace interaction`() {
    composeTestRule.setContent {
      MyApplicationTheme {
        ReviewWorkspaceScreen()
      }
    }

    // Scroll to and verify code input box (IDE style Terminal)
    composeTestRule.onNodeWithTag("workspace_lazy_column")
        .performScrollToNode(hasTestTag("code_terminal_input"))
    composeTestRule.onNodeWithTag("code_terminal_input").assertExists()

    // Type code snippet
    composeTestRule.onNodeWithTag("code_terminal_input").performTextInput("val test = 123")

    // Scroll to and verify Submit button is present and enabled
    composeTestRule.onNodeWithTag("workspace_lazy_column")
        .performScrollToNode(hasTestTag("submit_review_button"))
    composeTestRule.onNodeWithTag("submit_review_button").assertIsEnabled()

    // Test clicking navigation tabs (which are in the scaffold bottom bar, always visible)
    composeTestRule.onNodeWithTag("nav_tab_report").performClick()
    composeTestRule.onNodeWithTag("nav_tab_history").performClick()
    composeTestRule.onNodeWithTag("nav_tab_workspace").performClick()
  }
}
