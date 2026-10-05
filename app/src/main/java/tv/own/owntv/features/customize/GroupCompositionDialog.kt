package tv.own.owntv.features.customize

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import tv.own.owntv.core.R
import tv.own.owntv.core.customize.GroupAction
import tv.own.owntv.core.customize.GroupCompositionEdit
import tv.own.owntv.core.customize.GroupDestination
import tv.own.owntv.features.settings.PickerDialog
import tv.own.owntv.ui.components.TextInputDialog

/** Uses existing modal focus/Back handling; one immutable scope/selection survives both prompts. */
@Composable
fun GroupCompositionDialog(
    edit: GroupCompositionEdit,
    title: String,
    suggestedName: String = "",
    chooseAction: Boolean,
    onConfirm: (GroupCompositionEdit) -> Unit,
    onDismiss: () -> Unit,
) {
    require(edit.parts.size == 1)
    var naming by remember(edit) { mutableStateOf(!chooseAction) }
    var action by remember(edit) { mutableStateOf(edit.action) }
    if (!naming) {
        PickerDialog(
            title = title,
            options = listOf(GroupAction.COPY.name to stringResource(R.string.group_copy_members),
                GroupAction.MOVE.name to stringResource(R.string.group_move_members)),
            selected = action.name,
            onSelect = { value ->
                action = GroupAction.valueOf(value)
                naming = true
            },
            onDismiss = onDismiss,
        )
    } else {
        TextInputDialog(
            title = title,
            initial = suggestedName,
            hint = stringResource(R.string.group_composition_hint),
            confirmLabel = stringResource(R.string.common_create),
            allowBlank = false,
            onConfirm = { name -> onConfirm(edit.copy(action = action,
                parts = listOf(edit.parts.single().copy(destination = GroupDestination(name = name))))) },
            onDismiss = onDismiss,
        )
    }
}
