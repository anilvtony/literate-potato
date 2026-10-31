package com.policyregistration.app

import com.policyregistration.app.domain.FormValidators
import org.junit.Assert.*
import org.junit.Test

class FormValidatorsTest {

    @Test
    fun testTitleCase() {
        val input = "RAMESH KUMAR REDDY"
        val expected = "Ramesh Kumar Reddy"
        assertEquals(expected, FormValidators.toTitleCase(input))
    }

    @Test
    fun testAadhaarValidation() {
        assertTrue(FormValidators.isValidAadhaar("548912345678"))
        assertFalse(FormValidators.isValidAadhaar("12345"))
        assertFalse(FormValidators.isValidAadhaar("54891234567899"))
    }

    @Test
    fun testPhoneValidation() {
        assertTrue(FormValidators.isValidPhone("9876543210"))
        assertTrue(FormValidators.isValidPhone("8123456789"))
        assertFalse(FormValidators.isValidPhone("1234567890"))
        assertFalse(FormValidators.isValidPhone("98765"))
    }

    @Test
    fun testDateNormalizationAndAgeCalculation() {
        val normalized1 = FormValidators.normalizeDate("15/08/1990")
        assertEquals("15.08.1990", normalized1)

        val normalized2 = FormValidators.normalizeDate("01-01-2000")
        assertEquals("01.01.2000", normalized2)

        val age = FormValidators.calculateAge("01.01.2000")
        assertNotNull(age)
        assertTrue(age!! >= 26)
    }
}

