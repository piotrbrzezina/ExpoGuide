package com.piotrbrzezina.expoguide

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var btnCapture: Button
    private lateinit var btnGallery: Button
    private lateinit var btnAskAi: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvTranslatedText: TextView
    private lateinit var tvAiResponse: TextView

    private lateinit var tts: TextToSpeech
    private lateinit var translator: Translator
    private var isTranslatorReady = false

    // Placeholder for API Key. W prawdziwej aplikacji uzyj bezpieczniejszego mechanizmu.
    private val GEMINI_API_KEY = "YOUR_GEMINI_API_KEY_HERE"

    private val takePicturePreview = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            processImage(bitmap)
        } else {
            Toast.makeText(this, "Nie udało się zrobić zdjęcia", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickVisualMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            loadBitmapFromUri(uri)
        }
    }

    private val pickImageFallback = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            loadBitmapFromUri(uri)
        }
    }

    private val requestStoragePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            pickImageFallback.launch("image/*")
        } else {
            Toast.makeText(this, "Brak uprawnień do odczytu galerii", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnCapture = findViewById(R.id.btnCapture)
        btnGallery = findViewById(R.id.btnGallery)
        btnAskAi = findViewById(R.id.btnAskAi)
        tvStatus = findViewById(R.id.tvStatus)
        tvTranslatedText = findViewById(R.id.tvTranslatedText)
        tvAiResponse = findViewById(R.id.tvAiResponse)

        btnCapture.isEnabled = false
        btnGallery.isEnabled = false
        
        // Initialize TTS
        tts = TextToSpeech(this, this)

        // Initialize Translator
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.POLISH)
            .build()
        translator = Translation.getClient(options)
        
        tvStatus.text = "Pobieranie modelu tłumaczenia..."
        translator.downloadModelIfNeeded()
            .addOnSuccessListener {
                isTranslatorReady = true
                tvStatus.text = "Gotowe. Możesz zrobić zdjęcie."
                btnCapture.isEnabled = true
                btnGallery.isEnabled = true
            }
            .addOnFailureListener { exception ->
                tvStatus.text = "Błąd pobierania modelu: ${exception.message}"
            }

        btnCapture.setOnClickListener {
            takePicturePreview.launch(null)
        }

        btnGallery.setOnClickListener {
            openGallery()
        }

        btnAskAi.setOnClickListener {
            askAiForTrivia(tvTranslatedText.text.toString())
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale("pl", "PL"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Język polski nie jest obsługiwany lub brakuje danych.")
            }
        }
    }

    private fun processImage(bitmap: Bitmap) {
        tvStatus.text = "Rozpoznawanie tekstu (OCR)..."
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val recognizedText = visionText.text
                if (recognizedText.isNotBlank()) {
                    translateText(recognizedText)
                } else {
                    tvStatus.text = "Nie znaleziono tekstu na zdjęciu."
                }
            }
            .addOnFailureListener { e ->
                tvStatus.text = "Błąd OCR: ${e.message}"
            }
    }

    private fun translateText(text: String) {
        if (!isTranslatorReady) {
            tvStatus.text = "Model tłumacza jeszcze się pobiera..."
            return
        }
        tvStatus.text = "Tłumaczenie..."
        translator.translate(text)
            .addOnSuccessListener { translatedText ->
                tvStatus.text = "Sukces!"
                tvTranslatedText.text = translatedText
                
                // Read text aloud
                tts.speak(translatedText, TextToSpeech.QUEUE_FLUSH, null, "read_text")
                
                // Show AI button
                btnAskAi.visibility = View.VISIBLE
            }
            .addOnFailureListener { e ->
                tvStatus.text = "Błąd tłumaczenia: ${e.message}"
            }
    }

    private fun askAiForTrivia(contextText: String) {
        if (GEMINI_API_KEY == "YOUR_GEMINI_API_KEY_HERE") {
            tvAiResponse.text = "Brak klucza API Gemini. Skonfiguruj klucz w kodzie (MainActivity.kt)."
            return
        }

        btnAskAi.isEnabled = false
        tvAiResponse.text = "AI myśli..."

        val generativeModel = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = GEMINI_API_KEY
        )

        val prompt = "Oto tekst z tabliczki w muzeum:\n$contextText\n\nPodaj 3 najciekawsze, nietypowe fakty o tym eksponacie, których nie było na tabliczce."

        lifecycleScope.launch {
            try {
                val response = generativeModel.generateContent(prompt)
                tvAiResponse.text = response.text
                
                // Read AI response aloud
                tts.speak(response.text ?: "", TextToSpeech.QUEUE_FLUSH, null, "ai_response")
            } catch (e: Exception) {
                tvAiResponse.text = "Błąd AI: ${e.message}"
            } finally {
                btnAskAi.isEnabled = true
            }
        }
    }

    private fun openGallery() {
        if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(this)) {
            pickVisualMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
                pickImageFallback.launch("image/*")
            } else {
                requestStoragePermission.launch(permission)
            }
        } else {
            pickImageFallback.launch("image/*")
        }
    }

    private fun loadBitmapFromUri(uri: Uri) {
        try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
            } else {
                contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
            if (bitmap != null) {
                processImage(bitmap)
            } else {
                Toast.makeText(this, "Nie udało się wczytać zdjęcia", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Błąd wczytywania zdjęcia: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.stop()
        tts.shutdown()
        translator.close()
    }
}
