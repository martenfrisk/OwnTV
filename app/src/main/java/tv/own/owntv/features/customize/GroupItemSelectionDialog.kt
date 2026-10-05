package tv.own.owntv.features.customize

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import tv.own.owntv.core.R
import tv.own.owntv.features.settings.PickerDialog

enum class GroupItemSelectionAction { HIDE, UNHIDE, FAVORITE, UNFAVORITE, RESET }

/** The same remote-focus picker handles every action on the editor's captured item range. */
@Composable
fun GroupItemSelectionDialog(
    count: Int,
    onSelect: (GroupItemSelectionAction) -> Unit,
    onDismiss: () -> Unit,
) {
    PickerDialog(
        title = pluralStringResource(R.plurals.settings_customize_selected_items, count, count),
        options = listOf(
            GroupItemSelectionAction.HIDE.name to stringResource(R.string.common_hide),
            GroupItemSelectionAction.UNHIDE.name to stringResource(R.string.common_show),
            GroupItemSelectionAction.FAVORITE.name to stringResource(R.string.content_favorite),
            GroupItemSelectionAction.UNFAVORITE.name to stringResource(R.string.group_remove_favorite),
            GroupItemSelectionAction.RESET.name to stringResource(R.string.group_reset_item_overrides),
        ),
        selected = GroupItemSelectionAction.HIDE.name,
        onSelect = { onSelect(GroupItemSelectionAction.valueOf(it)) },
        onDismiss = onDismiss,
    )
}
