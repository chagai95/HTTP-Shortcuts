package ch.rmy.android.http_shortcuts.activities.categories

import android.app.Activity
import android.app.Application
import ch.rmy.android.framework.extensions.attachTo
import ch.rmy.android.framework.extensions.context
import ch.rmy.android.framework.extensions.createIntent
import ch.rmy.android.framework.extensions.swapped
import ch.rmy.android.framework.extensions.toLocalizable
import ch.rmy.android.framework.utils.localization.QuantityStringLocalizable
import ch.rmy.android.framework.utils.localization.StringResLocalizable
import ch.rmy.android.framework.viewmodel.BaseViewModel
import ch.rmy.android.framework.viewmodel.WithDialog
import ch.rmy.android.framework.viewmodel.viewstate.DialogState
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.activities.categories.usecases.GetBackgroundTypeDialogUseCase
import ch.rmy.android.http_shortcuts.activities.categories.usecases.GetContextMenuDialogUseCase
import ch.rmy.android.http_shortcuts.activities.categories.usecases.GetCreationDialogUseCase
import ch.rmy.android.http_shortcuts.activities.categories.usecases.GetDeletionDialogUseCase
import ch.rmy.android.http_shortcuts.activities.categories.usecases.GetLayoutTypeDialogUseCase
import ch.rmy.android.http_shortcuts.activities.categories.usecases.GetRenameDialogUseCase
import ch.rmy.android.http_shortcuts.data.domains.categories.CategoryRepository
import ch.rmy.android.http_shortcuts.data.enums.CategoryBackgroundType
import ch.rmy.android.http_shortcuts.data.enums.CategoryLayoutType
import ch.rmy.android.http_shortcuts.data.models.Category
import ch.rmy.android.http_shortcuts.utils.ExternalURLs
import ch.rmy.android.http_shortcuts.utils.LauncherShortcutManager

class CategoriesViewModel(application: Application) : BaseViewModel<Unit, CategoriesViewState>(application), WithDialog {

    private val categoryRepository: CategoryRepository = CategoryRepository()
    private val launcherShortcutManager = LauncherShortcutManager
    private val getContextMenuDialog = GetContextMenuDialogUseCase()
    private val getLayoutTypeDialog = GetLayoutTypeDialogUseCase()
    private val getBackgroundTypeDialog = GetBackgroundTypeDialogUseCase()
    private val getDeletionDialog = GetDeletionDialogUseCase()
    private val getRenameDialog = GetRenameDialogUseCase()
    private val getCreationDialog = GetCreationDialogUseCase()

    private lateinit var categories: List<Category>
    private var hasChanged = false

    override var dialogState: DialogState?
        get() = currentViewState.dialogState
        set(value) {
            updateViewState {
                copy(dialogState = value)
            }
        }

    override fun initViewState() = CategoriesViewState(
        categories = mapCategories(categories),
    )

    override fun onInitializationStarted(data: Unit) {
        categoryRepository.getObservableCategories()
            .subscribe { categories ->
                this.categories = categories
                if (isInitialized) {
                    updateViewState {
                        copy(categories = mapCategories(categories))
                    }
                } else {
                    finalizeInitialization()
                }
            }
            .attachTo(destroyer)
    }

    fun onBackPressed() {
        waitForOperationsToFinish {
            finish(hasChanged)
        }
    }

    private fun finish(hasChanges: Boolean) {
        finish(
            result = Activity.RESULT_OK,
            intent = createIntent {
                putExtra(CategoriesActivity.EXTRA_CATEGORIES_CHANGED, hasChanges)
            },
        )
    }

    fun onCategoryClicked(categoryId: String) {
        showContextMenu(categoryId)
    }

    private fun showContextMenu(categoryId: String) {
        val category = getCategory(categoryId) ?: return
        dialogState = getContextMenuDialog(
            categoryId = category.id,
            title = category.name.toLocalizable(),
            hideOptionVisible = !category.hidden && categories.count { !it.hidden } > 1,
            showOptionVisible = category.hidden,
            changeLayoutTypeOptionVisible = !category.hidden,
            placeOnHomeScreenOptionVisible = !category.hidden && launcherShortcutManager.supportsPinning(context),
            deleteOptionVisible = category.hidden || categories.count { !it.hidden } > 1,
            viewModel = this,
        )
    }

