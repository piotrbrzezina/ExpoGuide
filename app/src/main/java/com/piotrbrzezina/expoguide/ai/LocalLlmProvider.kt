package com.piotrbrzezina.expoguide.ai

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LocalLlmProvider(private val context: Context) : AiProvider {
    
    private var llmInference: LlmInference? = null
    private var currentModelPath: String? = null
    
    private fun getActiveModelPath(): String? {
        val prefs = context.getSharedPreferences("ai_settings", Context.MODE_PRIVATE)
        val activeId = prefs.getString("active_model_id", null) ?: return null
        return File(context.filesDir, "$activeId.bin").absolutePath
    }

    override suspend fun generateContent(prompt: String): String = withContext(Dispatchers.IO) {
        val path = getActiveModelPath() ?: throw Exception("Brak aktywnego modelu. Wybierz model w panelu administracyjnym.")
        val file = File(path)
        if (!file.exists()) {
            throw Exception("Model lokalny nie jest dostępny. Wymagane pobranie.")
        }
        
        // Re-create llmInference if the model path has changed
        if (llmInference == null || currentModelPath != path) {
            llmInference?.close() // clean up old if exists
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(path)
                .setMaxTokens(512)
                .build()
            llmInference = LlmInference.createFromOptions(context, options)
            currentModelPath = path
        }
        
        val response = llmInference?.generateResponse(prompt)
        return@withContext response ?: throw Exception("Błąd generowania odpowiedzi przez lokalny model")
    }
}
