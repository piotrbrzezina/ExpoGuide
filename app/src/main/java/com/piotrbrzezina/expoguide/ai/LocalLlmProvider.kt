package com.piotrbrzezina.expoguide.ai

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LocalLlmProvider(private val context: Context) : AiProvider {
    
    private var llmInference: LlmInference? = null
    
    // Scieżka do modelu na urządzeniu, np. po pobraniu
    private val modelPath: String
        get() = File(context.filesDir, "llm_model.bin").absolutePath

    override suspend fun generateContent(prompt: String): String = withContext(Dispatchers.IO) {
        val file = File(modelPath)
        if (!file.exists()) {
            throw Exception("Model lokalny nie jest dostępny. Wymagane pobranie.")
        }
        
        if (llmInference == null) {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(512)
                .build()
            llmInference = LlmInference.createFromOptions(context, options)
        }
        
        val response = llmInference?.generateResponse(prompt)
        return@withContext response ?: throw Exception("Błąd generowania odpowiedzi przez lokalny model")
    }
}
