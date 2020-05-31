package ch.rmy.android.http_shortcuts.scripting.actions.types

import android.content.Context
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.data.DataSource
import ch.rmy.android.http_shortcuts.data.Repository
import ch.rmy.android.http_shortcuts.data.Transactions
import ch.rmy.android.http_shortcuts.data.models.Shortcut
import ch.rmy.android.http_shortcuts.exceptions.ActionException
import ch.rmy.android.http_shortcuts.extensions.truncate
import ch.rmy.android.http_shortcuts.http.ErrorResponse
import ch.rmy.android.http_shortcuts.http.ShortcutResponse
import ch.rmy.android.http_shortcuts.utils.LauncherShortcutManager
import ch.rmy.android.http_shortcuts.variables.VariableManager
import ch.rmy.android.http_shortcuts.variables.Variables
import ch.rmy.android.http_shortcuts.widget.WidgetManager
import io.reactivex.Completable

class RenameShortcutAction(
    actionType: RenameShortcutActionType,
    data: Map<String, String>
) : BaseAction(actionType) {

    private val name: String = data[KEY_NAME] ?: ""

    private val shortcutNameOrId = data[KEY_SHORTCUT_NAME_OR_ID]?.takeUnless { it.isEmpty() }

    override fun perform(context: Context, shortcutId: String, variableManager: VariableManager, response: ShortcutResponse?, responseError: ErrorResponse?, recursionDepth: Int): Completable =
        renameShortcut(context, this.shortcutNameOrId ?: shortcutId, variableManager)

    private fun renameShortcut(context: Context, shortcutNameOrId: String, variableManager: VariableManager): Completable {
        val newName = Variables.rawPlaceholdersToResolvedValues(name, variableManager.getVariableValuesByIds())
            .truncate(Shortcut.NAME_MAX_LENGTH)
        if (newName.isEmpty()) {
            return Completable.complete()
        }
        val shortcut = DataSource.getShortcutByNameOrId(shortcutNameOrId)
            ?: return Completable
                .error(ActionException {
                    it.getString(R.string.error_shortcut_not_found_for_renaming, shortcutNameOrId)
                })
        return renameShortcut(shortcut.id, newName)
            .andThen(Completable.fromAction {
                if (LauncherShortcutManager.supportsPinning(context)) {
                    LauncherShortcutManager.updatePinnedShortcut(
                        context = context,
                        shortcutId = shortcut.id,
                        shortcutName = newName,
                        shortcutIcon = shortcut.iconName
                    )
                }
                WidgetManager.updateWidgets(context, shortcut.id)
            })
    }

    companion object {

        const val KEY_NAME = "name"
        const val KEY_SHORTCUT_NAME_OR_ID = "shortcut_id"

        private fun renameShortcut(shortcutId: String, newName: String) =
            Transactions.commit { realm ->
                Repository.getShortcutById(realm, shortcutId)?.name = newName.truncate(40)
            }

    }

}