package com.example.bloomie.ui.main

import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.example.bloomie.R
import java.io.File

/**
 * Loads a plant photo from the internal file path / URI string stored in SQLite.
 * Falls back to the leaf icon when there is no photo or it cannot be loaded.
 */
object PlantImageLoader {

    fun load(imageView: ImageView, imageUri: String) {
        if (imageUri.isBlank()) {
            Glide.with(imageView).clear(imageView)
            showPlaceholder(imageView)
            return
        }

        imageView.setPadding(0, 0, 0, 0)
        imageView.imageTintList = null
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP

        val model: Any = if (hasScheme(imageUri)) Uri.parse(imageUri) else File(imageUri)

        Glide.with(imageView)
            .load(model)
            .placeholder(R.drawable.ic_leaf)
            .error(R.drawable.ic_leaf)
            .centerCrop()
            .into(imageView)
    }

    private fun hasScheme(value: String): Boolean {
        return value.startsWith("file://") ||
            value.startsWith("content://") ||
            value.startsWith("http://") ||
            value.startsWith("https://")
    }

    private fun showPlaceholder(imageView: ImageView) {
        val padding = (imageView.resources.displayMetrics.density * PLACEHOLDER_PADDING_DP).toInt()
        imageView.setPadding(padding, padding, padding, padding)
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        imageView.setImageResource(R.drawable.ic_leaf)
        imageView.imageTintList = imageView.context.getColorStateList(R.color.sage_green)
    }

    private const val PLACEHOLDER_PADDING_DP = 14
}
