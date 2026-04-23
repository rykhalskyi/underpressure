package com.otakeessen.underpressure.domain.validation

/**
 * Result of the blood pressure string validation.
 */
sealed class ValidationResult {
    data class Success(
        val systolic: Int,
        val diastolic: Int,
        val pulse: Int
    ) : ValidationResult()

    data class Error(val message: String) : ValidationResult()
}

/**
 * Utility class to parse and validate blood pressure measurements from a single string.
 * Format: "SYS/DIA @PULSE" (e.g., "120/80 @72")
 */
class BloodPressureValidator {

    private val regex = Regex("""^(\d{2,3})/(\d{2,3})\s*@\s*(\d{2,3})$""")

    /**
     * Validates and parses the input string.
     * @param input The raw input string from the user.
     * @return [ValidationResult.Success] with parsed values or [ValidationResult.Error] with a reason.
     */
    fun validate(input: String): ValidationResult {
        if (input.isBlank()) {
            return ValidationResult.Error("Input cannot be empty")
        }

        val matchResult = regex.find(input.trim())
            ?: return ValidationResult.Error("Invalid format. Use: SYS/DIA @PULSE")

        return try {
            val (sys, dia, pulse) = matchResult.destructured
            ValidationResult.Success(
                systolic = sys.toInt(),
                diastolic = dia.toInt(),
                pulse = pulse.toInt()
            )
        } catch (e: Exception) {
            ValidationResult.Error("Values must be valid numbers")
        }
    }
}

