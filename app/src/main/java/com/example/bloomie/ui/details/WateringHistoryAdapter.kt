package com.example.bloomie.ui.details

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bloomie.databinding.ItemWateringHistoryBinding
import com.example.bloomie.models.WateringLog
import com.example.bloomie.ui.main.DateFormatter

/** Lists the watering events of one plant, newest first. */
class WateringHistoryAdapter :
    ListAdapter<WateringLog, WateringHistoryAdapter.HistoryViewHolder>(Diff) {

    class HistoryViewHolder(private val binding: ItemWateringHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(log: WateringLog) {
            binding.txtDate.text = DateFormatter.formatDateTime(log.wateredTimestamp)
            binding.txtNotes.text = log.notes
            binding.txtNotes.visibility = if (log.notes.isBlank()) View.GONE else View.VISIBLE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemWateringHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private object Diff : DiffUtil.ItemCallback<WateringLog>() {
        override fun areItemsTheSame(oldItem: WateringLog, newItem: WateringLog): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: WateringLog, newItem: WateringLog): Boolean =
            oldItem == newItem
    }
}
