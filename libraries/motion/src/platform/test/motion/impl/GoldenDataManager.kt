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

package platform.test.motion.impl

import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import org.json.JSONObject
import platform.test.screenshot.GoldenPathManager

/** Reads expected golden data from assets and exports actual generated golden data. */
internal class GoldenDataManager(private val goldenPathManager: GoldenPathManager) {

    /**
     * Reads a golden JSON file from test assets.
     *
     * @throws GoldenNotFoundException if the golden does not exist.
     * @throws JSONException if the file is not valid JSON.
     */
    fun readGoldenJson(goldenIdentifier: String): JSONObject {
        val path = goldenPathManager.goldenIdentifierResolver(goldenIdentifier, JSON_EXTENSION)
        val instrument = InstrumentationRegistry.getInstrumentation()
        return listOf(instrument.targetContext.applicationContext, instrument.context)
            .firstNotNullOfOrNull { context ->
                try {
                    context.assets.open(path).bufferedReader().use { JSONObject(it.readText()) }
                } catch (e: FileNotFoundException) {
                    null
                }
            }
            ?: throw GoldenNotFoundException(path)
    }

    /** Writes generated, actual golden JSON data to the device, to be picked up by TF. */
    fun writeGeneratedJson(goldenIdentifier: String, data: JSONObject) {
        requireValidGoldenIdentifier(goldenIdentifier)

        val relativeGoldenPath =
            goldenPathManager.goldenIdentifierResolver(goldenIdentifier, JSON_EXTENSION)
        val goldenFile = File(goldenPathManager.deviceLocalPath).resolve(relativeGoldenPath)

        val goldenFileDirectory = checkNotNull(goldenFile.parentFile)
        if (!goldenFileDirectory.exists()) {
            goldenFileDirectory.mkdirs()
        }
        try {
            FileOutputStream(goldenFile).bufferedWriter().use {
                it.write(data.toString(JSON_INDENTATION))
            }
        } catch (e: Exception) {
            throw IOException("Failed to write generated JSON (${goldenFile.absolutePath}). ", e)
        }
    }

    private fun requireValidGoldenIdentifier(goldenIdentifier: String) {
        require(goldenIdentifier.matches(GOLDEN_IDENTIFIER_REGEX)) {
            "Golden identifier '$goldenIdentifier' does not satisfy the naming " +
                "requirement. Allowed characters are: '[A-Za-z0-9_-]'"
        }
    }

    companion object {
        private const val JSON_EXTENSION = "json"
        private const val JSON_INDENTATION = 2
        private val GOLDEN_IDENTIFIER_REGEX = "^[A-Za-z0-9_-]+$".toRegex()
    }
}

class GoldenNotFoundException(val missingGoldenFile: String) : Exception()
