package com.example.bloomie.ui.assistant

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bloomie.R
import com.example.bloomie.databinding.ItemScheduleGroupBinding
import com.example.bloomie.databinding.ItemScheduleRowBinding
import com.example.bloomie.models.Plant
import com.example.bloomie.ui.main.PlantImageLoader
import com.example.bloomie.ui.main.PlantUiHelper

/** The timeline sections of the watering schedule. */
enum class ScheduleKind { OVERDUE, TODAY, WEEK, LATER }

/** One timeline section and the plants in it. */
data class ScheduleGroup(val kind: ScheduleKind, val plants: List<Plant>)

/**
 * Timeline sections ("Overdue", "Due Today", "Upcoming This Week", "Later"), each listing its
 * plants with a one-tap Water button.
 */
class ScheduleGroupAdapter(
    private val onWater: (Plant) -> Unit,
    private val onOpen: (Plant) -> Unit
) : ListAdapter<ScheduleGroup, ScheduleGroupAdapter.GroupViewHolder>(Diff) {

    inner class GroupViewHolder(private val binding: ItemScheduleGroupBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(group: ScheduleGroup) {
            val context = binding.root.context

            binding.txtGroupTitle.setText(titleFor(group.kind))
            binding.txtGroupCount.text = group.plants.size.toString()
            val (background, color) = when (group.kind) {
                ScheduleKind.OVERDUE -> R.drawable.bg_badge_overdue to R.color.status_overdue
                ScheduleKind.TODAY -> R.drawable.bg_badge_warning to R.color.status_warning
                else -> R.drawable.bg_badge_neutral to R.color.sage_green
            }
            binding.txtGroupCount.setBackgroundResource(background)
            binding.txtGroupCount.setTextColor(context.getColor(color))

            binding.groupContainer.removeAllViews()
            val inflater = LayoutInflater.from(context)
            group.plants.forEachIndexed { index, plant ->
                val row = ItemScheduleRowBinding.inflate(inflater, binding.groupContainer, false)
                row.txtName.text = plant.displayName
                row.txtRoom.text = PlantUiHelper.roomLabel(context, plant)
                PlantUiHelper.styleCountdownPill(row.txtCountdown, plant)
                PlantImageLoader.load(row.imgPlant, plant.imageUri)
                row.divider.visibility = if (index == group.plants.lastIndex) View.GONE else View.VISIBLE
                row.btnWater.setOnClickListener { onWater(plant) }
                row.rowRoot.setOnClickListener { onOpen(plant) }
                binding.groupContainer.addView(row.root)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ItemScheduleGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private fun titleFor(kind: ScheduleKind): Int = when (kind) {
        ScheduleKind.OVERDUE -> R.string.schedule_overdue
        ScheduleKind.TODAY -> R.string.schedule_due_today
        ScheduleKind.WEEK -> R.string.schedule_upcoming_week
        ScheduleKind.LATER -> R.string.schedule_later
    }

    private object Diff : DiffUtil.ItemCallback<ScheduleGroup>() {
        override fun areItemsTheSame(oldItem: ScheduleGroup, newItem: ScheduleGroup): Boolean =
            oldItem.kind == newItem.kind

        override fun areContentsTheSame(oldItem: ScheduleGroup, newItem: ScheduleGroup): Boolean =
            oldItem == newItem
    }
}
