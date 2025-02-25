package ch.rmy.android.http_shortcuts.scripting.actions.types

import ch.rmy.android.http_shortcuts.scripting.ActionAlias
import ch.rmy.android.http_shortcuts.scripting.actions.ActionDTO

class VibrateActionType : BaseActionType() {

    override val type = TYPE

    override fun fromDTO(actionDTO: ActionDTO) = VibrateAction(
        patternId = actionDTO.getInt(KEY_PATTERN) ?: 0,
        waitForCompletion = actionDTO.getBoolean(KEY_WAIT_FOR_COMPLETION) ?: false,
    )

    override fun getAlias() = ActionAlias(
        functionName = FUNCTION_NAME,
        parameters = listOf(KEY_PATTERN, KEY_WAIT_FOR_COMPLETION),
    )

    companion object {

        const val TYPE = "vibrate"
        const val FUNCTION_NAME = "vibrate"

        const val KEY_PATTERN = "pattern"
        const val KEY_WAIT_FOR_COMPLETION = "wait"
    }
}
