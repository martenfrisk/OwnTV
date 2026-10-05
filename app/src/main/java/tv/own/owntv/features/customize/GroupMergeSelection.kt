package tv.own.owntv.features.customize

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import tv.own.owntv.core.customize.GroupAction
import tv.own.owntv.core.customize.GroupCompositionEdit
import tv.own.owntv.core.customize.GroupCompositionPart
import tv.own.owntv.core.customize.GroupDestination
import tv.own.owntv.core.customize.GroupScope
import tv.own.owntv.core.customize.GroupSelection

/** A remote range captures its order and scope on the first press, before a modal can open. */
internal class GroupMergeSelection {
    private data class Session(val scope: GroupScope, val keys: List<String>, val anchor: String)
    private var session: Session? = null
    private val _selectedKeys = MutableStateFlow<Set<String>>(emptySet())
    val selectedKeys = _selectedKeys.asStateFlow()

    fun cancel() { session = null; _selectedKeys.value = emptySet() }

    fun extend(scope: GroupScope?, key: String) {
        val current = session ?: return
        if (scope != current.scope) { cancel(); return }
        val start = current.keys.indexOf(current.anchor)
        val end = current.keys.indexOf(key)
        if (end < 0) { cancel(); return }
        _selectedKeys.value = current.keys.subList(minOf(start, end), maxOf(start, end) + 1).toSet()
    }

    fun press(scope: GroupScope?, orderedKeys: List<String>, key: String): GroupCompositionEdit? {
        if (scope == null) { cancel(); return null }
        val current = session
        if (current == null) {
            if (key !in orderedKeys) return null
            val captured = scope.copy(sourceIds = scope.sourceIds?.toSet())
            session = Session(captured, orderedKeys.distinct(), key)
            _selectedKeys.value = setOf(key)
            return null
        }
        if (scope != current.scope || key == current.anchor) { cancel(); return null }
        extend(scope, key)
        val selections = current.keys.filter { it in _selectedKeys.value }.map { GroupSelection(it) }
        cancel()
        return selections.takeIf { it.size > 1 }?.let {
            GroupCompositionEdit(current.scope, GroupAction.COPY,
                listOf(GroupCompositionPart(GroupDestination(name = ""), it)))
        }
    }
}
