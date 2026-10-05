package tv.own.owntv.features.customize

import org.junit.Assert.*
import org.junit.Test
import tv.own.owntv.core.customize.GroupScope
import tv.own.owntv.core.model.MediaType

class GroupMergeSelectionTest {
    private val scope = GroupScope(1, MediaType.LIVE, setOf(4))
    private val keys = listOf("a", "b", "c", "d")

    @Test fun reverseRangeRetainsDisplayOrderAndClearsAfterSnapshot() {
        val selection = GroupMergeSelection()
        assertNull(selection.press(scope, keys, "d"))
        selection.extend(scope, "b")
        assertEquals(setOf("b", "c", "d"), selection.selectedKeys.value)
        val edit = selection.press(scope, keys, "b")!!
        assertEquals(listOf("b", "c", "d"), edit.parts.single().selections.map { it.groupId })
        assertEquals(scope, edit.scope)
        assertTrue(selection.selectedKeys.value.isEmpty())
    }

    @Test fun changingProfileSourceOrTypeCannotCommitAnOldRange() {
        for (changed in listOf(scope.copy(profileId = 2), scope.copy(sourceIds = setOf(5)), scope.copy(mediaType = MediaType.MOVIE))) {
            val selection = GroupMergeSelection()
            selection.press(scope, keys, "a")
            assertNull(selection.press(changed, keys, "d"))
            assertTrue(selection.selectedKeys.value.isEmpty())
        }
    }

    @Test fun reorderingOrFilteringWhileSelectingDoesNotChangeCapturedMembers() {
        val selection = GroupMergeSelection()
        selection.press(scope, keys, "a")
        val edit = selection.press(scope, listOf("d", "a", "new"), "d")!!
        assertEquals(keys, edit.parts.single().selections.map { it.groupId })
    }

    @Test fun pressingAnchorOrBackCancelsWithoutOpeningEditor() {
        val selection = GroupMergeSelection()
        selection.press(scope, keys, "a")
        assertNull(selection.press(scope, keys, "a"))
        selection.press(scope, keys, "a")
        selection.cancel()
        assertTrue(selection.selectedKeys.value.isEmpty())
        assertNull(selection.press(scope, keys, "d"))
        assertEquals(setOf("d"), selection.selectedKeys.value)
    }

    @Test fun unknownRowsAndUnavailableScopeCannotFinishASelection() {
        val selection = GroupMergeSelection()
        assertNull(selection.press(scope, keys, "unknown"))
        selection.press(scope, keys, "a")
        selection.extend(scope, "new")
        assertTrue(selection.selectedKeys.value.isEmpty())
        selection.press(scope, keys, "a")
        assertNull(selection.press(null, keys, "d"))
        assertTrue(selection.selectedKeys.value.isEmpty())
    }
}
