package com.example.bloomie.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.bloomie.data.db.GuideRepository
import com.example.bloomie.databinding.DialogGuideDetailBinding
import com.example.bloomie.databinding.ItemBulletLineBinding
import com.example.bloomie.models.PlantGuide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/** Full care instructions for one Care Library plant, with a shortcut to add it to the garden. */
class GuideDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogGuideDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogGuideDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val guideId = arguments?.getString(ARG_GUIDE_ID).orEmpty()
        val guide = GuideRepository.findById(requireContext(), guideId)
        if (guide == null) {
            dismiss()
            return
        }
        bindGuide(guide)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun bindGuide(guide: PlantGuide) {
        binding.imgGuide.setImageResource(
            GuideRepository.imageResFor(requireContext(), guide)
        )
        binding.txtCommonName.text = guide.commonName
        binding.txtScientificName.text = guide.scientificName
        binding.txtCategory.text = guide.category
        PlantGuideAdapter.styleDifficulty(binding.txtDifficulty, guide.difficulty)

        binding.txtDescription.text = guide.description
        binding.txtWatering.text = guide.watering
        binding.txtSunlight.text = guide.sunlight
        binding.txtHumidity.text = guide.humidity
        binding.txtTemperature.text = guide.temperature
        binding.txtSoil.text = guide.soil
        binding.txtToxicity.text = guide.toxicity

        binding.tipsContainer.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        for (tip in guide.careTips) {
            val row = ItemBulletLineBinding.inflate(inflater, binding.tipsContainer, false)
            row.txtLine.text = tip
            binding.tipsContainer.addView(row.root)
        }

        binding.btnAddToGarden.setOnClickListener {
            val context = requireContext()
            ScreenLauncher.openAddEditPlantPrefilled(
                context = context,
                name = guide.commonName,
                species = guide.scientificName,
                imagePath = "",
                sunlight = GuideRepository.sunlightOptionFor(context, guide.sunlight),
                wateringDays = GuideRepository.wateringDaysFor(guide)
            )
            dismiss()
        }
    }

    companion object {
        const val TAG = "GuideDetailBottomSheet"
        private const val ARG_GUIDE_ID = "guide_id"

        fun newInstance(guideId: String): GuideDetailBottomSheet {
            return GuideDetailBottomSheet().apply {
                arguments = Bundle().apply { putString(ARG_GUIDE_ID, guideId) }
            }
        }
    }
}