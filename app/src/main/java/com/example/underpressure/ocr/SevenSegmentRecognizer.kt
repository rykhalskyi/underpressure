package com.example.underpressure.ocr

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

class SevenSegmentRecognizer {

    companion object {
        private val SEGMENT_MAP = mapOf(
            listOf(1, 1, 1, 1, 1, 1, 0) to 0, // top, tr, br, bottom, bl, tl, no-middle
            listOf(0, 1, 1, 0, 0, 0, 0) to 1, // no-top, tr, br, no-bottom, no-bl, no-tl, no-middle
            listOf(1, 1, 0, 1, 1, 0, 1) to 2, // top, tr, no-br, bottom, bl, no-tl, middle
            listOf(1, 1, 1, 1, 0, 0, 1) to 3, // top, tr, br, bottom, no-bl, no-tl, middle
            listOf(1, 1, 1, 1, 1, 0, 1) to 3, // alternative 3
            listOf(0, 1, 1, 0, 0, 1, 1) to 4, // no-top, tr, br, no-bottom, no-bl, tl, middle
            listOf(1, 0, 1, 1, 0, 1, 1) to 5, // top, no-tr, br, bottom, no-bl, tl, middle
            listOf(1, 0, 1, 1, 1, 1, 1) to 6, // top, no-tr, br, bottom, bl, tl, middle
            listOf(1, 1, 1, 0, 0, 0, 0) to 7, // top, tr, br, no-bottom, no-bl, no-tl, no-middle
            listOf(1, 1, 1, 1, 1, 1, 1) to 8, // all segments on
            listOf(1, 1, 1, 1, 0, 1, 1) to 9  // top, tr, br, bottom, no-bl, tl, middle
        )
    }

    fun recognize(bitmap: Bitmap): String? {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        val result = recognize(mat)
        mat.release()
        return result
    }

    fun recognize(src: Mat): String? {
        val gray = Mat()
        if (src.channels() > 1) {
            Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)
        } else {
            src.copyTo(gray)
        }

        val edges = preprocess(src)
        val warped = detectDisplay(gray, edges)
        
        edges.release()
        gray.release()
        
        if (warped == null) return null
        
        val processed = segmentDigits(warped)
        warped.release()
        
        val digitCnts = findDigitContours(processed)
        if (digitCnts.isEmpty()) {
            processed.release()
            return null
        }
        
        val recognizedDigits = mutableListOf<Triple<Int, Int, Int>>() // x, y, digit
        for (c in digitCnts) {
            val rect = Imgproc.boundingRect(c)
            val roi = processed.submat(rect)
            val digit = classifyDigit(roi)
            if (digit != null) {
                recognizedDigits.add(Triple(rect.x, rect.y, digit))
            }
            roi.release()
        }
        processed.release()
        
        if (recognizedDigits.isEmpty()) return null
        
        // Group by row (y ± 3px)
        val rowGroups = mutableMapOf<Int, MutableList<Pair<Int, Int>>>() // y -> list of (x, digit)
        for (item in recognizedDigits) {
            val y = item.second
            var assigned = false
            for (groupY in rowGroups.keys) {
                if (Math.abs(y - groupY) <= 3) {
                    rowGroups[groupY]?.add(item.first to item.third)
                    assigned = true
                    break
                }
            }
            if (!assigned) {
                rowGroups[y] = mutableListOf(item.first to item.third)
            }
        }
        
        val result = StringBuilder()
        val sortedY = rowGroups.keys.sorted()
        for (y in sortedY) {
            val rowDigits = rowGroups[y]?.sortedBy { it.first } ?: continue
            val rowString = rowDigits.joinToString("") { it.second.toString() }
            if (result.isNotEmpty()) result.append("\n")
            result.append(rowString)
        }
        
