package com.example.bloomie.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.bloomie.R
import com.example.bloomie.data.db.ImageStorage
import com.example.bloomie.databinding.FragmentIdentifyBinding
import com.example.bloomie.models.PlantNetResponse
import com.example.bloomie.network.PlantIdentifier
import com.example.bloomie.ui.details.IdentifyResultsActivity
import retrofit2.Call
import java.io.File

/**
 * Screen 09: take or pick a photo, preview it and identify the plant with Pl@ntNet.
 * Scans are kept in the cache until the user saves a plant from the results screen.
 */
class IdentifyFragment : Fragment() {

    private var _binding: FragmentIdentifyBinding? = null
    private val binding get() = _binding!!

    private var selectedPath: String = ""
    private var pendingCameraFile: File? = null
    private var inFlightCall: Call<PlantNetResponse>? = null
    private var isLoading = false

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
                selectedPath = file.absolutePath
                showPreview()
            } else {
                file.delete()
            }
        }

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                launchCamera()
            } else {
                Toast.makeText(requireContext(), R.string.camera_permission_needed, Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            selectedPath = savedInstanceState.getString(STATE_SELECTED_PATH).orEmpty()
            pendingCameraFile = savedInstanceState.getString(STATE_PENDING_CAMERA)?.let { File(it) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIdentifyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCamera.setOnClickListener { onCameraClicked() }
        binding.btnGallery.setOnClickListener { pickImage.launch("image/*") }
        binding.btnIdentify.setOnClickListener { identify() }

        showPreview()
        setLoading(false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_SELECTED_PATH, selectedPath)
        outState.putString(STATE_PENDING_CAMERA, pendingCameraFile?.absolutePath)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        inFlightCall?.cancel()
        inFlightCall = null
        _binding = null
    }

    // ---------------------------------------------------------------------------------------
    // Photo selection
    // ---------------------------------------------------------------------------------------

    private fun onCameraClicked() {
        val context = requireContext()
        if (context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        val context = requireContext()
        val file = ImageStorage.newScanFile(context)
        pendingCameraFile = file
        takePicture.launch(ImageStorage.uriFor(context, file))
    }

    private fun handlePickedImage(uri: Uri) {
        val context = requireContext()
        val target = ImageStorage.newScanFile(context, ImageStorage.extensionFor(context, uri))
        if (ImageStorage.copyUriTo(context, uri, target)) {
            selectedPath = target.absolutePath
            showPreview()
        } else {
            Toast.makeText(context, R.string.photo_save_error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPreview() {
        if (_binding == null) return
        val hasImage = selectedPath.isNotBlank() && File(selectedPath).exists()
        binding.imgPreview.visibility = if (hasImage) View.VISIBLE else View.GONE
        binding.layoutPlaceholder.visibility = if (hasImage) View.GONE else View.VISIBLE
        if (hasImage) {
            Glide.with(this).load(File(selectedPath)).centerCrop().into(binding.imgPreview)
        }
    }

    // ---------------------------------------------------------------------------------------
    // Identification
    // ---------------------------------------------------------------------------------------

    private fun identify() {
        if (isLoading) return
        val file = File(selectedPath)
        if (selectedPath.isBlank() || !file.exists()) {
            Toast.makeText(requireContext(), R.string.identify_pick_first, Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)
        val scannedPath = selectedPath
        val appContext = requireContext().applicationContext
        inFlightCall = PlantIdentifier.identify(appContext, file) { result ->
            // The screen may have been closed while the request was running.
            if (_binding != null) {
                inFlightCall = null
                setLoading(false)
                IdentifyResultsActivity.start(requireContext(), scannedPath, result.matches, result.source)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        isLoading = loading
        if (_binding == null) return
        binding.progressIdentify.visibility = if (loading) View.VISIBLE else View.GONE
        binding.txtLoading.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnCamera.isEnabled = !loading
        binding.btnGallery.isEnabled = !loading
        binding.btnIdentify.isEnabled = !loading
    }

    private companion object {
        const val STATE_SELECTED_PATH = "state_selected_path"
        const val STATE_PENDING_CAMERA = "state_pending_camera"
    }
}
