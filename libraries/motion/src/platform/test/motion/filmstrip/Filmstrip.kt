/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package platform.test.motion.filmstrip

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import platform.test.motion.golden.FrameId

/** Concatenates animation screenshots into a filmstrip image. */
class Filmstrip(private val screenshots: List<MotionScreenshot>) {
    init {
        require(screenshots.isNotEmpty()) { "Filmstrip must have at least one screenshot" }
    }

    /** Direction in which to concatenate the frames. */
    var orientation: FilmstripOrientation = FilmstripOrientation.AUTOMATIC

    private var screenshotWidth = screenshots.maxOf { it.bitmap.width }
    private var screenshotHeight = screenshots.maxOf { it.bitmap.height }

    /**
     * Scales down the screenshots, so that to the longer side will be at most [sizePx].
     *
     * All screenshots are scaled uniformly, the scale factor is determined by the largest
     * width/height of all screenshots.
     *
     * Aspect ratio of each screenshot is retained. Has no effect is [sizePx] is equal or larger
     * than the largest screenshot.
     */
    fun limitLongestSide(sizePx: Int) {
        val aspectRatio = screenshotWidth / screenshotHeight.toFloat()
        if (aspectRatio >= 1 && sizePx < screenshotWidth) {
            screenshotHeight = (screenshotHeight * (sizePx.toFloat() / screenshotWidth)).toInt()
            screenshotWidth = sizePx
        } else if (aspectRatio < 1 && sizePx < screenshotHeight) {
            screenshotWidth = (screenshotWidth * (sizePx.toFloat() / screenshotHeight)).toInt()
            screenshotHeight = sizePx
        }
    }

    /** Draws the screenshots into a new filmstrip [Bitmap]. */
    fun renderFilmstrip(): Bitmap {
        check(screenshots.isNotEmpty()) { "Filmstrip can only be rendered with screenshots" }
        return filmstripRenderer.render()
    }

    private val filmstripRenderer: FilmstripRenderer
        get() {
            val isHorizontal =
                when (orientation) {
                    FilmstripOrientation.HORIZONTAL -> true
                    FilmstripOrientation.VERTICAL -> false
                    FilmstripOrientation.AUTOMATIC -> screenshotWidth <= screenshotHeight
                }

            val scaleX = screenshotWidth.toFloat() / screenshots.maxOf { it.bitmap.width }
            val scaleY = screenshotHeight.toFloat() / screenshots.maxOf { it.bitmap.height }
            val scale = min(scaleX, scaleY).coerceAtMost(1f)

            return if (isHorizontal) {
                HorizontalFilmstripRenderer(screenshots, screenshotWidth, screenshotHeight, scale)
            } else {
                VerticalFilmstripRenderer(screenshots, screenshotWidth, screenshotHeight, scale)
            }
        }
}

/** Orientation in which screenshots are stitched together to a filmstrip. */
enum class FilmstripOrientation {
    /** Horizontal for screenshots taller than wide, and vice versa */
    AUTOMATIC,
    HORIZONTAL,
    VERTICAL,
}

/** An animation screenshot annotated with the frame its originating from. */
data class MotionScreenshot(val frameId: FrameId, val bitmap: Bitmap)

private sealed class FilmstripRenderer(
    val screenshots: List<MotionScreenshot>,
    val screenshotWidth: Int,
    val screenshotHeight: Int,
    val scale: Float,
) {
    val bitmapConfig = checkNotNull(screenshots.first().bitmap.config)

    val labels = screenshots.map { it.frameId.label }

    val backgroundPaint =
        Paint().apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }

    val textPaint =
        Paint().apply {
            style = Paint.Style.FILL
            textSize = 20f
            color = Color.BLACK
        }

    val labelMargin = 5
    val labelWidth = ceil(labels.map { textPaint.measureText(it) }.max()).toInt()
    val labelHeight = ceil(textPaint.textSize).toInt()

    abstract fun render(): Bitmap
}

private class HorizontalFilmstripRenderer(
    screenshots: List<MotionScreenshot>,
    screenshotWidth: Int,
    screenshotHeight: Int,
    scale: Float,
) : FilmstripRenderer(screenshots, screenshotWidth, screenshotHeight, scale) {

    init {
        textPaint.textAlign = Paint.Align.CENTER
    }

    override fun render(): Bitmap {
        val tileWidth = max(screenshotWidth, labelWidth)

        val width = screenshots.size * tileWidth
        val height = screenshotHeight + labelHeight + 2 * labelMargin

        val filmstrip = Bitmap.createBitmap(width, height, bitmapConfig)
        val canvas = Canvas(filmstrip)

        // Background behind the labels
        canvas.drawRect(
            /* left = */ 0f,
            /* top = */ screenshotHeight.toFloat(),
            /* right = */ width.toFloat(),
            /* bottom = */ height.toFloat(),
            /* paint = */ backgroundPaint,
        )

        val transform = Matrix()
        var x = 0f
        for ((screenshot, label) in screenshots.zip(labels)) {
            val left = x + (tileWidth - screenshotWidth) / 2

            transform.reset()
            transform.setTranslate(left, 0f)
            transform.postScale(scale, scale, left, 0f)

            canvas.drawBitmap(
                /* bitmap = */ screenshot.bitmap,
                /* matrix = */ transform,
                /* paint = */ backgroundPaint,
            )

            canvas.drawText(
                /* text = */ label,
                /* x = */ x + tileWidth / 2,
                /* y = */ (screenshotHeight + labelMargin + labelHeight).toFloat(),
                /* paint = */ textPaint,
            )

            x += tileWidth
        }
        return filmstrip
    }
}

private class VerticalFilmstripRenderer(
    screenshots: List<MotionScreenshot>,
    screenshotWidth: Int,
    screenshotHeight: Int,
    scale: Float,
) : FilmstripRenderer(screenshots, screenshotWidth, screenshotHeight, scale) {
    override fun render(): Bitmap {
        val tileHeight = max(screenshotHeight, labelHeight + 2 * labelMargin)

        val width = screenshotWidth + labelWidth + 2 * labelMargin
        val height = screenshots.size * tileHeight

        val filmstrip = Bitmap.createBitmap(width, height, bitmapConfig)
        val canvas = Canvas(filmstrip)

        canvas.drawRect(
            /* left = */ screenshotWidth.toFloat(),
            /* top = */ 0f,
            /* right = */ width.toFloat(),
            /* bottom = */ height.toFloat(),
            /* paint = */ backgroundPaint,
        )

        val transform = Matrix()
        var y = 0f
        for ((screenshot, label) in screenshots.zip(labels)) {
            val top = y + (tileHeight - screenshotHeight) / 2
            transform.reset()
            transform.setTranslate(0f, top)
            transform.postScale(scale, scale, 0f, top)

            canvas.drawBitmap(
                /* bitmap = */ screenshot.bitmap,
                /* matrix = */ transform,
                /* paint = */ backgroundPaint,
            )

            val textBounds = Rect()
            textPaint.getTextBounds(label, 0, label.length, textBounds)
            canvas.drawText(
                /* text = */ label,
                /* x = */ (screenshotWidth + labelMargin).toFloat(),
                /* y = */ y + (tileHeight + textBounds.height()) / 2,
                /* paint = */ textPaint,
            )

            y += tileHeight
        }
        return filmstrip
    }
}
