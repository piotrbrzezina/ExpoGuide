package com.piotrbrzezina.expoguide.ai

import com.google.ai.client.generativeai.GenerativeModel

class FallbackAiProvider(private val apiKey: String) : AiProvider {
    override suspend fun generateContent(prompt: String): String {
        val generativeModel = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey
        )
        val response = generativeModel.generateContent(prompt)
        return response.text ?: throw Exception("Pusta odpowiedź z API Gemini")
    }
}
