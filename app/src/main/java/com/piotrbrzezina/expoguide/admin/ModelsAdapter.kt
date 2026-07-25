package com.piotrbrzezina.expoguide.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.piotrbrzezina.expoguide.R

class ModelsAdapter(
    private var models: List<LanguageModel>,
    private val isModelDownloaded: (String) -> Boolean,
    private val getActiveModelId: () -> String?,
    private val onDownloadSelected: (LanguageModel, Boolean) -> Unit,
    private val onSetActive: (LanguageModel) -> Unit,
    private val onRemove: (LanguageModel) -> Unit
) : RecyclerView.Adapter<ModelsAdapter.ViewHolder>() {

    val selectedToDownload = mutableSetOf<String>()

    fun updateData(newModels: List<LanguageModel>) {
        models = newModels
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_language_model, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = models[position]
        holder.bind(model)
    }

    override fun getItemCount() = models.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvModelName: TextView = view.findViewById(R.id.tvModelName)
        private val tvActiveMark: TextView = view.findViewById(R.id.tvActiveMark)
        private val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        private val cbSelect: CheckBox = view.findViewById(R.id.cbSelect)
        private val btnSetActive: Button = view.findViewById(R.id.btnSetActive)
        private val btnRemove: Button = view.findViewById(R.id.btnRemove)

        fun bind(model: LanguageModel) {
            tvModelName.text = model.name
            val downloaded = isModelDownloaded(model.id)
            val active = getActiveModelId() == model.id

            tvActiveMark.visibility = if (active) View.VISIBLE else View.GONE
            
            if (downloaded) {
                tvStatus.text = "Status: Pobrano"
                cbSelect.visibility = View.GONE
                cbSelect.isChecked = false
                selectedToDownload.remove(model.id)
            } else {
                tvStatus.text = "Status: Niepobrano"
                cbSelect.visibility = View.VISIBLE
                cbSelect.setOnCheckedChangeListener(null)
                cbSelect.isChecked = selectedToDownload.contains(model.id)
                cbSelect.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) selectedToDownload.add(model.id)
                    else selectedToDownload.remove(model.id)
                }
            }

            btnSetActive.isEnabled = downloaded && !active
            btnSetActive.setOnClickListener {
                onSetActive(model)
            }

            btnRemove.isEnabled = downloaded
            btnRemove.setOnClickListener {
                onRemove(model)
            }
        }
    }
}
