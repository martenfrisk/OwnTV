package tv.own.owntv.features.customize

import android.graphics.Bitmap
import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import tv.own.owntv.core.R
import tv.own.owntv.core.customize.*
import tv.own.owntv.core.model.MediaType
import tv.own.owntv.core.theme.AccentColor
import tv.own.owntv.core.theme.ThemeMode
import tv.own.owntv.ui.theme.OwnTVTheme
import tv.own.owntv.ui.components.rememberDialogFocusRestore
import tv.own.owntv.ui.stage.StageButton

/** Exercises the actual TV modal windows, including remote OK/Back and the two-step editor. */
class GroupCompositionDialogTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val edit = GroupCompositionEdit(GroupScope(7, MediaType.LIVE, setOf(3, 4)), GroupAction.COPY,
        listOf(GroupCompositionPart(GroupDestination(name = ""), listOf(GroupSelection("3:news"), GroupSelection("custom:sports")))))

    private fun remote(key: Int) {
        instrumentation.sendKeyDownUpSync(key)
        compose.waitForIdle()
    }

    @Test fun duplicateCreatesOneNamedSnapshotWithoutChangingTheOriginalCommand() {
        var visible by mutableStateOf(true)
        var accepted: GroupCompositionEdit? = null
        compose.setContent {
            OwnTVTheme(ThemeMode.DARK, AccentColor.TEAL, false) {
                if (visible) GroupCompositionDialog(edit, "Duplicate", "News copy", false,
                    onConfirm = { accepted = it; visible = false }, onDismiss = { visible = false })
            }
        }
        compose.onNode(hasSetTextAction()).performTextReplacement("My news")
        compose.onNodeWithText(context.getString(R.string.common_create)).performClick()
        compose.runOnIdle {
            assertEquals("My news", accepted!!.parts.single().destination.name)
            assertEquals(edit.scope, accepted!!.scope)
            assertEquals(edit.parts.single().selections, accepted!!.parts.single().selections)
            assertEquals("", edit.parts.single().destination.name)
        }
    }

    @Test fun remoteCanChooseMoveThenNameTheMergedGroup() {
        var accepted: GroupCompositionEdit? = null
        var visible by mutableStateOf(true)
        compose.setContent {
            OwnTVTheme(ThemeMode.DARK, AccentColor.TEAL, false) {
                if (visible) GroupCompositionDialog(edit, "Merge", chooseAction = true,
                    onConfirm = { accepted = it; visible = false }, onDismiss = { visible = false })
            }
        }
        compose.onNodeWithText(context.getString(R.string.group_copy_members)).assertIsDisplayed()
        // Focus is placed on the selected Copy option after the platform popup attaches.
        compose.waitUntil(5_000) { compose.onAllNodes(androidx.compose.ui.test.isFocused()).fetchSemanticsNodes().isNotEmpty() }
        remote(KeyEvent.KEYCODE_DPAD_DOWN)
        remote(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.onNode(hasSetTextAction()).performTextReplacement("Merged")
        compose.onNodeWithText("Merged").assertIsDisplayed()
        val screenshot = compose.onNode(isDialog()).captureToImage().asAndroidBitmap()
        File(context.cacheDir, "group-composition-dialog.png").outputStream().use { output ->
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        compose.onNodeWithText(context.getString(R.string.common_create)).performClick()
        compose.runOnIdle {
            assertEquals(GroupAction.MOVE, accepted!!.action)
            assertEquals("Merged", accepted!!.parts.single().destination.name)
            assertEquals(edit.scope, accepted!!.scope)
        }
    }

    @Test fun remoteBackFromModeOrNameCancelsWithoutSubmitting() {
        var visible by mutableStateOf(true)
        var mode by mutableStateOf(true)
        var dismissed = 0
        var submissions = 0
        compose.setContent {
            OwnTVTheme(ThemeMode.DARK, AccentColor.TEAL, false) {
                if (visible) GroupCompositionDialog(edit, "Split", chooseAction = mode,
                    onConfirm = { submissions++ }, onDismiss = { dismissed++; visible = false })
            }
        }
        compose.onNodeWithText(context.getString(R.string.group_copy_members)).assertIsDisplayed()
        remote(KeyEvent.KEYCODE_BACK)
        compose.runOnIdle { assertEquals(1, dismissed); mode = false; visible = true }
        compose.onNode(hasSetTextAction()).assertIsDisplayed()
        remote(KeyEvent.KEYCODE_BACK)
        compose.runOnIdle { assertEquals(2, dismissed); assertEquals(0, submissions) }
    }

    @Test fun emptyNewGroupNameCannotSubmit() {
        var submissions = 0
        compose.setContent {
            OwnTVTheme(ThemeMode.DARK, AccentColor.TEAL, false) {
                GroupCompositionDialog(edit, "Split", chooseAction = false, onConfirm = { submissions++ }, onDismiss = {})
            }
        }
        compose.onNodeWithText(context.getString(R.string.common_create)).performClick()
        compose.runOnIdle { assertEquals(0, submissions) }
    }

    @Test fun backReturnsFocusToTheActionThatOpenedTheEditor() {
        compose.setContent {
            OwnTVTheme(ThemeMode.DARK, AccentColor.TEAL, false) {
                var visible by remember { mutableStateOf(false) }
                val opener = remember { FocusRequester() }
                var returnTo by rememberDialogFocusRestore(visible)
                StageButton("Open merge", onClick = { returnTo = opener; visible = true },
                    modifier = Modifier.focusRequester(opener))
                LaunchedEffect(Unit) { opener.requestFocus() }
                if (visible) GroupCompositionDialog(edit, "Merge", chooseAction = true,
                    onConfirm = { visible = false }, onDismiss = { visible = false })
            }
        }
        compose.onNodeWithText("Open merge").assertIsFocused()
        remote(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.onNodeWithText(context.getString(R.string.group_copy_members)).assertIsDisplayed()
        remote(KeyEvent.KEYCODE_BACK)
        compose.waitUntil(5_000) { compose.onAllNodes(androidx.compose.ui.test.isFocused()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Open merge").assertIsFocused()
    }
}
