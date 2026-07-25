import re

with open('app/src/main/java/com/piotrbrzezina/expoguide/MainActivity.kt', 'r') as f:
    content = f.read()

# Add imports
imports = """import android.speech.tts.UtteranceProgressListener
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
"""
content = re.sub(r'(import android.widget.Toast\n)', r'\1' + imports, content)

# Add properties
fields = """    private lateinit var llTtsControls: View
    private lateinit var btnTtsPlayPause: Button
    private lateinit var btnTtsStop: Button
    private lateinit var btnTtsRewind: Button
    private lateinit var btnTtsForward: Button
    private lateinit var sbTtsSpeed: SeekBar

    private var ttsSentences = listOf<String>()
    private var currentSentenceIndex = 0
    private var isPlayingTts = false
    private var isPaused = false
"""
content = re.sub(r'(private var isTranslatorReady = false\n)', r'\1\n' + fields, content)

# In onCreate
on_create_bind = """        llTtsControls = findViewById(R.id.llTtsControls)
        btnTtsPlayPause = findViewById(R.id.btnTtsPlayPause)
        btnTtsStop = findViewById(R.id.btnTtsStop)
        btnTtsRewind = findViewById(R.id.btnTtsRewind)
        btnTtsForward = findViewById(R.id.btnTtsForward)
        sbTtsSpeed = findViewById(R.id.sbTtsSpeed)

        setupTtsControls()
"""
content = re.sub(r'(tvAiResponse = findViewById\(R.id.tvAiResponse\)\n)', r'\1\n' + on_create_bind, content)

# Methods
methods = """    private fun setupTtsControls() {
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
"""

content = re.sub(r'(override fun onInit\(status: Int\) \{)', methods + r'\n    \1', content)

# Modify onInit
on_init_body = """        if (status == TextToSpeech.SUCCESS) {
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
        }"""
content = re.sub(r'if \(status == TextToSpeech.SUCCESS\) \{[\s\S]*?\}\n        \}', on_init_body, content)

# Replace tts.speak calls
content = re.sub(r'tts\.speak\(translatedText, TextToSpeech\.QUEUE_FLUSH, null, "read_text"\)', r'readTextWithControls(translatedText)', content)
content = re.sub(r'tts\.speak\(response\.text \?: "", TextToSpeech\.QUEUE_FLUSH, null, "ai_response"\)', r'readTextWithControls(response.text ?: "")', content)

with open('app/src/main/java/com/piotrbrzezina/expoguide/MainActivity.kt', 'w') as f:
    f.write(content)
