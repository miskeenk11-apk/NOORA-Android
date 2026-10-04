package com.noora.assistant.ai

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Calls NOORA's server-side gateway. No provider API key is stored in Android. */
class GatewayAiClient(
    private val gatewayUrl: String,
    private val gatewayToken: String? = null
) {
    fun chat(messages: List<Pair<String, String>>): Result<String> = runCatching {
        val connection = (URL(gatewayUrl.trimEnd('/') + "/v1/chat").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 60_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            gatewayToken?.takeIf { it.isNotBlank() }?.let { setRequestProperty("Authorization", "Bearer $it") }
        }
        val array = JSONArray()
        messages.forEach { (role, content) ->
            array.put(JSONObject().put("role", role).put("content", content))
        }
        val body = JSONObject().put("messages", array).toString()
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (connection.responseCode !in 200..299) {
            throw IllegalStateException(JSONObject(response).optString("error", "Gateway error ${connection.responseCode}"))
        }
        JSONObject(response).optString("reply").ifBlank { throw IllegalStateException("Gateway returned no text") }
    }
}
