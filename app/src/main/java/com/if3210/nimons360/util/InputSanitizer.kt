package com.if3210.nimons360.util

private val CONTROL_CHARACTER_REGEX = Regex("[\\p{Cntrl}]")
private val MULTIPLE_WHITESPACE_REGEX = Regex("\\s+")

fun String.normalizeSingleLineInput(): String {
    return trim()
        .replace(CONTROL_CHARACTER_REGEX, "")
        .replace(MULTIPLE_WHITESPACE_REGEX, " ")
}
