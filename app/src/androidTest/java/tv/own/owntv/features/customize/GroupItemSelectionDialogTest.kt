package tv.own.owntv.features.customize

import android.graphics.Bitmap
import android.view.KeyEvent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import tv.own.owntv.core.R
import tv.own.owntv.core.theme.AccentColor
import tv.own.owntv.core.theme.ThemeMode
import tv.own.owntv.ui.components.rememberDialogFocusRestore
import tv.own.owntv.ui.stage.StageButton
import tv.own.owntv.ui.theme.OwnTVTheme

class GroupItemSelectionDialogTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private fun remote(key: Int) { instrumentation.sendKeyDownUpSync(key); compose.waitForIdle() }

    @Test fun remoteCanChooseFavoriteForTheSelectedRange() {
        var accepted: GroupItemSelectionAction? = null
        var visible by mutableStateOf(true)
        compose.setContent {
            OwnTVTheme(ThemeMode.DARK, AccentColor.TEAL, false) {
                if (visible) GroupItemSelectionDialog(3, { accepted = it; visible = false }, { visible = false })
            }
        }
        // The shared picker installs selected-option focus after its 80 ms attachment delay.
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithText(context.getString(R.string.common_hide)).assertIsFocused()
        remote(KeyEvent.KEYCODE_DPAD_DOWN)
        compose.onNodeWithText(context.getString(R.string.common_show)).assertIsFocused()
        remote(KeyEvent.KEYCODE_DPAD_DOWN)
        compose.onNodeWithText(context.getString(R.string.content_favorite)).assertIsFocused()
        remote(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertEquals(GroupItemSelectionAction.FAVORITE, accepted) }
    }

    @Test fun pickerShowsBothFavoriteChoicesAndTheSpecificResetLabel() {
        var accepted: GroupItemSelectionAction? = null
        compose.setContent {
            OwnTVTheme(ThemeMode.DARK, AccentColor.TEAL, false) {
                GroupItemSelectionDialog(10, { accepted = it }, {})
            }
        }
        for (label in listOf(R.string.common_hide, R.string.common_show, R.string.content_favorite,
            R.string.group_remove_favorite, R.string.group_reset_item_overrides)) {
            compose.onNodeWithText(context.getString(label)).assertIsDisplayed()
        }
        compose.onNode(isDialog()).captureToImage().asAndroidBitmap().let { screenshot ->
            File(context.cacheDir, "group-item-selection-dialog.png").outputStream().use {
                screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
        compose.onNodeWithText(context.getString(R.string.group_reset_item_overrides)).performClick()
        compose.runOnIdle { assertEquals(GroupItemSelectionAction.RESET, accepted) }
    }

    @Test fun backCancelsThePickerAndReturnsRemoteFocusToItsOpener() {
        var submissions = 0
        compose.setContent {
            OwnTVTheme(ThemeMode.DARK, AccentColor.TEAL, false) {
                var visible by remember { mutableStateOf(false) }
                val opener = remember { FocusRequester() }
                var returnTo by rememberDialogFocusRestore(visible)
                StageButton("Open item actions", { returnTo = opener; visible = true }, modifier = Modifier.focusRequester(opener))
                LaunchedEffect(Unit) { opener.requestFocus() }
                if (visible) GroupItemSelectionDialog(3, { submissions++; visible = false }, { visible = false })
            }
        }
        compose.onNodeWithText("Open item actions").assertIsFocused()
        remote(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.onNodeWithText(context.getString(R.string.content_favorite)).assertIsDisplayed()
        remote(KeyEvent.KEYCODE_BACK)
        compose.waitUntil(5_000) { compose.onAllNodes(isFocused()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Open item actions").assertIsFocused()
        compose.runOnIdle { assertEquals(0, submissions) }
    }
}