        return result.toString()
    }

    private fun preprocess(src: Mat): Mat {
        val gray = Mat()
        if (src.channels() > 1) {
            Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)
        } else {
            src.copyTo(gray)
        }
        
        val blur = Mat()
        Imgproc.GaussianBlur(gray, blur, Size(5.0, 5.0), 0.0)
        
        val edges = Mat()
        Imgproc.Canny(blur, edges, 50.0, 200.0)
        
        gray.release()
        blur.release()
        
        return edges
    }

    private fun detectDisplay(gray: Mat, edges: Mat): Mat? {
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        
        // Sort contours by area in descending order
        contours.sortByDescending { Imgproc.contourArea(it) }
        
        var displayCnt: MatOfPoint2f? = null
        for (c in contours) {
            val c2f = MatOfPoint2f(*c.toArray())
            val peri = Imgproc.arcLength(c2f, true)
            val approx = MatOfPoint2f()
            Imgproc.approxPolyDP(c2f, approx, 0.02 * peri, true)
            
            if (approx.total() == 4L) {
                displayCnt = approx
                break
            }
        }
        
        val result = if (displayCnt != null) {
            fourPointTransform(gray, displayCnt)
        } else {
            null
        }
        
        hierarchy.release()
        return result
    }

    private fun fourPointTransform(image: Mat, pts: MatOfPoint2f): Mat {
        val points = pts.toArray()
        
        // Order points: top-left, top-right, bottom-right, bottom-left
        val rect = arrayOf(Point(), Point(), Point(), Point())
        
        val sum = points.map { it.x + it.y }
        rect[0] = points[sum.indexOf(sum.minOrNull())] // tl
        rect[2] = points[sum.indexOf(sum.maxOrNull())] // br
        
        val diff = points.map { it.y - it.x }
        rect[1] = points[diff.indexOf(diff.minOrNull())] // tr
        rect[3] = points[diff.indexOf(diff.maxOrNull())] // bl
        
        val tl = rect[0]
        val tr = rect[1]
        val br = rect[2]
        val bl = rect[3]
        
        val widthA = Math.sqrt(Math.pow(br.x - bl.x, 2.0) + Math.pow(br.y - bl.y, 2.0))
        val widthB = Math.sqrt(Math.pow(tr.x - tl.x, 2.0) + Math.pow(tr.y - tl.y, 2.0))
        val maxWidth = Math.max(widthA, widthB).toInt()
        
        val heightA = Math.sqrt(Math.pow(tr.x - br.x, 2.0) + Math.pow(tr.y - br.y, 2.0))
        val heightB = Math.sqrt(Math.pow(tl.x - bl.x, 2.0) + Math.pow(tl.y - bl.y, 2.0))
        val maxHeight = Math.max(heightA, heightB).toInt()
        
        val dst = MatOfPoint2f(
            Point(0.0, 0.0),
            Point((maxWidth - 1).toDouble(), 0.0),
            Point((maxWidth - 1).toDouble(), (maxHeight - 1).toDouble()),
            Point(0.0, (maxHeight - 1).toDouble())
        )
        
        val srcPts = MatOfPoint2f(rect[0], rect[1], rect[2], rect[3])
        val m = Imgproc.getPerspectiveTransform(srcPts, dst)
        val warped = Mat()
        Imgproc.warpPerspective(image, warped, m, Size(maxWidth.toDouble(), maxHeight.toDouble()))
        
        m.release()
        return warped
    }

    private fun segmentDigits(warped: Mat): Mat {
        val thresh = Mat()
        Imgproc.threshold(warped, thresh, 33.0, 255.0, Imgproc.THRESH_BINARY_INV)
        
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(1.0, 5.0))
        val opened = Mat()
        Imgproc.morphologyEx(thresh, opened, Imgproc.MORPH_OPEN, kernel)
        
        // Resize to height 500
        val ratio = 500.0 / opened.height()
        val width = (opened.width() * ratio).toInt()
        val resized = Mat()
        Imgproc.resize(opened, resized, Size(width.toDouble(), 500.0))
        
        val blur = Mat()
        Imgproc.GaussianBlur(resized, blur, Size(7.0, 7.0), 0.0)
        
        thresh.release()
        opened.release()
        kernel.release()
        resized.release()
        
        return blur
    }

    private fun findDigitContours(image: Mat): List<MatOfPoint> {
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(image, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        
        val digitCnts = mutableListOf<MatOfPoint>()
        for (c in contours) {
            val rect = Imgproc.boundingRect(c)
            if (rect.width in 20..125 && rect.height in 70..138) {
                digitCnts.add(c)
            }
        }
        
        // Group by height (±3px)
        val heightGroups = mutableMapOf<Int, MutableList<MatOfPoint>>()
        for (c in digitCnts) {
            val rect = Imgproc.boundingRect(c)
            val h = rect.height
            var assigned = false
            for (groupHeight in heightGroups.keys) {
                if (Math.abs(h - groupHeight) <= 3) {
                    heightGroups[groupHeight]?.add(c)
                    assigned = true
                    break
                }
            }
            if (!assigned) {
                heightGroups[h] = mutableListOf(c)
            }
        }
        
        val filteredDigitCnts = mutableListOf<MatOfPoint>()
        for (group in heightGroups.values) {
            if (group.size > 1) {
                filteredDigitCnts.addAll(group)
            }
        }
        
        hierarchy.release()
        return filteredDigitCnts
    }

    private fun classifyDigit(roi: Mat): Int? {
        val w = roi.width()
        val h = roi.height()
        
        if (w < 0.25 * h) {
            return 1
        }
        
        val segments = listOf(
            Rect((0.2 * w).toInt(), 0, (0.6 * w).toInt(), (0.2 * h).toInt()),             // top
            Rect((0.8 * w).toInt(), (0.2 * h).toInt(), (0.2 * w).toInt(), (0.3 * h).toInt()), // top-right
            Rect((0.8 * w).toInt(), (0.5 * h).toInt(), (0.2 * w).toInt(), (0.4 * h).toInt()), // bottom-right
            Rect((0.2 * w).toInt(), (0.8 * h).toInt(), (0.6 * w).toInt(), (0.2 * h).toInt()), // bottom
            Rect(0, (0.5 * h).toInt(), (0.2 * w).toInt(), (0.4 * h).toInt()),             // bottom-left
            Rect(0, (0.2 * h).toInt(), (0.2 * w).toInt(), (0.3 * h).toInt()),             // top-left
            Rect((0.2 * w).toInt(), (0.45 * h).toInt(), (0.6 * w).toInt(), (0.1 * h).toInt()) // middle
        )
        
        val on = mutableListOf<Int>()
        for (rect in segments) {
            // Ensure rect is within roi bounds
            val safeRect = Rect(
                Math.max(0, rect.x),
                Math.max(0, rect.y),
                Math.min(w - rect.x, rect.width),
                Math.min(h - rect.y, rect.height)
            )
            
            if (safeRect.width <= 0 || safeRect.height <= 0) {
                on.add(0)
                continue
            }
            
            val seg = roi.submat(safeRect)
            val total = Core.countNonZero(seg)
            val area = safeRect.width * safeRect.height
            val ratio = total.toDouble() / area
            
            if (ratio > 0.25) {
                on.add(1)
            } else {
                on.add(0)
            }
            seg.release()
        }
        
        return SEGMENT_MAP[on]
    }
}
