package com.otakeessen.underpressure.data.export

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Manages the export of chart images to the application cache.
 */
class ChartExportManager(
    private val context: Context
) {
    private val filenameFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

    /**
     * Saves a bitmap as a PNG file in the app's cache directory.
     *
     * @param bitmap The chart bitmap to save.
     * @return The saved File object.
     */
    suspend fun saveChartToCache(bitmap: Bitmap): File = withContext(Dispatchers.IO) {
        val timestamp = LocalDateTime.now().format(filenameFormatter)
        val filename = "chart_export_$timestamp.png"
        
        // Use the cache directory configured in file_paths.xml (path=".")
        val file = File(context.cacheDir, filename)
        
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        
        file
    }
}

