package tv.own.owntv.features.customize

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import org.koin.compose.koinInject
import tv.own.owntv.core.R
import tv.own.owntv.core.customize.GroupError
import tv.own.owntv.core.customize.GroupService
import tv.own.owntv.ui.theme.OwnTVTheme

/** Feedback only: no focus target, input interception, player control or navigation side effect. */
@Composable
fun GroupOperationStatus(modifier: Modifier = Modifier, groups: GroupService = koinInject()) {
    val state by groups.progress.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val failure = state.failure
    LaunchedEffect(failure) {
        if (failure != null) {
            val message = when (failure) {
                GroupError.REVISION_CONFLICT -> R.string.group_edit_conflict
                GroupError.POSITION_OVERFLOW -> R.string.group_edit_limit
                GroupError.JOURNAL_UNAVAILABLE -> R.string.group_edit_recovery_failed
                GroupError.INVALID_NAME -> R.string.group_edit_invalid_name
                else -> R.string.group_edit_invalid_scope
            }
            Toast.makeText(context, context.getString(message), Toast.LENGTH_LONG).show()
            groups.acknowledgeFailure()
        }
    }
    if (state.operationId != null && state.total >= 500 && failure == null) {
        Column(modifier.background(OwnTVTheme.colors.surface, RoundedCornerShape(8.dp)).padding(12.dp)) {
            Text(stringResource(R.string.group_edit_progress, state.completed, state.total), color = OwnTVTheme.colors.onSurface)
        }
    }
}
