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

package android.platform.helpers.notesrole

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipDescription.MIMETYPE_TEXT_INTENT
import android.content.ClipDescription.MIMETYPE_TEXT_URILIST
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_LAUNCH_CAPTURE_CONTENT_ACTIVITY_FOR_NOTE
import android.content.Intent.CAPTURE_CONTENT_FOR_NOTE_FAILED
import android.content.Intent.CAPTURE_CONTENT_FOR_NOTE_SUCCESS
import android.content.Intent.EXTRA_CAPTURE_CONTENT_FOR_NOTE_STATUS_CODE
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.view.setPadding

/** Sample notes app activity for app clips verification in end to end tests. */
class NotesAppActivity : ComponentActivity() {

    private val finishBroadcastReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                finish()
            }
        }

    private lateinit var appClipsLauncher: ActivityResultLauncher<Intent>
    private lateinit var rootLinearLayout: LinearLayout
    private var screenshotUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        registerReceiver(
            finishBroadcastReceiver,
            IntentFilter(FINISH_NOTES_APP_ACTIVITY_ACTION),
            RECEIVER_EXPORTED
        )

        appClipsLauncher =
            registerForActivityResult(StartActivityForResult(), this::onAppClipsActivityResult)

        actionBar?.hide()
        setContentView(createContentView())
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(finishBroadcastReceiver)
        screenshotUri?.let { contentResolver.delete(it, /* extras= */ null) }
    }

    private fun createContentView(): View =
        LinearLayout(this)
            .apply {
                orientation = LinearLayout.VERTICAL
                setPadding(PADDING)
                addView(createTriggerAppClipsButton())
            }
            .also { rootLinearLayout = it }

    private fun createTriggerAppClipsButton(): Button =
        Button(this).apply {
            text = APP_CLIPS_BUTTON_TEXT
            setOnClickListener {
                appClipsLauncher.launch(Intent(ACTION_LAUNCH_CAPTURE_CONTENT_ACTIVITY_FOR_NOTE))
            }
        }

    private fun onAppClipsActivityResult(activityResult: ActivityResult) {
        // Activity/app is killed after each test, so de-clutter views for easier verification.
        rootLinearLayout.removeAllViews()

        // Add a text view with specific text to make it easier for verification in tests.
        TextView(this).apply { text = RESPONSE_TEXT }.also { rootLinearLayout.addView(it) }

        val result = activityResult.data
        val statusCode =
            result?.getIntExtra(
                EXTRA_CAPTURE_CONTENT_FOR_NOTE_STATUS_CODE,
                CAPTURE_CONTENT_FOR_NOTE_FAILED
            )
        // In case app clips returns error, show it and return early.
        if (statusCode != CAPTURE_CONTENT_FOR_NOTE_SUCCESS) {
            showErrorViewWithText("Error status code: $statusCode")
            return
        }

        // Show backlinks data, if available.
        rootLinearLayout.addView(
            result.clipData.let { clipData ->
                TextView(this).apply {
                    text =
                        clipData?.let { getStringFormattedBacklinksData(it) } ?: NO_BACKLINKS_TEXT
                }
            }
        )

        // Show app clips screenshot, if available.
        result.data
            ?.also { screenshotUri = it }
            ?.let { uri ->
                val imageView = ImageView(this).apply { setImageURI(uri) }
                rootLinearLayout.addView(imageView)
            }
    }

    private fun showErrorViewWithText(error: String) {
        TextView(this).apply { text = error }.also { rootLinearLayout.addView(it) }
    }

    companion object {
        private const val PADDING = 50

        const val FINISH_NOTES_APP_ACTIVITY_ACTION: String = "FINISH_NOTES_APP_ACTIVITY_ACTION"
        const val APP_CLIPS_BUTTON_TEXT: String = "TRIGGER APP CLIPS"
        const val RESPONSE_TEXT: String = "RESPONSE"
        const val NO_BACKLINKS_TEXT: String = "NO BACKLINKS AVAILABLE"

        /** Formats Backlinks [ClipData] in readable format for verification. */
        fun getStringFormattedBacklinksData(clipData: ClipData): String =
            clipData.let { data ->
                data
                    .getItemAt(0)
                    .let { dataItem ->
                        when (data.description.getMimeType(0)) {
                            MIMETYPE_TEXT_URILIST -> dataItem.uri.toString()
                            MIMETYPE_TEXT_INTENT -> dataItem.intent.toString()
                            else -> dataItem.toString()
                        }
                    }
                    .let { backlinksData -> "${data.description.label}\n$backlinksData" }
            }
    }
}
