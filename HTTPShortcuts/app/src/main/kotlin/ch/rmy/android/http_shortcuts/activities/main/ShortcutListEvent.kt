package ch.rmy.android.http_shortcuts.activities.main

import android.net.Uri
import ch.rmy.android.framework.viewmodel.ViewModelEvent
import ch.rmy.android.http_shortcuts.import_export.ExportFormat

abstract class ShortcutListEvent : ViewModelEvent() {
    data class ShowContextMenu(
        val shortcutId: String,
        val title: String,
        val isPending: Boolean,
        val isMovable: Boolean,
    ) : ViewModelEvent()

    data class ShowMoveToCategoryDialog(val shortcutId: String, val categoryOptions: List<CategoryOption>) : ViewModelEvent() {
        data class CategoryOption(val categoryId: String, val name: String)
    }

    data class ShowFileExportDialog(val shortcutId: String, val format: ExportFormat, val variableIds: Collection<String>) : ViewModelEvent()

    data class StartExport(val shortcutId: String, val uri: Uri, val format: ExportFormat, val variableIds: Collection<String>) : ViewModelEvent()
}
