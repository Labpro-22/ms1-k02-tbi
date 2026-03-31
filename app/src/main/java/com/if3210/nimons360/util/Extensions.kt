package com.if3210.nimons360.util

import android.view.View
import java.util.Locale

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

fun String.toInitials(maxChars: Int = 2): String {
    val safeMaxChars = maxChars.coerceAtLeast(1)
    val words = trim().split(Regex("\\s+")).filter { it.isNotBlank() }

    if (words.isEmpty()) {
        return ""
    }

    val initials = if (words.size == 1) {
        words.first().take(safeMaxChars)
    } else {
        "${words.first().first()}${words.last().first()}"
    }

    return initials.uppercase(Locale.getDefault()).take(safeMaxChars)
}
