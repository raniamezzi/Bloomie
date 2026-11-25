package com.example.bloomie.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bloomie.R
import com.example.bloomie.data.db.GuideRepository
import com.example.bloomie.databinding.ItemPlantGuideBinding
import com.example.bloomie.models.PlantGuide

/** Care Library cards. */
class PlantGuideAdapter(
    private val onOpen: (PlantGuide) -> Unit
) : ListAdapter<PlantGuide, PlantGuideAdapter.GuideViewHolder>(Diff) {

    inner class GuideViewHolder(private val binding: ItemPlantGuideBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(guide: PlantGuide) {
            binding.txtCommonName.text = guide.commonName
            binding.txtScientificName.text = guide.scientificName
            binding.txtCategory.text = guide.category
            binding.imgGuide.setImageResource(
                GuideRepository.imageResFor(binding.root.context, guide)
            )
            styleDifficulty(binding.txtDifficulty, guide.difficulty)
            binding.cardGuide.setOnClickListener { onOpen(guide) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuideViewHolder {
        val binding = ItemPlantGuideBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GuideViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GuideViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private object Diff : DiffUtil.ItemCallback<PlantGuide>() {
        override fun areItemsTheSame(oldItem: PlantGuide, newItem: PlantGuide): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: PlantGuide, newItem: PlantGuide): Boolean =
            oldItem == newItem
    }

    companion object {
        /** Colors the difficulty pill: green for easy, amber for moderate, red for advanced. */
        fun styleDifficulty(textView: TextView, difficulty: String) {
            textView.text = difficulty
            val (background, color) = when (difficulty.lowercase()) {
                "easy" -> R.drawable.bg_badge_healthy to R.color.status_healthy
                "advanced" -> R.drawable.bg_badge_overdue to R.color.status_overdue
                else -> R.drawable.bg_badge_warning to R.color.status_warning
            }
            textView.setBackgroundResource(background)
            textView.setTextColor(textView.context.getColor(color))
        }
    }
}