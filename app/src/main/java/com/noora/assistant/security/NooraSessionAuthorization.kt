package com.noora.assistant.security

/**
 * Session-level authorization. Recognition is treated as convenience; sensitive
 * operations should still use Android biometric/PIN confirmation.
 */
class NooraSessionAuthorization {
    enum class State { OWNER, AUTHORIZED_GUEST, UNKNOWN }

    var recognitionEnabled: Boolean = false
        private set

    var state: State = State.OWNER
        private set

    fun setRecognitionEnabled(enabled: Boolean) {
        recognitionEnabled = enabled
        state = if (enabled) State.UNKNOWN else State.OWNER
    }

    fun authorizeOwner() { state = State.OWNER }
    fun authorizeGuest() { state = State.AUTHORIZED_GUEST }
    fun clearAuthorization() { state = if (recognitionEnabled) State.UNKNOWN else State.OWNER }

    fun mayUseAssistant(): Boolean = state != State.UNKNOWN
}
