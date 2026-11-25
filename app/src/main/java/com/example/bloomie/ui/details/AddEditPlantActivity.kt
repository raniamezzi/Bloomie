package com.example.bloomie.ui.details

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.bloomie.R
import com.example.bloomie.data.db.DatabaseHelper
import com.example.bloomie.data.db.ImageStorage
import com.example.bloomie.databinding.ActivityAddEditPlantBinding
import com.example.bloomie.models.Plant
import com.example.bloomie.ui.main.PlantImageLoader
import com.example.bloomie.ui.main.ScreenLauncher
import com.google.android.material.chip.Chip
import java.io.File

/**
 * Screen 07: add a plant, or edit one when [ScreenLauncher.EXTRA_PLANT_ID] is passed.
 * The form can also be pre-filled from the Identify results or the Care Library through the
 * `ScreenLauncher.EXTRA_PREFILL_*` extras.
 */
class AddEditPlantActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditPlantBinding
    private lateinit var db: DatabaseHelper

    private var editingPlant: Plant? = null

    /** Photo currently shown in the form (may be empty). */
    private var imagePath: String = ""

    /** Photo the form started with, used to clean up the old file after a replacement is saved. */
    private var originalImagePath: String = ""

    private var pendingCameraFile: File? = null
    private var selectedRoom: String = ""
    private var wasSaved: Boolean = false

    private val sunlightOptions: List<String> by lazy {
        listOf(
            getString(R.string.sunlight_low),
            getString(R.string.sunlight_indirect),
            getString(R.string.sunlight_partial),
            getString(R.string.sunlight_full)
        )
    }

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) handlePickedImage(uri)
        }

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val file = pendingCameraFile
            pendingCameraFile = null
            if (file == null) return@registerForActivityResult
            if (success && file.exists() && file.length() > 0L) {
                replacePhoto(file.absolutePath)
            } else {
                file.delete()
            }
        }

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                launchCamera()
            } else {
                Toast.makeText(this, R.string.camera_permission_needed, Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditPlantBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = DatabaseHelper(this)

        val plantId = intent.getLongExtra(ScreenLauncher.EXTRA_PLANT_ID, -1L)
        if (plantId > 0) {
            val plant = db.getPlantById(plantId)
            if (plant == null) {
                Toast.makeText(this, R.string.details_not_found, Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            editingPlant = plant
        }

        setupSunlightDropdown()

        if (savedInstanceState == null) {
            populateInitialValues()
        } else {
            imagePath = savedInstanceState.getString(STATE_IMAGE_PATH).orEmpty()
            originalImagePath = savedInstanceState.getString(STATE_ORIGINAL_IMAGE_PATH).orEmpty()
            selectedRoom = savedInstanceState.getString(STATE_ROOM).orEmpty()
            pendingCameraFile = savedInstanceState.getString(STATE_PENDING_CAMERA)?.let { File(it) }
        }

        buildRoomChips()
        showPhoto()

        val isEditing = editingPlant != null
        binding.txtTitle.setText(if (isEditing) R.string.edit_plant_title else R.string.add_plant_title)
        binding.rowWatered.visibility = if (isEditing) android.view.View.GONE else android.view.View.VISIBLE

        binding.btnBack.setOnClickListener { finish() }
        binding.btnChoosePhoto.setOnClickListener { pickImage.launch("image/*") }
        binding.btnTakePhoto.setOnClickListener { onTakePhotoClicked() }
        binding.btnSave.setOnClickListener { save() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_IMAGE_PATH, imagePath)
        outState.putString(STATE_ORIGINAL_IMAGE_PATH, originalImagePath)
        outState.putString(STATE_ROOM, selectedRoom)
        outState.putString(STATE_PENDING_CAMERA, pendingCameraFile?.absolutePath)
    }

    override fun onDestroy() {
        // Leaving without saving: drop a photo that was only chosen during this visit.
        if (isFinishing && !wasSaved && imagePath.isNotEmpty() && imagePath != originalImagePath) {
            ImageStorage.deleteIfPermanent(this, imagePath)
        }
        db.close()
        super.onDestroy()
    }

    // ---------------------------------------------------------------------------------------
    // Initial values
    // ---------------------------------------------------------------------------------------

    private fun populateInitialValues() {
        val plant = editingPlant
        if (plant != null) {
            binding.etName.setText(plant.name)
            binding.etSpecies.setText(plant.species)
            binding.etNickname.setText(plant.nickname)
            binding.etFrequency.setText(plant.wateringFrequencyDays.toString())
            binding.dropSunlight.setText(plant.sunlight, false)
            binding.etNotes.setText(plant.notes)
            selectedRoom = plant.room
            imagePath = plant.imageUri
            originalImagePath = plant.imageUri
            return
        }

        // Add mode: values may come from the Identify results or the Care Library.
        binding.etName.setText(intent.getStringExtra(ScreenLauncher.EXTRA_PREFILL_NAME).orEmpty())
        binding.etSpecies.setText(intent.getStringExtra(ScreenLauncher.EXTRA_PREFILL_SPECIES).orEmpty())
        binding.etFrequency.setText(
            intent.getIntExtra(ScreenLauncher.EXTRA_PREFILL_WATERING_DAYS, DEFAULT_FREQUENCY_DAYS).toString()
        )
        val sunlight = intent.getStringExtra(ScreenLauncher.EXTRA_PREFILL_SUNLIGHT).orEmpty()
        binding.dropSunlight.setText(sunlight.ifBlank { sunlightOptions[1] }, false)

        imagePath = intent.getStringExtra(ScreenLauncher.EXTRA_PREFILL_IMAGE_PATH).orEmpty()
        originalImagePath = imagePath
    }

    private fun setupSunlightDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, sunlightOptions)
        binding.dropSunlight.setAdapter(adapter)
    }

    private fun buildRoomChips() {
        val rooms = mutableListOf(
            getString(R.string.room_living_room),
            getString(R.string.room_bedroom),
            getString(R.string.room_kitchen),
            getString(R.string.room_bathroom),
            getString(R.string.room_office),
            getString(R.string.room_balcony)
        )
        if (selectedRoom.isNotBlank() && rooms.none { it.equals(selectedRoom, ignoreCase = true) }) {
            rooms.add(selectedRoom)
        }

        binding.chipGroupRoom.removeAllViews()
        for (room in rooms) {
            val chip = layoutInflater.inflate(R.layout.item_room_chip, binding.chipGroupRoom, false) as Chip
            chip.text = room
            chip.isChecked = room.equals(selectedRoom, ignoreCase = true)
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedRoom = room
                } else if (selectedRoom == room) {
                    selectedRoom = ""
                }
            }
            binding.chipGroupRoom.addView(chip)
        }
    }

    // ---------------------------------------------------------------------------------------
    // Photo handling
    // ---------------------------------------------------------------------------------------

    private fun showPhoto() {
        PlantImageLoader.load(binding.imgPhoto, imagePath)
    }

    private fun handlePickedImage(uri: Uri) {
        val target = ImageStorage.newPlantImageFile(this, ImageStorage.extensionFor(this, uri))
        if (ImageStorage.copyUriTo(this, uri, target)) {
            replacePhoto(target.absolutePath)
        } else {
            Toast.makeText(this, R.string.photo_save_error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun onTakePhotoClicked() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        val file = ImageStorage.newPlantImageFile(this)
        pendingCameraFile = file
        takePicture.launch(ImageStorage.uriFor(this, file))
    }

    /** Shows a newly chosen photo and removes the previous one if it was only chosen in this session. */
    private fun replacePhoto(newPath: String) {
        if (imagePath.isNotEmpty() && imagePath != originalImagePath && imagePath != newPath) {
            ImageStorage.deleteIfPermanent(this, imagePath)
        }
        imagePath = newPath
        showPhoto()
    }

    // ---------------------------------------------------------------------------------------
    // Saving
    // ---------------------------------------------------------------------------------------

    private fun save() {
        val name = binding.etName.text?.toString().orEmpty().trim()
        val frequency = binding.etFrequency.text?.toString().orEmpty().trim().toIntOrNull()

        var valid = true
        if (name.isEmpty()) {
            binding.tilName.error = getString(R.string.error_plant_name_required)
            valid = false
        } else {
            binding.tilName.error = null
        }
        if (frequency == null || frequency !in MIN_FREQUENCY_DAYS..MAX_FREQUENCY_DAYS) {
            binding.tilFrequency.error = getString(R.string.error_frequency_invalid)
            valid = false
        } else {
            binding.tilFrequency.error = null
        }
        if (!valid || frequency == null) return

        val species = binding.etSpecies.text?.toString().orEmpty().trim()
        val nickname = binding.etNickname.text?.toString().orEmpty().trim()
        val sunlight = binding.dropSunlight.text?.toString().orEmpty().trim()
        val notes = binding.etNotes.text?.toString().orEmpty().trim()

        // Photos taken on the Identify screen live in the cache, so move them to permanent storage.
        val finalImagePath = ImageStorage.persist(this, imagePath)

        val existing = editingPlant
        val saved = if (existing == null) {
            insertNewPlant(name, species, nickname, frequency, sunlight, notes, finalImagePath)
        } else {
            db.updatePlant(
                existing.copy(
                    name = name,
                    species = species,
                    nickname = nickname,
                    room = selectedRoom,
                    wateringFrequencyDays = frequency,
                    sunlight = sunlight,
                    notes = notes,
                    imageUri = finalImagePath
                )
            )
        }

        if (!saved) {
            Toast.makeText(this, R.string.error_generic, Toast.LENGTH_SHORT).show()
            return
        }

        if (originalImagePath.isNotEmpty() && originalImagePath != finalImagePath) {
            ImageStorage.deleteIfPermanent(this, originalImagePath)
        }
        wasSaved = true
        Toast.makeText(this, R.string.plant_saved, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun insertNewPlant(
        name: String,
        species: String,
        nickname: String,
        frequency: Int,
        sunlight: String,
        notes: String,
        imagePath: String
    ): Boolean {
        val plant = Plant(
            name = name,
            species = species,
            nickname = nickname,
            room = selectedRoom,
            wateringFrequencyDays = frequency,
            sunlight = sunlight,
            notes = notes,
            imageUri = imagePath
        )
        val newId = db.insertPlant(plant)
        if (newId == -1L) return false

        // Without a last-watered date the plant is scheduled for today, which is what the switch off means.
        if (binding.switchWatered.isChecked) {
            db.markAsWatered(newId)
        }
        return true
    }

    private companion object {
        const val DEFAULT_FREQUENCY_DAYS = 7
        const val MIN_FREQUENCY_DAYS = 1
        const val MAX_FREQUENCY_DAYS = 90

        const val STATE_IMAGE_PATH = "state_image_path"
        const val STATE_ORIGINAL_IMAGE_PATH = "state_original_image_path"
        const val STATE_ROOM = "state_room"
        const val STATE_PENDING_CAMERA = "state_pending_camera"
    }
}
