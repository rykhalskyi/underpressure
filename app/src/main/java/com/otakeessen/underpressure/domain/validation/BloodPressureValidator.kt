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

    sealed class Error : ValidationResult() {
        object EmptyInput : Error()
        object InvalidFormat : Error()
        object InvalidNumbers : Error()
        object IncorrectMeasurements : Error()
    }
}

/**
 * Utility class to parse and validate blood pressure measurements from a single string.
 * Supported formats: "120/80@72", "120 80 72", "120,80,72", "120/80/72", "120 80", etc.
 */
class BloodPressureValidator {

    private val regex = Regex("""^(\d{2,3})[/\s,]+(\d{2,3})(?:[/\s,@]+(\d{2,3}))?\s*$""")

    /**
     * Validates and parses the input string.
     * @param input The raw input string from the user.
     * @return [ValidationResult.Success] with parsed values or [ValidationResult.Error].
     */
    fun validate(input: String): ValidationResult {
        if (input.isBlank()) {
            return ValidationResult.Error.EmptyInput
        }

        val matchResult = regex.find(input.trim())
            ?: return ValidationResult.Error.InvalidFormat

        return try {
            val groups = matchResult.groupValues
            val sys = groups[1].toInt()
            val dia = groups[2].toInt()
            val pulse = if (groups.size > 3 && groups[3].isNotEmpty()) groups[3].toInt() else 0

            // Range checks
            if (sys < 40 || sys > 300 || dia < 20 || dia > 200) {
                return ValidationResult.Error.IncorrectMeasurements
            }
            if (pulse != 0 && (pulse < 30 || pulse > 300)) {
                return ValidationResult.Error.IncorrectMeasurements
            }

            // Logic check
            if (sys <= dia) {
                return ValidationResult.Error.IncorrectMeasurements
            }

            ValidationResult.Success(
                systolic = sys,
                diastolic = dia,
                pulse = pulse
            )
        } catch (e: Exception) {
            ValidationResult.Error.InvalidNumbers
        }
    }
}

