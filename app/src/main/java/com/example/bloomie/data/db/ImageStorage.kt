package com.example.bloomie.data.db

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * Keeps plant photos in the app's private storage so saved paths never expire.
 *
 * - Photos for saved plants live in `filesDir/plant_images` (permanent).
 * - Scans taken on the Identify screen live in `cacheDir/scans` (temporary) and are copied
 *   into permanent storage with [persist] only when the user saves the plant.
 */
object ImageStorage {

    private const val PLANT_DIR = "plant_images"
    private const val SCAN_DIR = "scans"

    fun newPlantImageFile(context: Context, extension: String = "jpg"): File {
        return File(ensureDir(File(context.filesDir, PLANT_DIR)), uniqueName(extension))
    }

    fun newScanFile(context: Context, extension: String = "jpg"): File {
        return File(ensureDir(File(context.cacheDir, SCAN_DIR)), uniqueName(extension))
    }

    /** Content URI for [file], suitable for handing to the camera app. */
    fun uriFor(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, authority(context), file)
    }

    /** Copies the content behind [uri] into [target]. Returns false if anything goes wrong. */
    fun copyUriTo(context: Context, uri: Uri, target: File): Boolean {
        return try {
            val input = context.contentResolver.openInputStream(uri)
            if (input == null) {
                false
            } else {
                input.use { source -> target.outputStream().use { sink -> source.copyTo(sink) } }
                true
            }
        } catch (e: IOException) {
            target.delete()
            false
        } catch (e: SecurityException) {
            target.delete()
            false
        }
    }

    /** File extension that matches the picked image's MIME type. */
    fun extensionFor(context: Context, uri: Uri): String {
        return when (context.contentResolver.getType(uri)) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
    }

    /** True if [path] is already inside permanent plant photo storage. */
    fun isPermanent(context: Context, path: String): Boolean {
        if (path.isBlank()) return false
        val dir = File(context.filesDir, PLANT_DIR).absolutePath
        return File(path).absolutePath.startsWith(dir)
    }

    /**
     * Makes sure the photo at [path] is stored permanently. Photos already in permanent storage
     * are returned unchanged; others are copied in. If copying fails the original path is returned.
     */
    fun persist(context: Context, path: String): String {
        if (path.isBlank() || isPermanent(context, path)) return path
        val source = File(path)
        if (!source.exists()) return path

        val target = newPlantImageFile(context, source.extension.ifBlank { "jpg" })
        return try {
            source.inputStream().use { input -> target.outputStream().use { output -> input.copyTo(output) } }
            target.absolutePath
        } catch (e: IOException) {
            target.delete()
            path
        }
    }

    /** Deletes the photo at [path] only if it is in permanent plant photo storage. */
    fun deleteIfPermanent(context: Context, path: String) {
        if (isPermanent(context, path)) File(path).delete()
    }

    private fun authority(context: Context): String = context.packageName + ".fileprovider"

    private fun ensureDir(dir: File): File {
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun uniqueName(extension: String): String {
        return "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.$extension"
    }
}
