package com.otakeessen.underpressure.domain.export

/**
 * Utility for formatting blood pressure measurement data into various string representations.
 * Adheres to monospaced ASCII table standards and standard CSV formats.
 */
class TableFormatter {

    /**
     * Formats a list of measurement data into a human-readable monospaced ASCII table.
     *
     * @param headers List of column headers (e.g., ["Date", "07:00", "12:00"]).
     * @param rows List of rows, where each row is a list of cell values as strings.
     * @param dateRange A string representing the date range (e.g., "2026-03-10 → 2026-03-11").
     * @return A formatted ASCII table as a String.
     */
    fun formatAsciiTable(
        headers: List<String>,
        rows: List<List<String>>,
        dateRange: String,
    ): String {
        if (headers.isEmpty()) return ""

        val sb = StringBuilder()
        sb.append("Blood Pressure Log\n")
        sb.append("$dateRange\n\n")

        // Define fixed widths based on requirements: Date ~ 10, Values ~ 10-12
        val dateWidth = 12 // YYYY-MM-DD plus one space
        val valueWidth = 12 // Values padded to 11 chars

        // Build Header Row
        headers.forEachIndexed { index, header ->
            val width = if (index == 0) dateWidth else valueWidth
            sb.append(header.padEnd(width))
        }
        sb.append("\n")

        // Build Separator Line
        val totalWidth = dateWidth + (headers.size - 1) * valueWidth
        sb.append("-".repeat(totalWidth.coerceAtMost(80)))
        sb.append("\n")
        //"—"
        // Build Data Rows
        rows.forEach { row ->
            row.forEachIndexed { index, cell ->
                val width = if (index == 0) dateWidth else valueWidth
                val value = if (cell.isBlank()) "—" else cell
                sb.append(value.padEnd(width))
            }
            sb.append("\n")
        }

        return sb.toString()
    }

    /**
     * Formats a list of measurement data into a standard comma-separated values (CSV) string.
     *
     * @param headers List of column headers.
     * @param rows List of rows, where each row is a list of cell values as strings.
     * @return A formatted CSV as a String.
     */
    fun formatCsv(
        headers: List<String>,
        rows: List<List<String>>,
    ): String {
        if (headers.isEmpty()) return ""

        val sb = StringBuilder()
        
        // Headers
        sb.append(headers.joinToString(","))
        sb.append("\n")

        // Data Rows
        rows.forEach { row ->
            sb.append(row.joinToString(",") { it.ifBlank { "—" } })
            sb.append("\n")
        }

        return sb.toString()
    }
}

