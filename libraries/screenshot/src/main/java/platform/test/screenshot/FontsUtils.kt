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
package platform.test.screenshot

import android.content.Context
import android.content.res.loader.ResourcesLoader
import android.content.res.loader.ResourcesProvider
import android.os.ParcelFileDescriptor
import android.system.Os
import android.util.SparseIntArray
import android.widget.RemoteViews
import com.google.common.io.ByteStreams
import java.io.File
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

private const val FIRST_RESOURCE_COLOR_ID = android.R.color.system_neutral1_0
private const val LAST_RESOURCE_COLOR_ID = android.R.color.system_accent3_1000
private const val ARSC_ENTRY_SIZE = 16
private const val PLACEHOLDER_GOOGLE_SANS = "plh-go-sans"
private const val PLACEHOLDER_GOOGLE_SANS_TEXT = "plh-go-sans-text"
private const val FONT_GOOGLE_SANS = "google-sans"
private const val FONT_GOOGLE_SANS_TEXT = "google-sans-text"

/** Creates and applies a resources provider for Google fonts to a context. */
fun createAndApplyResourcesProvider(context: Context) {
    try {
        val arscPath = System.getProperty("arsc.file.path")
        val oldContents = File(arscPath).readBytes()
        val contentBytes = compiledResourcesContentForGoogleFonts(oldContents)
        if (contentBytes == null) {
            return
        }

        var arscFile: FileDescriptor? = null
        var pfd: ParcelFileDescriptor? = null
        try {
            arscFile = Os.memfd_create("font_resources.arsc", /* flags= */ 0)
            // Note: This must not be closed through the OutputStream.
            val pipeWriter = FileOutputStream(arscFile)
            pipeWriter.write(contentBytes);

            pfd = ParcelFileDescriptor.dup(arscFile)
            val colorsLoader = ResourcesLoader()
            colorsLoader.addProvider(
                ResourcesProvider.loadFromTable(pfd, /* assetsProvider= */ null))
            context.resources.addLoaders(colorsLoader)
        } finally {
            if (arscFile != null) {
                Os.close(arscFile)
            }
            if (pfd != null && pfd.getFileDescriptor() != null && pfd.getFileDescriptor().valid()) {
                pfd.close()
            }
        }
    } catch (ex: Exception) {
        println("Failed to setup the context for fonts: ${ex}")
    }
}

/** Manipulates the compiled resources content manipulated for Google fonts. */
private fun compiledResourcesContentForGoogleFonts(oldContent: ByteArray?): ByteArray? {
    if (oldContent == null) {
        return null
    }
    val newContent = oldContent.copyOf()
    stringReplaceOnce(newContent, PLACEHOLDER_GOOGLE_SANS_TEXT, FONT_GOOGLE_SANS_TEXT)
    stringReplaceOnce(newContent, PLACEHOLDER_GOOGLE_SANS, FONT_GOOGLE_SANS)
    return newContent
}

/** Replaces the first occurrence of `fromValue` to `toValue`. */
private fun stringReplaceOnce(contents: ByteArray, fromValue: String, toValue: String) {
    if (fromValue.length != toValue.length) { return }
    for (i in 0..<contents.size) {
        if (i + fromValue.length > contents.size) { return }
        var isEqual: Boolean = true
        for (j in 0..<fromValue.length) {
            if (contents[i + j] != fromValue.get(j).toByte()) {
                isEqual = false
                break
            }
        }
        if (isEqual) {
            for (j in 0..<toValue.length) {
                contents[i + j] = toValue.get(j).toByte();
            }
            break
        }
    }
}
