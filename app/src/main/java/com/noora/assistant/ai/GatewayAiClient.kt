package com.noora.assistant.ai

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class GatewayAiClient(
    private val gatewayUrl: String,
    private val gatewayToken: String? = null
) {
    fun isConfigured(): Boolean =
        gatewayUrl.isNotBlank() && !gatewayUrl.contains("YOUR-NOORA-GATEWAY", true)

    fun chat(messages: List<Pair<String, String>>): Result<String> = runCatching {
        check(isConfigured()) { "NOORA AI gateway is not configured." }
        val connection=(URL(gatewayUrl.trimEnd('/')+"/v1/chat").openConnection() as HttpURLConnection).apply {
            requestMethod="POST"; connectTimeout=15_000; readTimeout=60_000; doOutput=true
            setRequestProperty("Content-Type","application/json")
            gatewayToken?.takeIf{it.isNotBlank()}?.let{setRequestProperty("Authorization","Bearer $it")}
        }
        val array=JSONArray()
        messages.forEach{(role,content)->array.put(JSONObject().put("role",role).put("content",content))}
        connection.outputStream.use{it.write(JSONObject().put("messages",array).toString().toByteArray(Charsets.UTF_8))}
        val code=connection.responseCode
        val stream=if(code in 200..299) connection.inputStream else connection.errorStream
        val response=stream?.bufferedReader()?.use{it.readText()}.orEmpty()
        if(code !in 200..299) throw IllegalStateException(JSONObject(response).optString("error","Gateway error $code"))
        JSONObject(response).optString("text").ifBlank{throw IllegalStateException("Gateway returned no text")}
    }
}
