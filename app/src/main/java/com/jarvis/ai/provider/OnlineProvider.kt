package com.jarvis.ai.provider

import com.google.gson.JsonParser
import com.jarvis.core.common.DispatcherProvider
import com.jarvis.core.network.ConnectivityMonitor
import com.jarvis.core.network.ConnectivityState
import com.jarvis.core.security.AuditLogger
import com.jarvis.core.security.PromptInjectionGuard
import com.jarvis.data.datastore.SecureSettingsRepository
import com.jarvis.data.datastore.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Optional online provider (OpenAI-compatible chat completions).
 *
 * Hard rules enforced here:
 * - Refuses to run unless online mode is enabled AND consent was given AND
 *   an API key is configured AND the device is actually online.
 * - API key is read from encrypted storage and never logged.
 * - External content is sanitised before sending.
 * - Every request is audit-logged (metadata only).
 */
@Singleton
class OnlineProvider @Inject constructor(
    private val settings: SettingsRepository,
    private val secureSettings: SecureSettingsRepository,
    private val connectivity: ConnectivityMonitor,
    private val auditLogger: AuditLogger,
    private val dispatchers: DispatcherProvider
) : AiProvider {

    override val name: String = "online-openai-compatible"

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override val isAvailable: Boolean
        get() = secureSettings.getApiKey() != null

    /** Checks all gates. Returns a user-presentable reason if blocked. */
    suspend fun gateReason(): String? {
        if (settings.offlineMode.first()) return "Offline mode is on. Turn it off in Settings to use online AI."
        if (!settings.onlineModeEnabled.first()) return "Online mode is not enabled. Enable it in Settings first."
        if (!settings.onlineConsentGiven.first()) return "Online consent has not been given. Review the privacy notice in Settings."
        if (secureSettings.getApiKey() == null) return "No API key configured. Add one in Model settings."
        if (connectivity.state.value == ConnectivityState.Offline) return "No internet connection."
        return null
    }

    override suspend fun respond(prompt: String): Result<String> {
        gateReason()?.let { return Result.failure(IllegalStateException(it)) }
        val apiKey = secureSettings.getApiKey()
            ?: return Result.failure(IllegalStateException("No API key configured."))
        val baseUrl = settings.onlineBaseUrl.first().trimEnd('/')
        val model = settings.onlineModel.first()
        val safePrompt = PromptInjectionGuard.sanitise(prompt)

        val body = """
            {"model":"$model","messages":[{"role":"system","content":"You are JARVIS, a concise personal assistant. Answer briefly."},{"role":"user","content":${jsonString(safePrompt)}}],"max_tokens":512}
        """.trimIndent()

        val request = Request.Builder()
            .url("$baseUrl/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            // Network on IO dispatcher — never on the main thread.
            withContext(dispatchers.io) {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        auditLogger.onlineRequest(name, false)
                        Result.failure(
                            IllegalStateException(
                                "Online provider returned HTTP ${response.code}. Check your API key and model name."
                            )
                        )
                    } else {
                        val content = JsonParser.parseString(responseBody)
                            .asJsonObject
                            .getAsJsonArray("choices")
                            .get(0).asJsonObject
                            .getAsJsonObject("message")
                            .get("content").asString
                        auditLogger.onlineRequest(name, true)
                        Result.success(content.trim())
                    }
                }
            }
        } catch (e: Exception) {
            auditLogger.onlineRequest(name, false)
            Result.failure(IllegalStateException("Online request failed: ${e.message}"))
        }
    }

    private fun jsonString(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""
}
