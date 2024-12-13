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

import android.media.MediaCodec
import android.media.MediaCodecInfo.CodecCapabilities
import android.media.MediaFormat
import android.media.MediaFormat.KEY_BIT_RATE
import android.media.MediaFormat.KEY_COLOR_FORMAT
import android.media.MediaFormat.KEY_FRAME_RATE
import android.media.MediaMuxer
import android.view.Surface
import platform.test.motion.golden.TimestampFrameId

/** Produces an MP4 based on the [screenshots]. */
class VideoRenderer(private val screenshots: List<MotionScreenshot>) {

    private var screenshotWidth = screenshots.maxOf { it.bitmap.width }.roundUpToNextMultipleOf16()
    private var screenshotHeight =
        screenshots.maxOf { it.bitmap.height }.roundUpToNextMultipleOf16()

    /**
     * Creates an MP4 file at [path], which will contain all screenshots.
     *
     * [bitsPerPixel] is used to estimate the bitrate needed.
     */
    fun renderToFile(path: String, bitsPerPixel: Float = 0.25f) {
        require(screenshots.isNotEmpty()) { "Filmstrip must have at least one screenshot" }
        val muxer = MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        val bitrate = (screenshotWidth * screenshotHeight * bitsPerPixel * FRAME_RATE).toInt()
        val mime = "video/avc"
        val format = MediaFormat.createVideoFormat(mime, screenshotWidth, screenshotHeight)
        format.setInteger(KEY_BIT_RATE, bitrate)
        format.setFloat(KEY_FRAME_RATE, FRAME_RATE)
        format.setInteger(KEY_COLOR_FORMAT, CodecCapabilities.COLOR_FormatSurface)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)

        val codec = MediaCodec.createEncoderByType(mime)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val surface = codec.createInputSurface()
        codec.start()

        encodeScreenshotsInVideo(codec, muxer, surface)

        codec.stop()
        codec.release()
        muxer.stop()
        muxer.release()
    }

    private fun encodeScreenshotsInVideo(
        encoder: MediaCodec,
        muxer: MediaMuxer,
        surface: Surface,
    ) {
        val bufferInfo = MediaCodec.BufferInfo()
        val screenshotIterator = screenshots.iterator()
        var isEndOfStream = false
        var videoTrackIndex = -1

        // The encoder uses the system clock of the [unlockCanvasAndPost] call as frame time.
        // However, this is arbitrary, as encoding happens as fast as possible. To avoid the extra
        // complexity of video bitmap format conversion that would be required when using
        // [queueInputBuffer] instead (which would allow specifying the presentation time), this
        // will override the presentation time when muxing instead.
        val framePresentationTimesUsIterator =
            buildList {
                    add(0L)
                    var presentationTimeUs = 0L
                    screenshots
                        .zipWithNext { first, second ->
                            if (
                                first.frameId is TimestampFrameId &&
                                    second.frameId is TimestampFrameId
                            ) {
                                second.frameId.milliseconds - first.frameId.milliseconds
                            } else {
                                // Exactly one frame for before / after
                                FRAME_DURATION
                            }
                        }
                        .forEach { frameDurationMillis ->
                            presentationTimeUs += frameDurationMillis * 1000L
                            add(presentationTimeUs)
                        }
                }
                .iterator()

        while (true) {
            if (screenshotIterator.hasNext()) {
                surface.lockCanvas(null).also { canvas ->
                    val screenshot = screenshotIterator.next()
                    canvas.drawBitmap(screenshot.bitmap, 0.0f, 0.0f, null)
                    surface.unlockCanvasAndPost(canvas)
                }
            } else if (!isEndOfStream) {
                encoder.signalEndOfInputStream()
                isEndOfStream = true
            }

            val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, DEQUEUE_TIMEOUT_US)
            if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // No output available yet.
                continue
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                videoTrackIndex = muxer.addTrack(encoder.outputFormat)
                muxer.start()
            } else if (outputBufferIndex >= 0) {
                val encodedDataBuffer =
                    encoder.getOutputBuffer(outputBufferIndex)
                        ?: throw RuntimeException("encoderOutputBuffer $outputBufferIndex was null")

                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    // Config was already sent to the muxer in INFO_OUTPUT_FORMAT_CHANGED.
                    bufferInfo.size = 0
                }

                if (bufferInfo.size != 0) {
                    encodedDataBuffer.position(bufferInfo.offset)
                    encodedDataBuffer.limit(bufferInfo.offset + bufferInfo.size)
                    check(framePresentationTimesUsIterator.hasNext()) {
                        "More than the number of input frames are sent to the output"
                    }
                    bufferInfo.presentationTimeUs = framePresentationTimesUsIterator.next()
                    muxer.writeSampleData(videoTrackIndex, encodedDataBuffer, bufferInfo)
                }

                encoder.releaseOutputBuffer(outputBufferIndex, false)

                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    return
                }
            } else {
                throw AssertionError("Unexpected dequeueOutputBuffer response $outputBufferIndex")
            }
        }
    }

    companion object {
        // Tests produce a frame every 16ms (62.5fps)
        const val FRAME_DURATION = 16L
        const val FRAME_RATE = 1000f / FRAME_DURATION
        const val DEQUEUE_TIMEOUT_US = 10_000L

        private fun Int.roundUpToNextMultipleOf16(): Int = (this + 15) and 0xF.inv()
    }
}
