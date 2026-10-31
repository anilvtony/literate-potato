package com.policyregistration.app.domain

import android.graphics.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ExtractedRegistrationFields(
    val rawText: String,
    val aadhaarNo: String = "",
    val name: String = "",
    val address: String = "",
    val age: String = "",
    val nominee: String = "",
    val relation: String = "",
    val phoneNumber: String = "",
    val dob: String = "",
    val gender: String = "Male",
    val accidentPrevious: String = "No",
    val existingPolicy: String = "No",
    val confidenceNotes: List<String> = emptyList()
)

class OcrExtractionEngine {

    // Bundled on-device Latin text recognizer (100% offline)
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun processImage(bitmap: Bitmap): ExtractedRegistrationFields = withContext(Dispatchers.Default) {
        // Preprocess handwritten forms (grayscale + high contrast enhancement)
        val enhancedBitmap = enhanceForHandwriting(bitmap)
        val inputImage = InputImage.fromBitmap(enhancedBitmap, 0)

        suspendCancellableCoroutine { cont ->
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val result = parseHandwrittenVisionText(visionText)
                    cont.resume(result)
                }
                .addOnFailureListener { exception ->
                    cont.resumeWithException(exception)
                }
        }
    }

    /**
     * Image enhancement specifically designed for handwritten text on paper:
     * Increases contrast, sharpens pencil/pen strokes, and converts to monochrome/grayscale.
     */
    private fun enhanceForHandwriting(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val enhanced = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(enhanced)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Color matrix: Grayscale + High contrast boost
        val contrast = 1.6f
        val brightness = -20f
        val cm = ColorMatrix(
            floatArrayOf(
                contrast * 0.299f, contrast * 0.587f, contrast * 0.114f, 0f, brightness,
                contrast * 0.299f, contrast * 0.587f, contrast * 0.114f, 0f, brightness,
                contrast * 0.299f, contrast * 0.587f, contrast * 0.114f, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return enhanced
    }

    /**
     * Resolves common handwritten OCR confusion:
     * 'O'/'o' -> '0', 'I'/'l'/'|' -> '1', 'Z' -> '2', 'S'/'s' -> '5', 'B' -> '8'
     */
    private fun cleanHandwrittenDigits(raw: String): String {
        return raw
            .replace('O', '0')
            .replace('o', '0')
            .replace('I', '1')
            .replace('l', '1')
            .replace('|', '1')
            .replace('Z', '2')
            .replace('z', '2')
            .replace('S', '5')
            .replace('s', '5')
            .replace('B', '8')
            .filter { it.isDigit() }
    }

    fun parseHandwrittenVisionText(visionText: Text): ExtractedRegistrationFields {
        val fullText = visionText.text
        val lines = fullText.lines().map { it.trim() }.filter { it.isNotBlank() }

        var aadhaar = ""
        var name = ""
        var address = ""
        var age = ""
        var nominee = ""
        var relation = ""
        var phone = ""
        var dob = ""
        var gender = "Male"
        var accidentPrevious = "No"
        var existingPolicy = "No"
        val notes = mutableListOf<String>()

        // 1. EXTRACT 12-DIGIT AADHAAR FROM HANDWRITING ANYWHERE IN IMAGE
        // A. Direct check: exact 12-digit continuous or grouped (e.g., 5489 1234 5678 or 5489-1234-5678)
        val direct12 = Regex("""\b(\d{4}[\s-]?\d{4}[\s-]?\d{4})\b""").find(fullText)
        if (direct12 != null) {
            val c = direct12.value.filter { it.isDigit() }
            if (c.length == 12) aadhaar = c
        }

        // B. If not found directly, look for any handwritten tokens with common letter substitutions
        if (aadhaar.isEmpty()) {
            val tokens = fullText.split(Regex("""[\s,/:|]+"""))
            for (t in tokens) {
                val cleaned = cleanHandwrittenDigits(t)
                if (cleaned.length == 12) {
                    aadhaar = cleaned
                    break
                }
            }
        }

        // C. Combine consecutive 4-digit groups (handwritten boxes often yield 4 digits per box)
        if (aadhaar.isEmpty()) {
            val fourDigitGroups = mutableListOf<String>()
            for (token in fullText.split(Regex("""\s+"""))) {
                val digits = cleanHandwrittenDigits(token)
                if (digits.length == 4) {
                    fourDigitGroups.add(digits)
                    if (fourDigitGroups.size == 3) {
                        aadhaar = fourDigitGroups.joinToString("")
                        break
                    }
                } else if (digits.length != 4) {
                    fourDigitGroups.clear()
                }
            }
        }

        // 2. EXTRACT 10-DIGIT PHONE NUMBER ANYWHERE IN IMAGE
        val phoneDirect = Regex("""\b(\d{10})\b""").findAll(fullText).map { it.value }.toList()
        for (candidate in phoneDirect) {
            if (aadhaar.isNotEmpty() && aadhaar.contains(candidate)) continue
            phone = candidate
            break
        }

        if (phone.isEmpty()) {
            val tokens = fullText.split(Regex("""[\s,/:|]+"""))
            for (t in tokens) {
                val cleaned = cleanHandwrittenDigits(t)
                if (cleaned.length == 10 && (aadhaar.isEmpty() || !aadhaar.contains(cleaned))) {
                    phone = cleaned
                    break
                }
            }
        }

        // 3. DOB Extraction (Handwritten formats: DD.MM.YYYY, DD/MM/YYYY, DD-MM-YYYY, DD/MM/YY)
        val dateRegex = Regex("""\b(\d{1,2}[./-]\d{1,2}[./-]\d{2,4})\b""")
        val dateMatches = dateRegex.findAll(fullText).map { it.value }.toList()
        for (d in dateMatches) {
            val normalized = FormValidators.normalizeDate(d)
            if (normalized != null) {
                dob = normalized
                val calcAge = FormValidators.calculateAge(normalized)
                if (calcAge != null) {
                    age = calcAge.toString()
                }
                break
            }
        }

        // If DOB is not found, look for standalone handwritten age (e.g. "Age: 35" or "35 Yrs")
        if (age.isEmpty()) {
            val ageRegex = Regex("""\b(age|yrs|years)?\s*([1-9]\d)\s*(yrs|years)?\b""", RegexOption.IGNORE_CASE)
            val ageMatch = ageRegex.find(fullText)
            if (ageMatch != null) {
                val num = ageMatch.groupValues[2]
                val n = num.toIntOrNull()
                if (n != null && n in 18..90) {
                    age = n.toString()
                }
            }
        }

        // 4. Gender Extraction (Handwritten M / F / Male / Female)
        val lowerText = fullText.lowercase()
        if (lowerText.contains("female") || lowerText.contains("/f") || lowerText.contains(" f ") || lowerText.contains("(f)")) {
            gender = "Female"
        } else if (lowerText.contains("male") || lowerText.contains("/m") || lowerText.contains(" m ") || lowerText.contains("(m)")) {
            gender = "Male"
        }

        // 5. Relations
        val relations = listOf("Wife", "Mother", "Father", "Husband", "Son", "Daughter", "Brother", "Sister")
        for (r in relations) {
            if (fullText.contains(r, ignoreCase = true)) {
                relation = r
                break
            }
        }

        // 6. Accident Previous & Existing Policy
        if (Regex("""(accident|previous accident).*?(yes|y|tick|✓)""", RegexOption.IGNORE_CASE).containsMatchIn(fullText)) {
            accidentPrevious = "Yes"
        }
        if (Regex("""(existing policy|policy).*?(yes|y|tick|✓)""", RegexOption.IGNORE_CASE).containsMatchIn(fullText)) {
            existingPolicy = "Yes"
        }

        // 7. Contextual Label Extraction for Names and Address in Handwritten Forms
        for (i in lines.indices) {
            val line = lines[i]
            val lower = line.lowercase()

            // Name of Mason
            if (lower.contains("name of the mason") || lower.contains("mason name") || lower.contains("name :") || lower.contains("name -") || (lower.startsWith("name") && !lower.contains("nominee"))) {
                val value = extractAfterColonOrNextLine(lines, i)
                if (value.isNotBlank() && name.isBlank()) {
                    name = FormValidators.toTitleCase(cleanHandwrittenName(value))
                }
            }

            // Nominee
            if (lower.contains("nominee")) {
                val value = extractAfterColonOrNextLine(lines, i)
                if (value.isNotBlank() && nominee.isBlank()) {
                    nominee = FormValidators.toTitleCase(cleanHandwrittenName(value))
                }
            }

            // Address
            if (lower.contains("address") || lower.contains("village") || lower.contains("mandal")) {
                val value = extractAfterColonOrNextLine(lines, i)
                if (value.isNotBlank() && address.isBlank()) {
                    address = value.trim()
                }
            }
        }

        // Handwriting assistance notes
        if (aadhaar.isBlank()) notes.add("Handwritten Aadhaar was not detected automatically")
        if (name.isBlank()) notes.add("Mason Name needs review/entry")
        if (phone.isBlank()) notes.add("Phone number needs review/entry")
        if (dob.isBlank() && age.isBlank()) notes.add("Handwritten Age/DOB needs manual check")

        return ExtractedRegistrationFields(
            rawText = fullText,
            aadhaarNo = aadhaar,
            name = name,
            address = address,
            age = age,
            nominee = nominee,
            relation = relation,
            phoneNumber = phone,
            dob = dob,
            gender = gender,
            accidentPrevious = accidentPrevious,
            existingPolicy = existingPolicy,
            confidenceNotes = notes
        )
    }

    private fun cleanHandwrittenName(value: String): String {
        return value.replace(Regex("""[^a-zA-Z\s]"""), "").trim()
    }

    private fun extractAfterColonOrNextLine(lines: List<String>, index: Int): String {
        val line = lines[index]
        if (line.contains(":") && line.substringAfter(":").trim().length > 1) {
            return line.substringAfter(":").trim()
        }
        if (line.contains("-") && line.substringAfter("-").trim().length > 1) {
            return line.substringAfter("-").trim()
        }
        if (index + 1 < lines.size) {
            val nextLine = lines[index + 1].trim()
            if (!isKeyLabel(nextLine)) {
                return nextLine
            }
        }
        return ""
    }

    private fun isKeyLabel(line: String): Boolean {
        val lower = line.lowercase()
        return lower.startsWith("name") ||
                lower.startsWith("aadhaar") ||
                lower.startsWith("address") ||
                lower.startsWith("phone") ||
                lower.startsWith("dob") ||
                lower.startsWith("age") ||
                lower.startsWith("nominee") ||
                lower.startsWith("relation") ||
                lower.startsWith("gender")
    }
}
