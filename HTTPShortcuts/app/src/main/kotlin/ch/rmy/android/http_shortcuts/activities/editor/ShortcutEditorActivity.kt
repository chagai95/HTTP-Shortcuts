package ch.rmy.android.http_shortcuts.activities.editor

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import ch.rmy.android.framework.extensions.attachTo
import ch.rmy.android.framework.extensions.bindViewModel
import ch.rmy.android.framework.extensions.consume
import ch.rmy.android.framework.extensions.focus
import ch.rmy.android.framework.extensions.logInfo
import ch.rmy.android.framework.extensions.observe
import ch.rmy.android.framework.extensions.observeTextChanges
import ch.rmy.android.framework.extensions.setMaxLength
import ch.rmy.android.framework.extensions.setSubtitle
import ch.rmy.android.framework.extensions.setTextSafely
import ch.rmy.android.framework.extensions.visible
import ch.rmy.android.framework.ui.BaseIntentBuilder
import ch.rmy.android.framework.viewmodel.ViewModelEvent
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.activities.BaseActivity
import ch.rmy.android.http_shortcuts.data.enums.ShortcutExecutionType
import ch.rmy.android.http_shortcuts.data.models.Shortcut
import ch.rmy.android.http_shortcuts.databinding.ActivityShortcutEditorOverviewBinding
import ch.rmy.android.http_shortcuts.icons.IconPicker
import ch.rmy.curlcommand.CurlCommand

class ShortcutEditorActivity : BaseActivity() {

    private val viewModel: ShortcutEditorViewModel by bindViewModel()

    private lateinit var binding: ActivityShortcutEditorOverviewBinding
    private var saveMenuItem: MenuItem? = null
    private var testMenuItem: MenuItem? = null

    private val iconPicker by lazy {
        IconPicker(this) { icon ->
            viewModel.onShortcutIconChanged(icon)
        }
    }

    override fun onCreated(savedState: Bundle?) {
        viewModel.initialize(
            ShortcutEditorViewModel.InitData(
                categoryId = intent.getStringExtra(EXTRA_CATEGORY_ID),
                shortcutId = intent.getStringExtra(EXTRA_SHORTCUT_ID),
                curlCommand = intent.getSerializableExtra(EXTRA_CURL_COMMAND) as CurlCommand?,
                executionType = intent.getStringExtra(EXTRA_EXECUTION_TYPE)
                    ?.let(ShortcutExecutionType::get)
                    ?: ShortcutExecutionType.APP,
            ),
        )
        initViews()
        initUserInputBindings()
        initViewModelBindings()
    }

    private fun initViews() {
        binding = applyBinding(ActivityShortcutEditorOverviewBinding.inflate(layoutInflater))
        binding.inputShortcutName.setMaxLength(Shortcut.NAME_MAX_LENGTH)
        binding.mainView.visible = false
        title = ""
    }

    private fun initUserInputBindings() {
        binding.iconContainer.setOnClickListener {
            iconPicker.openIconSelectionDialog()
        }
        binding.buttonBasicRequestSettings.setOnClickListener {
            viewModel.onBasicRequestSettingsButtonClicked()
        }
        binding.buttonHeaders.setOnClickListener {
            viewModel.onHeadersButtonClicked()
        }
        binding.buttonRequestBody.setOnClickListener {
            viewModel.onRequestBodyButtonClicked()
        }
        binding.buttonAuthentication.setOnClickListener {
            viewModel.onAuthenticationButtonClicked()
        }
        binding.buttonResponseHandling.setOnClickListener {
            viewModel.onResponseHandlingButtonClicked()
        }
        binding.buttonScripting.setOnClickListener {
            viewModel.onScriptingButtonClicked()
        }
        binding.buttonTriggerShortcuts.setOnClickListener {
            viewModel.onTriggerShortcutsButtonClicked()
        }
        binding.buttonExecutionSettings.setOnClickListener {
            viewModel.onExecutionSettingsButtonClicked()
        }
        binding.buttonAdvancedTechnicalSettings.setOnClickListener {
            viewModel.onAdvancedSettingsButtonClicked()
        }

        binding.inputShortcutName
            .observeTextChanges()
            .subscribe { name ->
                viewModel.onShortcutNameChanged(name.toString())
            }
            .attachTo(destroyer)
        binding.inputDescription
            .observeTextChanges()
            .subscribe { description ->
                viewModel.onShortcutDescriptionChanged(description.toString())
            }
            .attachTo(destroyer)
    }

