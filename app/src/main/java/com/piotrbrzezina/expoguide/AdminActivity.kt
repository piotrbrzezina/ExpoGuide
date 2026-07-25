package com.piotrbrzezina.expoguide

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.piotrbrzezina.expoguide.admin.LanguageModel
import com.piotrbrzezina.expoguide.admin.ModelsAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AdminActivity : AppCompatActivity() {

    private lateinit var rvModels: RecyclerView
    private lateinit var btnDownloadSelected: Button
    private lateinit var tvAdminStatus: TextView
    private lateinit var adapter: ModelsAdapter

    private val allModels = listOf(
        LanguageModel("gemma_2b", "Gemma 2B", true, true),
        LanguageModel("gemma_7b", "Gemma 7B", true, true),
        LanguageModel("llama_2", "Llama 2", false, true),
        LanguageModel("mistral_v1", "Mistral v1", true, false),
        LanguageModel("phi_3", "Phi-3 Vision", true, true)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        rvModels = findViewById(R.id.rvModels)
        btnDownloadSelected = findViewById(R.id.btnDownloadSelected)
        tvAdminStatus = findViewById(R.id.tvAdminStatus)

        val filteredModels = allModels.filter { it.supportsVision && it.supportsTranslation }

        adapter = ModelsAdapter(
            models = filteredModels,
            isModelDownloaded = { id -> isModelDownloaded(id) },
            getActiveModelId = { getActiveModelId() },
            onDownloadSelected = { _, _ -> }, // nieuzywane bezposrednio tutaj
            onSetActive = { model -> setActiveModel(model) },
            onRemove = { model -> removeModel(model) }
        )

        rvModels.layoutManager = LinearLayoutManager(this)
        rvModels.adapter = adapter

        btnDownloadSelected.setOnClickListener {
            downloadSelectedModels()
        }

        updateStatus()
    }

    private fun isModelDownloaded(id: String): Boolean {
        return File(filesDir, "$id.bin").exists()
    }

    private fun getActiveModelId(): String? {
        val prefs = getSharedPreferences("ai_settings", Context.MODE_PRIVATE)
        return prefs.getString("active_model_id", null)
    }

    private fun setActiveModelId(id: String?) {
        val prefs = getSharedPreferences("ai_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("active_model_id", id).apply()
    }

    private fun updateStatus() {
        val active = getActiveModelId()
        if (active != null && !isModelDownloaded(active)) {
            // Jeśli aktywny model nie jest pobrany, usuwamy z aktywnych
            setActiveModelId(null)
        }
        
        // Zabezpieczenie: jeśli usunięto aktywny model, wybierz inny
        val newActive = getActiveModelId()
        if (newActive == null) {
            val anyDownloaded = allModels.firstOrNull { isModelDownloaded(it.id) }
            if (anyDownloaded != null) {
                setActiveModelId(anyDownloaded.id)
            }
        }

        adapter.notifyDataSetChanged()
    }

    private fun setActiveModel(model: LanguageModel) {
        if (!isModelDownloaded(model.id)) {
            Toast.makeText(this, "Nie można ustawić jako aktywnego modelu, który nie jest pobrany", Toast.LENGTH_SHORT).show()
            return
        }
        setActiveModelId(model.id)
        updateStatus()
    }

    private fun removeModel(model: LanguageModel) {
        try {
            val file = File(filesDir, "${model.id}.bin")
            if (file.exists()) {
                file.delete()
            }
            updateStatus()
        } catch (e: Exception) {
            Toast.makeText(this, "Błąd podczas usuwania modelu", Toast.LENGTH_SHORT).show()
        }
    }

    private fun downloadSelectedModels() {
        val toDownload = adapter.selectedToDownload.toList()
        if (toDownload.isEmpty()) {
            Toast.makeText(this, "Nie wybrano modeli do pobrania", Toast.LENGTH_SHORT).show()
            return
        }

        btnDownloadSelected.isEnabled = false
        tvAdminStatus.text = "Rozpoczynam pobieranie modeli: ${toDownload.joinToString()}"
        
        lifecycleScope.launch(Dispatchers.IO) {
            var successCount = 0
            for (id in toDownload) {
                try {
                    // Symulacja pobierania
                    delay(1500)
                    val modelFile = File(filesDir, "$id.bin")
                    if (!modelFile.exists()) {
                        modelFile.createNewFile()
                    }
                    successCount++
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@AdminActivity, "Błąd pobierania modelu $id", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            
            withContext(Dispatchers.Main) {
                tvAdminStatus.text = "Pobrano $successCount z ${toDownload.size} modeli."
                btnDownloadSelected.isEnabled = true
                adapter.selectedToDownload.clear()
                updateStatus()
            }
        }
    }
}
