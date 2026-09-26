package com.noora.assistant.voice

/** Parses explicit owner statements such as: "NOORA, this is my friend/cousin." */
class AuthorizationPhraseParser {
    fun authorizeGuest(text:String):Boolean {
        val t=text.lowercase()
        val hasNoora=t.contains("noora")
        val hasIdentity=t.contains("this is my") || t.contains("ye mera") || t.contains("ye meri")
        return hasNoora && hasIdentity
    }
}
