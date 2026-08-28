package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.data.ai.GeminiApiRequest
import com.example.data.ai.GeminiContent
import com.example.data.ai.GeminiGenerationConfig
import com.example.data.ai.GeminiPart
import com.example.data.ai.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

object ApiKeyManager {
    const val DEFAULT_PUBLIC_API_KEY = "YOUR_API_KEY_HERE"
    private const val PREFS_NAME = "gemini_api_key_prefs"
    private const val KEY_CUSTOM_API_KEY = "custom_gemini_api_key"

    private var sharedPreferences: SharedPreferences? = null
    private val _customApiKey = MutableStateFlow("")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    fun init(context: Context) {
        if (sharedPreferences == null) {
            val appContext = context.applicationContext
            val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sharedPreferences = prefs
            val savedKey = prefs.getString(KEY_CUSTOM_API_KEY, "") ?: ""
            _customApiKey.value = savedKey
        }
    }

    fun getActiveApiKey(): String {
        val custom = _customApiKey.value.trim()
        if (custom.isNotBlank()) {
            return custom
        }
        val buildConfigKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
        if (buildConfigKey.isNotBlank() && buildConfigKey != "MY_GEMINI_API_KEY") {
            return buildConfigKey.trim()
        }
        return DEFAULT_PUBLIC_API_KEY
    }

    fun isUsingCustomKey(): Boolean {
        return _customApiKey.value.trim().isNotBlank()
    }

    fun setCustomApiKey(key: String) {
        val trimmed = key.trim()
        _customApiKey.value = trimmed
        sharedPreferences?.edit()?.putString(KEY_CUSTOM_API_KEY, trimmed)?.apply()
    }

    fun resetToDefault() {
        _customApiKey.value = ""
        sharedPreferences?.edit()?.remove(KEY_CUSTOM_API_KEY)?.apply()
    }

    fun getMaskedActiveKey(): String {
        val active = getActiveApiKey()
        if (active.length <= 8) return "••••••••"
        val prefix = active.take(4)
        val suffix = active.takeLast(4)
        return "$prefix••••••••$suffix"
    }

    suspend fun testApiKey(keyToTest: String): Result<String> = withContext(Dispatchers.IO) {
        val key = keyToTest.trim().ifBlank { getActiveApiKey() }
        try {
            val testRequest = GeminiApiRequest(
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(text = "Hello Gemini, reply with 'OK'"))
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    maxOutputTokens = 10,
                    temperature = 0.1f
                )
            )
            val response = RetrofitClient.geminiService.generateContent(key, testRequest)
            val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
            if (!reply.isNullOrBlank()) {
                Result.success("Connection successful! Gemini replied: $reply")
            } else {
                Result.failure(Exception("Received empty response from Gemini API."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
