package com.example.bloomie.ui.details

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bloomie.R
import com.example.bloomie.databinding.ItemIdentifyMatchBinding
import com.example.bloomie.models.IdentifyMatch

/**
 * Pl@ntNet candidates with confidence badges. One match is selected at a time;
 * the selected one is what "Save to My Garden" uses.
 */
class IdentifyMatchAdapter(
    private val matches: List<IdentifyMatch>,
    private val onSelected: (IdentifyMatch) -> Unit
) : RecyclerView.Adapter<IdentifyMatchAdapter.MatchViewHolder>() {

    var selectedIndex: Int = 0
        private set

    inner class MatchViewHolder(private val binding: ItemIdentifyMatchBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(match: IdentifyMatch, position: Int) {
            val context = binding.root.context
            val isSelected = position == selectedIndex

            binding.txtBestLabel.visibility = if (position == 0) View.VISIBLE else View.GONE
            binding.txtCommonName.text = match.displayName
            binding.txtScientificName.text = match.scientificName
            binding.txtFamily.text = context.getString(R.string.results_family, match.family)
            binding.txtFamily.visibility = if (match.family.isBlank()) View.GONE else View.VISIBLE

            binding.txtScore.text = context.getString(R.string.results_match, match.percent)
            val (background, color) = when {
                match.percent >= 70 -> R.drawable.bg_badge_healthy to R.color.status_healthy
                match.percent >= 40 -> R.drawable.bg_badge_warning to R.color.status_warning
                else -> R.drawable.bg_badge_neutral to R.color.sage_green
            }
            binding.txtScore.setBackgroundResource(background)
            binding.txtScore.setTextColor(context.getColor(color))

            binding.cardMatch.strokeColor =
                context.getColor(if (isSelected) R.color.sage_green else R.color.card_stroke)
            binding.cardMatch.strokeWidth = (context.resources.displayMetrics.density * if (isSelected) 2 else 1).toInt()
            binding.cardMatch.setCardBackgroundColor(
                context.getColor(if (isSelected) R.color.badge_mint else R.color.surface_white)
            )

            binding.cardMatch.setOnClickListener {
                val adapterPosition = bindingAdapterPosition
                if (adapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                val previous = selectedIndex
                selectedIndex = adapterPosition
                notifyItemChanged(previous)
                notifyItemChanged(selectedIndex)
                onSelected(matches[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
        val binding = ItemIdentifyMatchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MatchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
        holder.bind(matches[position], position)
    }

    override fun getItemCount(): Int = matches.size

    /** The currently selected match, or null when there are no matches. */
    fun selectedMatch(): IdentifyMatch? = matches.getOrNull(selectedIndex)
}
