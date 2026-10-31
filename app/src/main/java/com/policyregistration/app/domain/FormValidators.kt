package com.policyregistration.app.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object FormValidators {

    /**
     * Preserves internal and trailing spaces so the operator can type freely.
     * Capitalizes each individual word without removing spacing or collapsing characters.
     */
    fun toTitleCase(input: String): String {
        if (input.isEmpty()) return ""
        val result = StringBuilder()
        var capitalizeNext = true
        for (char in input) {
            if (char.isWhitespace()) {
                result.append(char)
                capitalizeNext = true
            } else {
                if (capitalizeNext) {
                    result.append(char.uppercaseChar())
                    capitalizeNext = false
                } else {
                    result.append(char)
                }
            }
        }
        return result.toString()
    }

    fun isValidAadhaar(aadhaar: String): Boolean {
        val digits = aadhaar.filter { it.isDigit() }
        return digits.length == 12
    }

    fun isValidPhone(phone: String): Boolean {
        val digits = phone.filter { it.isDigit() }
        return digits.length == 10
    }

    /**
     * Parses multiple Indian date formats (DD.MM.YYYY, DD/MM/YYYY, DD-MM-YYYY, DD.MM.YY)
     * and normalizes to DD.MM.YYYY
     */
    fun normalizeDate(input: String): String? {
        val cleaned = input.trim()
        val patterns = listOf(
            "dd.MM.yyyy",
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "dd.MM.yy",
            "dd/MM/yy",
            "dd-MM-yy",
            "yyyy-MM-dd"
        )
        for (p in patterns) {
            try {
                val sdf = SimpleDateFormat(p, Locale.US).apply { isLenient = false }
                val date = sdf.parse(cleaned)
                if (date != null) {
                    val outSdf = SimpleDateFormat("dd.MM.yyyy", Locale.US)
                    return outSdf.format(date)
                }
            } catch (_: Exception) {
            }
        }
        return null
    }

    fun calculateAge(dobNormalized: String): Int? {
        return try {
            val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.US).apply { isLenient = false }
            val birthDate = sdf.parse(dobNormalized) ?: return null
            val birthCal = Calendar.getInstance().apply { time = birthDate }
            val nowCal = Calendar.getInstance()

            var age = nowCal.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)
            if (nowCal.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) {
                age--
            }
            if (age in 0..125) age else null
        } catch (e: Exception) {
            null
        }
    }
}
