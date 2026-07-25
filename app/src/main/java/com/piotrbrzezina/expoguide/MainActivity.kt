package com.piotrbrzezina.expoguide

import android.graphics.Bitmap
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.speech.tts.UtteranceProgressListener
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Switch
import android.content.Context
import android.content.SharedPreferences
import com.piotrbrzezina.expoguide.ai.AiRepository
import com.piotrbrzezina.expoguide.ai.LocalLlmProvider
import com.piotrbrzezina.expoguide.ai.FallbackAiProvider
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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
    private lateinit var btnAskAi: Button
    private lateinit var switchSkipLocalAi: Switch
    private lateinit var tvStatus: TextView
    private lateinit var tvTranslatedText: TextView
    private lateinit var tvAiResponse: TextView

    private lateinit var aiRepository: AiRepository
    private lateinit var sharedPrefs: SharedPreferences

    private lateinit var tts: TextToSpeech
    private lateinit var translator: Translator
    private var isTranslatorReady = false

    private lateinit var llTtsControls: View
    private lateinit var btnTtsPlayPause: Button
    private lateinit var btnTtsStop: Button
    private lateinit var btnTtsRewind: Button
    private lateinit var btnTtsForward: Button
    private lateinit var sbTtsSpeed: SeekBar

    private var ttsSentences = listOf<String>()
    private var currentSentenceIndex = 0
    private var isPlayingTts = false
    private var isPaused = false

    // Placeholder for API Key. W prawdziwej aplikacji uzyj bezpieczniejszego mechanizmu.
    private val GEMINI_API_KEY = "YOUR_GEMINI_API_KEY_HERE"

    private val takePicturePreview = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            processImage(bitmap)
        } else {
            Toast.makeText(this, "Nie udało się zrobić zdjęcia", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnCapture = findViewById(R.id.btnCapture)
        btnAskAi = findViewById(R.id.btnAskAi)
        switchSkipLocalAi = findViewById(R.id.switchSkipLocalAi)
        tvStatus = findViewById(R.id.tvStatus)
        tvTranslatedText = findViewById(R.id.tvTranslatedText)
        tvAiResponse = findViewById(R.id.tvAiResponse)

        sharedPrefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        switchSkipLocalAi.isChecked = sharedPrefs.getBoolean("skip_local_ai", false)
        switchSkipLocalAi.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("skip_local_ai", isChecked).apply()
        }

        aiRepository = AiRepository(
            localProvider = LocalLlmProvider(this),
            fallbackProvider = FallbackAiProvider(GEMINI_API_KEY),
            sharedPreferences = sharedPrefs
        )

        llTtsControls = findViewById(R.id.llTtsControls)
        btnTtsPlayPause = findViewById(R.id.btnTtsPlayPause)
        btnTtsStop = findViewById(R.id.btnTtsStop)
        btnTtsRewind = findViewById(R.id.btnTtsRewind)
        btnTtsForward = findViewById(R.id.btnTtsForward)
        sbTtsSpeed = findViewById(R.id.sbTtsSpeed)

        setupTtsControls()

        btnCapture.isEnabled = false
        
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
            }
            .addOnFailureListener { exception ->
                tvStatus.text = "Błąd pobierania modelu: ${exception.message}"
            }

        btnCapture.setOnClickListener {
            takePicturePreview.launch(null)
        }

        btnAskAi.setOnClickListener {
            askAiForTrivia(tvTranslatedText.text.toString())
        }
    }

        private fun setupTtsControls() {
        btnTtsPlayPause.setOnClickListener {
            if (isPlayingTts) {
                if (isPaused) {
                    resumeTts()
                } else {
                    pauseTts()
                }
            } else {
                startReadingFrom(0)
            }
        }
        btnTtsStop.setOnClickListener {
            stopTts()
            startReadingFrom(0) // Reset to beginning but don't play immediately
            pauseTts() // To keep it paused
        }
        btnTtsRewind.setOnClickListener {
            if (currentSentenceIndex > 0) {
                currentSentenceIndex--
                startReadingFrom(currentSentenceIndex)
                if (isPaused) pauseTts()
            }
        }
        btnTtsForward.setOnClickListener {
            if (currentSentenceIndex < ttsSentences.size - 1) {
                currentSentenceIndex++
                startReadingFrom(currentSentenceIndex)
                if (isPaused) pauseTts()
            }
        }
        sbTtsSpeed.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val speed = progress / 100f
                tts.setSpeechRate(if (speed < 0.1f) 0.1f else speed)
                if (isPlayingTts && !isPaused) {
                    startReadingFrom(currentSentenceIndex)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun readTextWithControls(text: String) {
        ttsSentences = text.split(Regex("(?<=[.!?])\\\\s+")).filter { it.isNotBlank() }
        runOnUiThread {
            llTtsControls.visibility = View.VISIBLE
        }
        startReadingFrom(0)
    }

    private fun startReadingFrom(index: Int) {
        currentSentenceIndex = index
        if (ttsSentences.isEmpty() || index >= ttsSentences.size) {
            isPlayingTts = false
            runOnUiThread { btnTtsPlayPause.text = "▶" }
            return
        }
        isPlayingTts = true
        isPaused = false
        runOnUiThread { btnTtsPlayPause.text = "⏸" }
        
        tts.stop()
        val textToSpeak = ttsSentences[index]
        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "utterance_$index")
        tts.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, "utterance_$index")
    }

    private fun pauseTts() {
        isPaused = true
        tts.stop()
        runOnUiThread { btnTtsPlayPause.text = "▶" }
    }

    private fun resumeTts() {
        isPaused = false
        startReadingFrom(currentSentenceIndex)
    }

    private fun stopTts() {
        isPlayingTts = false
        isPaused = false
        tts.stop()
        runOnUiThread { btnTtsPlayPause.text = "▶" }
    }

    override fun onInit(status: Int) {
                if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale("pl", "PL"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Język polski nie jest obsługiwany lub brakuje danych.")
            }
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    if (isPlayingTts && !isPaused) {
                        currentSentenceIndex++
                        if (currentSentenceIndex < ttsSentences.size) {
                            val textToSpeak = ttsSentences[currentSentenceIndex]
                            val params = Bundle()
                            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "utterance_$currentSentenceIndex")
                            tts.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, "utterance_$currentSentenceIndex")
                        } else {
                            isPlayingTts = false
                            runOnUiThread { btnTtsPlayPause.text = "▶" }
                        }
                    }
                }
                override fun onError(utteranceId: String?) {}
            })
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
                readTextWithControls(translatedText)
                
                // Show AI button
                btnAskAi.visibility = View.VISIBLE
            }
            .addOnFailureListener { e ->
                tvStatus.text = "Błąd tłumaczenia: ${e.message}"
            }
    }

    private fun askAiForTrivia(contextText: String) {
        if (GEMINI_API_KEY == "YOUR_GEMINI_API_KEY_HERE" && sharedPrefs.getBoolean("skip_local_ai", false)) {
            tvAiResponse.text = "Brak klucza API Gemini dla fallbacku. Skonfiguruj klucz w kodzie (MainActivity.kt)."
            return
        }

        btnAskAi.isEnabled = false
        tvAiResponse.text = "AI myśli..."

        val prompt = "Oto tekst z tabliczki w muzeum:\n$contextText\n\nPodaj 3 najciekawsze, nietypowe fakty o tym eksponacie, których nie było na tabliczce."

        lifecycleScope.launch {
            try {
                val responseText = aiRepository.generateContent(prompt)
                tvAiResponse.text = responseText
                
                // Read AI response aloud
                readTextWithControls(responseText)
            } catch (e: Exception) {
                tvAiResponse.text = "Błąd AI: ${e.message}"
            } finally {
                btnAskAi.isEnabled = true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.stop()
        tts.shutdown()
        translator.close()
    }
}
