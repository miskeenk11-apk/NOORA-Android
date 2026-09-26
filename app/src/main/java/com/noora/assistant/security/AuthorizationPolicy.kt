package com.noora.assistant.security

/** Owner/guest policy. Face/voice recognition is convenience; sensitive actions still require Android/biometric verification. */
class AuthorizationPolicy {
    enum class Person { OWNER, AUTHORIZED_GUEST, UNKNOWN }
    fun mayUseAssistant(person: Person)=person!=Person.UNKNOWN
    fun requiresStrongAuth(action:String)=action in setOf("payments","passwords","security","account_changes")
}
