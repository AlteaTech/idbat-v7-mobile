package com.idbat.mobile.data.model

sealed class PassageSaveState {
    object Idle    : PassageSaveState()
    object Saving  : PassageSaveState()
    object Success : PassageSaveState()
    data class Error(val message: String) : PassageSaveState()
}