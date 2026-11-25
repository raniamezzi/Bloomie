package com.example.bloomie.ui.main

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.bloomie.R
import com.example.bloomie.data.db.DatabaseHelper
import com.example.bloomie.databinding.FragmentGardenBinding
import com.example.bloomie.models.HealthStatus
import com.example.bloomie.models.Plant
import java.util.Locale

/**
 * Screen 05, My Garden: search, filter chips, plant count and a two-column grid of plants.
 */
class GardenFragment : Fragment() {

    private enum class Filter { ALL, NEEDS_WATER, HEALTHY }

    private var _binding: FragmentGardenBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: DatabaseHelper
    private lateinit var adapter: PlantCardAdapter

    private var allPlants: List<Plant> = emptyList()
    private var filter: Filter = Filter.ALL
    private var query: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGardenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()
        db = DatabaseHelper(context)

        adapter = PlantCardAdapter { plant ->
            ScreenLauncher.openPlantDetails(requireContext(), plant.id)
        }
        binding.recyclerPlants.layoutManager = GridLayoutManager(context, GRID_COLUMNS)
        binding.recyclerPlants.adapter = adapter

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                query = s?.toString().orEmpty()
                applyFilters()
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })

        binding.chipAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) selectFilter(Filter.ALL)
        }
        binding.chipNeedsWater.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) selectFilter(Filter.NEEDS_WATER)
        }
        binding.chipHealthy.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) selectFilter(Filter.HEALTHY)
        }

        binding.fabAddPlant.setOnClickListener { ScreenLauncher.openAddEditPlant(requireContext()) }
    }

    override fun onResume() {
        super.onResume()
        allPlants = db.getAllPlants()
        applyFilters()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerPlants.adapter = null
        db.close()
        _binding = null
    }

    private fun selectFilter(newFilter: Filter) {
        filter = newFilter
        applyFilters()
    }

    private fun applyFilters() {
        if (_binding == null) return

        val needle = query.trim().lowercase(Locale.getDefault())
        val filtered = allPlants
            .filter { plant -> matchesFilter(plant) }
            .filter { plant -> needle.isEmpty() || matchesQuery(plant, needle) }

        adapter.submitList(filtered)

        binding.txtPlantCount.text =
            resources.getQuantityString(R.plurals.plants_count, filtered.size, filtered.size)

        val isEmpty = filtered.isEmpty()
        binding.recyclerPlants.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE

        if (isEmpty) {
            if (allPlants.isEmpty()) {
                binding.txtEmptyTitle.setText(R.string.garden_empty_title)
                binding.txtEmptyMessage.setText(R.string.garden_empty_message)
                binding.txtEmptyMessage.visibility = View.VISIBLE
            } else {
                binding.txtEmptyTitle.setText(R.string.garden_no_results)
                binding.txtEmptyMessage.visibility = View.GONE
            }
        }
    }

    private fun matchesFilter(plant: Plant): Boolean {
        return when (filter) {
            Filter.ALL -> true
            Filter.NEEDS_WATER ->
                plant.healthStatus == HealthStatus.NEEDS_WATER || plant.healthStatus == HealthStatus.OVERDUE
            Filter.HEALTHY -> plant.healthStatus == HealthStatus.HEALTHY
        }
    }

    private fun matchesQuery(plant: Plant, needle: String): Boolean {
        return plant.name.lowercase(Locale.getDefault()).contains(needle) ||
            plant.nickname.lowercase(Locale.getDefault()).contains(needle) ||
            plant.species.lowercase(Locale.getDefault()).contains(needle) ||
            plant.room.lowercase(Locale.getDefault()).contains(needle)
    }

    private companion object {
        const val GRID_COLUMNS = 2
    }
}
