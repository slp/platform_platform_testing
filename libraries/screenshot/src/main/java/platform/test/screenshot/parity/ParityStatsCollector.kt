/*
 * Copyright 2024 The Android Open Source Project
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

package platform.test.screenshot.parity

import platform.test.screenshot.matchers.MatchResult
import platform.test.screenshot.proto.ScreenshotResultProto.DiffResult.ComparisonStatistics

/**
 * A class which collects and reports statistics of screenshot test parity.
 *
 * The objects of this class collect screenshot test parity (in terms of percentage of different
 * pixels between test and golden images), and report parity spectrum upon user's request.
 *
 * Screenshot test parity spectrum is interpreted as statements like:
 * - X1 (Y1 percent of) test images match golden images.
 * - X2 (Y2 percent of) test images are at least 99% the same as golden images.
 * - X3 (Y3 percent of) test images are 95% - 99% the same as golden images.
 * - ...
 */
public class ParityStatsCollector {
    private val testStats : HashMap<String, MutableList<String>>
        = HashMap<String, MutableList<String>>()

    fun clear() {
        testStats.clear()
    }

    fun collectTestStats(testIdentifier: String, matchResult: MatchResult) {
        if (matchResult.matches) {
            val tmpList = testStats.getOrDefault(EXACTLY_SAME, mutableListOf<String>())
            tmpList.add(testIdentifier)
            testStats[EXACTLY_SAME] = tmpList
        } else {
            val numDiffPixels = matchResult.comparisonStatistics.numberPixelsDifferent
            val numPixels = matchResult.comparisonStatistics.numberPixelsCompared
            if (numDiffPixels < numPixels * 0.01) {
                val tmpList = testStats.getOrDefault(SAME99, mutableListOf<String>())
                tmpList.add(testIdentifier)
                testStats[SAME99] = tmpList
            } else if (numDiffPixels < numPixels * 0.05) {
                val tmpList = testStats.getOrDefault(SAME95, mutableListOf<String>())
                tmpList.add(testIdentifier)
                testStats[SAME95] = tmpList
            } else if (numDiffPixels < numPixels * 0.10) {
                val tmpList = testStats.getOrDefault(SAME90, mutableListOf<String>())
                tmpList.add(testIdentifier)
                testStats[SAME90] = tmpList
            } else {
                val tmpList = testStats.getOrDefault(PIXEL_DIFFERENT, mutableListOf<String>())
                tmpList.add(testIdentifier)
                testStats[PIXEL_DIFFERENT] = tmpList
            }
        }
    }

    fun report() {
        testStats.forEach { entry ->
            println("${entry.key} : ${entry.value.size} test(s).")
        }
        println("Tests with significant different pixel number: ${testStats[PIXEL_DIFFERENT]}")
    }

    private companion object {
        const val EXACTLY_SAME = "exactly_same"
        const val SAME99 = "pixel_same99"
        const val SAME95 = "pixel_same95"
        const val SAME90 = "pixel_same90"
        const val PIXEL_DIFFERENT = "pixel_different"
    }
}
