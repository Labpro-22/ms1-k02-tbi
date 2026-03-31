package com.if3210.nimons360.model

enum class InternetStatus(val apiValue: String) {
    WIFI("wifi"),
    MOBILE("mobile");

    companion object {
        fun fromApiValue(value: String): InternetStatus? {
            return entries.firstOrNull { status ->
                status.apiValue.equals(value, ignoreCase = true)
            }
        }
    }
}
