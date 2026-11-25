package com.example.bloomie.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bloomie.databinding.ItemTaskTodayBinding
import com.example.bloomie.models.Plant

/** Diff callback shared by the plant list adapters. */
internal object PlantDiffCallback : DiffUtil.ItemCallback<Plant>() {
    override fun areItemsTheSame(oldItem: Plant, newItem: Plant): Boolean = oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Plant, newItem: Plant): Boolean = oldItem == newItem
}

/**
 * "Today's Tasks" rows on the dashboard.
 * [onWater] is called when the Water button is tapped, [onOpen] when the row is tapped.
 */
class TaskAdapter(
    private val onWater: (Plant) -> Unit,
    private val onOpen: (Plant) -> Unit
) : ListAdapter<Plant, TaskAdapter.TaskViewHolder>(PlantDiffCallback) {

    inner class TaskViewHolder(private val binding: ItemTaskTodayBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(plant: Plant) {
            val context = binding.root.context
            binding.txtName.text = plant.displayName
            binding.txtRoom.text = PlantUiHelper.roomLabel(context, plant)
            PlantUiHelper.styleCountdownPill(binding.txtBadge, plant)
            PlantImageLoader.load(binding.imgPlant, plant.imageUri)

            binding.btnWater.setOnClickListener { onWater(plant) }
            binding.cardTask.setOnClickListener { onOpen(plant) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskTodayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
