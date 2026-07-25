package com.piotrbrzezina.expoguide

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AdminActivity : AppCompatActivity() {

    private lateinit var btnDownloadLlm: Button
    private lateinit var tvAdminStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        btnDownloadLlm = findViewById(R.id.btnDownloadLlm)
        tvAdminStatus = findViewById(R.id.tvAdminStatus)
        
        checkModelStatus()

        btnDownloadLlm.setOnClickListener {
            downloadLlmModel()
        }
    }

    private fun checkModelStatus() {
        val modelFile = File(filesDir, "llm_model.bin")
        if (modelFile.exists()) {
            tvAdminStatus.text = "Model LLM jest już pobrany."
        } else {
            tvAdminStatus.text = "Brak modelu LLM. Pobierz go używając przycisku wyżej."
        }
    }

    private fun downloadLlmModel() {
        btnDownloadLlm.isEnabled = false
        tvAdminStatus.text = "Rozpoczynam pobieranie modelu LLM..."
        
        lifecycleScope.launch(Dispatchers.IO) {
            val modelFile = File(filesDir, "llm_model.bin")
            
            // Symulacja pobierania
            delay(2000)
            
            if (!modelFile.exists()) {
                modelFile.createNewFile()
            }
            
            withContext(Dispatchers.Main) {
                tvAdminStatus.text = "Model LLM został pomyślnie pobrany i zapisany jako llm_model.bin."
                btnDownloadLlm.isEnabled = true
            }
        }
    }
}
