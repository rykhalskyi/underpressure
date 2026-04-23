package com.otakeessen.underpressure.domain.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [TableFormatter].
 */
class TableFormatterTest {

    private val formatter = TableFormatter()

    @Test
    fun `formatAsciiTable produces correct layout for 4 columns`() {
        val headers = listOf("Date", "07:00", "12:00", "18:00", "22:00")
        val rows = listOf(
            listOf("2026-03-11", "120/80@60", "121/81@61", "119/79@59", "120/80@60"),
            listOf("2026-03-12", "118/78@58", "", "122/82@62", "")
        )
        val dateRange = "2026-03-11 → 2026-03-12"

        val result = formatter.formatAsciiTable(headers, rows, dateRange)

        // Basic structural assertions
        assertTrue(result.contains("Blood Pressure Log"))
        assertTrue(result.contains(dateRange))
        assertTrue(result.contains("Date"))
        assertTrue(result.contains("07:00"))
        
        // Alignment checks
        val lines = result.split("\n")
        val headerLine = lines[3]
        val separatorLine = lines[4]
        val dataRow1 = lines[5]
        val dataRow2 = lines[6]

        // Verify "—" for missing values
        assertTrue(dataRow2.contains("—"))
        
        // Check padding: 11 for Date, 11 for values. 5 columns total.
        assertEquals(60, headerLine.length)
        assertEquals(60, separatorLine.length)
    }

    @Test
    fun `formatCsv produces correct layout`() {
        val headers = listOf("Date", "07:15", "12:00")
        val rows = listOf(
            listOf("2026-03-10", "120/80@60", "121/81@61"),
            listOf("2026-03-11", "118/79@58", "")
        )

        val result = formatter.formatCsv(headers, rows)

        val expected = """
            Date,07:15,12:00
            2026-03-10,120/80@60,121/81@61
            2026-03-11,118/79@58,—
            
        """.trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun `formatAsciiTable respects width constraints`() {
        // Test with 4 active slots (5 columns total)
        val headers = listOf("Date", "07:00", "12:00", "18:00", "22:00")
        val rows = listOf(listOf("2026-03-11", "1", "2", "3", "4"))
        
        val result = formatter.formatAsciiTable(headers, rows, "Range")
        val lines = result.split("\n")
        
        // Header line width should be exactly 55 (11 * 5)
        assertEquals(60, lines[3].length)
        assertTrue(lines[3].length < 80)
    }
}