    private fun initViewModelBindings() {
        viewModel.viewState.observe(this) { viewState ->
            binding.loadingIndicator.visible = false
            binding.mainView.visible = true
            val type = viewState.shortcutExecutionType
            setTitle(viewState.toolbarTitle)
            setSubtitle(viewState.toolbarSubtitle)
            binding.inputIcon.setIcon(viewState.shortcutIcon, animated = true)
            binding.inputShortcutName.setTextSafely(viewState.shortcutName)
            binding.inputDescription.setTextSafely(viewState.shortcutDescription)
            binding.buttonBasicRequestSettings.visible = type.usesUrl
            binding.dividerBelowBasicRequestSettings.visible = type.usesUrl
            binding.buttonHeaders.visible = type.usesRequestOptions
            binding.dividerBelowHeaders.visible = type.usesRequestOptions
            binding.buttonRequestBody.visible = type.usesRequestOptions
            binding.dividerBelowRequestBody.visible = type.usesRequestOptions
            binding.buttonAuthentication.visible = type.usesRequestOptions
            binding.dividerBelowAuthentication.visible = type.usesRequestOptions
            binding.buttonResponseHandling.visible = type.usesResponse
            binding.buttonAdvancedTechnicalSettings.visible = type.usesRequestOptions
            binding.buttonScripting.visible = type.usesScriptingEditor
            binding.buttonTriggerShortcuts.visible = type == ShortcutExecutionType.TRIGGER
            binding.dividerBelowScripting.visible = type.usesScriptingEditor || type == ShortcutExecutionType.TRIGGER
            binding.buttonBasicRequestSettings.setSubtitle(viewState.basicSettingsSubtitle)
            binding.buttonHeaders.setSubtitle(viewState.headersSubtitle)
            binding.buttonRequestBody.setSubtitle(viewState.requestBodySettingsSubtitle)
            binding.buttonAuthentication.setSubtitle(viewState.authenticationSettingsSubtitle)
            binding.buttonScripting.setSubtitle(viewState.scriptingSubtitle)
            binding.buttonTriggerShortcuts.setSubtitle(viewState.triggerShortcutsSubtitle)
            binding.buttonRequestBody.isEnabled = viewState.requestBodyButtonEnabled
            testMenuItem?.isVisible = viewState.testButtonVisible
            saveMenuItem?.isVisible = viewState.saveButtonVisible
        }
        viewModel.events.observe(this, ::handleEvent)
    }

    override val navigateUpIcon = R.drawable.ic_clear

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.editor_activity_menu, menu)
        saveMenuItem = menu.findItem(R.id.action_save_shortcut)
        testMenuItem = menu.findItem(R.id.action_test_shortcut)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId) {
            R.id.action_save_shortcut -> consume {
                logInfo("Clicked Save button in shortcut editor")
                viewModel.onSaveButtonClicked()
            }
            R.id.action_test_shortcut -> consume {
                logInfo("Clicked Test button in shortcut editor")
                viewModel.onTestButtonClicked()
            }
            else -> super.onOptionsItemSelected(item)
        }

    override fun handleEvent(event: ViewModelEvent) {
        when (event) {
            is ShortcutEditorEvent.FocusNameInputField -> {
                binding.inputShortcutName.focus()
            }
            else -> super.handleEvent(event)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        iconPicker.handleResult(requestCode, resultCode, intent)
    }

    override fun onBackPressed() {
        viewModel.onBackPressed()
    }

    class IntentBuilder : BaseIntentBuilder(ShortcutEditorActivity::class.java) {

        fun shortcutId(shortcutId: String) = also {
            intent.putExtra(EXTRA_SHORTCUT_ID, shortcutId)
        }

        fun categoryId(categoryId: String) = also {
            intent.putExtra(EXTRA_CATEGORY_ID, categoryId)
        }

        fun curlCommand(command: CurlCommand) = also {
            intent.putExtra(EXTRA_CURL_COMMAND, command)
        }

        fun executionType(type: ShortcutExecutionType) = also {
            intent.putExtra(EXTRA_EXECUTION_TYPE, type.type)
        }
    }

    companion object {

        private const val EXTRA_SHORTCUT_ID = "shortcutId"
        private const val EXTRA_CATEGORY_ID = "categoryId"
        private const val EXTRA_CURL_COMMAND = "curlCommand"
        private const val EXTRA_EXECUTION_TYPE = "executionType"

        const val RESULT_SHORTCUT_ID = "shortcutId"
    }
}
