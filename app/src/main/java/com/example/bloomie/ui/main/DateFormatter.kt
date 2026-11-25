package com.example.bloomie.ui.main

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Small helpers for showing timestamps in the user's locale. */
object DateFormatter {

    /** For example "Nov 17, 2026". */
    fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
    }

    /** For example "Nov 17, 2026 · 9:30 AM". */
    fun formatDateTime(timestamp: Long): String {
        return SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()).format(Date(timestamp))
    }
}
