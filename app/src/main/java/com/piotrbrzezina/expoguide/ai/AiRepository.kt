package com.piotrbrzezina.expoguide.ai

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class AiRepository(
    private val localProvider: AiProvider,
    private val fallbackProvider: AiProvider,
    private val sharedPreferences: SharedPreferences
) {
    suspend fun generateContent(prompt: String): String {
        val skipLocalAi = sharedPreferences.getBoolean("skip_local_ai", false)
        
        if (!skipLocalAi) {
            try {
                Log.d("AiRepository", "Próba użycia lokalnego LLM...")
                val localResponse = localProvider.generateContent(prompt)
                Log.d("AiRepository", "Lokalny LLM odpowiedział pomyślnie.")
                return localResponse
            } catch (e: Exception) {
                Log.e("AiRepository", "Błąd lokalnego LLM: ${e.message}, użycie Fallbacku.")
            }
        } else {
            Log.d("AiRepository", "Pominięto lokalne AI z powodu ustawień.")
        }

        // Fallback
        Log.d("AiRepository", "Użycie Fallback (API w chmurze)...")
        return fallbackProvider.generateContent(prompt)
    }
}
