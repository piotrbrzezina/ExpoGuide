package com.piotrbrzezina.expoguide.admin

data class LanguageModel(
    val id: String,
    val name: String,
    val supportsVision: Boolean,
    val supportsTranslation: Boolean
)
