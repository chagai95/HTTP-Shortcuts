package ch.rmy.android.http_shortcuts.activities.main.usecases

import androidx.annotation.CheckResult
import ch.rmy.android.framework.viewmodel.viewstate.DialogState
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.activities.main.MainViewModel

class GetShortcutPlacementDialogUseCase {

    @CheckResult
    operator fun invoke(viewModel: MainViewModel, shortcutId: String) =
        DialogState.create {
            title(R.string.title_select_placement_method)
                .message(R.string.description_select_placement_method)
                .positive(R.string.label_placement_method_default) {
                    viewModel.onShortcutPlacementConfirmed(shortcutId, useLegacyMethod = false)
                }
                .negative(R.string.label_placement_method_legacy) {
                    viewModel.onShortcutPlacementConfirmed(shortcutId, useLegacyMethod = true)
                }
                .build()
        }
}