    private fun getCategory(categoryId: String) =
        categories.firstOrNull { it.id == categoryId }

    fun onCategoryMoved(categoryId1: String, categoryId2: String) {
        updateViewState {
            copy(categories = categories.swapped(categoryId1, categoryId2) { id })
        }
        performOperation(categoryRepository.moveCategory(categoryId1, categoryId2)) {
            hasChanged = true
        }
    }

    fun onHelpButtonClicked() {
        openURL(ExternalURLs.CATEGORIES_DOCUMENTATION)
    }

    fun onCreateCategoryButtonClicked() {
        dialogState = getCreationDialog(this)
    }

    fun onCreateDialogConfirmed(name: String) {
        performOperation(
            categoryRepository.createCategory(name)
                .doOnComplete {
                    hasChanged = true
                    showSnackbar(R.string.message_category_created)
                }
        )
    }

    fun onRenameCategoryOptionSelected(categoryId: String) {
        val category = getCategory(categoryId) ?: return
        dialogState = getRenameDialog(categoryId, prefill = category.name, this)
    }

    fun onRenameDialogConfirmed(categoryId: String, newName: String) {
        performOperation(
            categoryRepository.renameCategory(categoryId, newName)
                .doOnComplete {
                    hasChanged = true
                    LauncherShortcutManager.updatePinnedCategoryShortcut(context, categoryId, newName)
                    showSnackbar(R.string.message_category_renamed)
                }
        )
    }

    fun onCategoryVisibilityChanged(categoryId: String, hidden: Boolean) {
        performOperation(categoryRepository.toggleCategoryHidden(categoryId, hidden)) {
            hasChanged = true
            showSnackbar(if (hidden) R.string.message_category_hidden else R.string.message_category_visible)
        }
    }

    fun onCategoryDeletionConfirmed(categoryId: String) {
        performOperation(categoryRepository.deleteCategory(categoryId)) {
            hasChanged = true
            showSnackbar(R.string.message_category_deleted)
        }
    }

    fun onCategoryDeletionSelected(categoryId: String) {
        val category = getCategory(categoryId) ?: return
        if (category.shortcuts.isEmpty()) {
            onCategoryDeletionConfirmed(categoryId)
        } else {
            dialogState = getDeletionDialog(categoryId, this)
        }
    }

    fun onPlaceOnHomeScreenSelected(categoryId: String) {
        val category = getCategory(categoryId) ?: return
        LauncherShortcutManager.pinCategory(context, category.id, category.name)
    }

    fun onLayoutTypeChanged(categoryId: String, layoutType: CategoryLayoutType) {
        performOperation(categoryRepository.setLayoutType(categoryId, layoutType)) {
            hasChanged = true
            showSnackbar(R.string.message_layout_type_changed)
        }
    }

    fun onBackgroundTypeChanged(categoryId: String, backgroundType: CategoryBackgroundType) {
        emitEvent(CategoriesEvent.RequestFilePermissionsIfNeeded)
        performOperation(categoryRepository.setBackground(categoryId, backgroundType)) {
            hasChanged = true
            showSnackbar(R.string.message_background_type_changed)
        }
    }

    fun onLayoutTypeOptionSelected(categoryId: String) {
        dialogState = getLayoutTypeDialog(categoryId, this)
    }

    fun onBackgroundTypeOptionSelected(categoryId: String) {
        dialogState = getBackgroundTypeDialog(categoryId, this)
    }

    companion object {
        private const val MAX_ICONS = 5

        private fun mapCategories(categories: List<Category>): List<CategoryListItem> =
            categories.map { category ->
                CategoryListItem(
                    id = category.id,
                    name = if (category.hidden) {
                        StringResLocalizable(R.string.label_category_hidden, category.name)
                    } else {
                        category.name.toLocalizable()
                    },
                    description = QuantityStringLocalizable(
                        R.plurals.shortcut_count,
                        count = category.shortcuts.size,
                    ),
                    icons = category.shortcuts
                        .take(MAX_ICONS)
                        .map { shortcut ->
                            shortcut.icon
                        },
                    layoutType = category.categoryLayoutType.takeUnless { category.hidden },
                )
            }
    }
}
