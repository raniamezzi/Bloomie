package com.example.bloomie.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bloomie.databinding.ItemPlantCardBinding
import com.example.bloomie.models.Plant

/** Two-column plant grid cards for the My Garden screen. */
class PlantCardAdapter(
    private val onOpen: (Plant) -> Unit
) : ListAdapter<Plant, PlantCardAdapter.PlantViewHolder>(PlantDiffCallback) {

    inner class PlantViewHolder(private val binding: ItemPlantCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(plant: Plant) {
            val context = binding.root.context
            binding.txtName.text = plant.displayName
            binding.txtSpecies.text = plant.species
            binding.txtRoom.text = PlantUiHelper.roomLabel(context, plant)
            PlantUiHelper.styleCountdownPill(binding.txtCountdown, plant)
            PlantUiHelper.styleStatusBadge(binding.txtHealth, plant)
            PlantImageLoader.load(binding.imgPlant, plant.imageUri)

            binding.cardPlant.setOnClickListener { onOpen(plant) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val binding = ItemPlantCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
