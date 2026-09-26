package com.noora.assistant.recognition

/**
 * Privacy-first recognition coordinator.
 * The actual ML recognizer can be plugged in later without changing the UI or policy layer.
 */
class UserRecognitionManager {
    var enabled:Boolean=false
    private var ownerKnown=false
    fun registerOwner(){ ownerKnown=true }
    fun isOwnerKnown()=ownerKnown
    fun classify(label:String?): PersonResult = when {
        !enabled -> PersonResult.DISABLED
        label==null -> PersonResult.UNKNOWN
        label.equals("owner", true) -> PersonResult.OWNER
        label.equals("guest", true) -> PersonResult.AUTHORIZED_GUEST
        else -> PersonResult.UNKNOWN
    }
    enum class PersonResult { DISABLED, OWNER, AUTHORIZED_GUEST, UNKNOWN }
}
