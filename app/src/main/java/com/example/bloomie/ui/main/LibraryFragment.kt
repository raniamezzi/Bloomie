package com.example.bloomie.ui.main

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bloomie.R
import com.example.bloomie.data.db.GuideRepository
import com.example.bloomie.databinding.FragmentLibraryBinding
import com.example.bloomie.models.PlantGuide
import java.util.Locale

/**
 * Screen 08, Care Library: plant guides from `assets/plants.json` with search and filter chips.
 * Tapping a card opens [GuideDetailBottomSheet].
 */
class LibraryFragment : Fragment() {

    private enum class Filter { ALL, EASY, MEDIUM, LOW_LIGHT, PET_FRIENDLY }

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PlantGuideAdapter
    private var guides: List<PlantGuide> = emptyList()
    private var filter: Filter = Filter.ALL
    private var query: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PlantGuideAdapter { guide ->
            if (parentFragmentManager.findFragmentByTag(GuideDetailBottomSheet.TAG) == null) {
                GuideDetailBottomSheet.newInstance(guide.id)
                    .show(parentFragmentManager, GuideDetailBottomSheet.TAG)
            }
        }
        binding.recyclerGuides.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerGuides.adapter = adapter

        guides = GuideRepository.load(requireContext())

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                query = s?.toString().orEmpty()
                applyFilters()
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })

        bindChip(binding.chipAll, Filter.ALL)
        bindChip(binding.chipEasy, Filter.EASY)
        bindChip(binding.chipMedium, Filter.MEDIUM)
        bindChip(binding.chipLowLight, Filter.LOW_LIGHT)
        bindChip(binding.chipPetFriendly, Filter.PET_FRIENDLY)

        applyFilters()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerGuides.adapter = null
        _binding = null
    }

    private fun bindChip(chip: com.google.android.material.chip.Chip, target: Filter) {
        chip.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                filter = target
                applyFilters()
            }
        }
    }

    private fun applyFilters() {
        if (_binding == null) return

        val needle = query.trim().lowercase(Locale.getDefault())
        val filtered = guides
            .filter { guide -> matchesFilter(guide) }
            .filter { guide -> needle.isEmpty() || matchesQuery(guide, needle) }

        adapter.submitList(filtered)

        val isEmpty = filtered.isEmpty()
        binding.recyclerGuides.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.txtEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.txtEmpty.setText(if (guides.isEmpty()) R.string.guide_load_error else R.string.library_empty)
    }

    private fun matchesFilter(guide: PlantGuide): Boolean {
        return when (filter) {
            Filter.ALL -> true
            Filter.EASY -> guide.difficulty.equals("Easy", ignoreCase = true)
            Filter.MEDIUM -> guide.difficulty.equals("Moderate", ignoreCase = true)
            Filter.LOW_LIGHT -> guide.sunlight.contains("low", ignoreCase = true)
            Filter.PET_FRIENDLY -> guide.toxicity.startsWith("Non-toxic", ignoreCase = true)
        }
    }

    private fun matchesQuery(guide: PlantGuide, needle: String): Boolean {
        val locale = Locale.getDefault()
        return guide.commonName.lowercase(locale).contains(needle) ||
            guide.scientificName.lowercase(locale).contains(needle) ||
            guide.category.lowercase(locale).contains(needle)
    }
}
