package com.piotrbrzezina.expoguide.ai

interface AiProvider {
    suspend fun generateContent(prompt: String): String
}
