package com.mcis.memoir.ui.memory.edit

sealed interface EditIntent {
    data object SaveClicked : EditIntent
}
