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
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
import android.content.Intent.FLAG_ACTIVITY_MULTIPLE_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_DOCUMENT
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity

/** Empty activity for app clips screenshots. */
class AppClipsScreenshotActivity : ComponentActivity() {

    private val launchDifferentAppInSplitScreenReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                launchDifferentAppInSplitScreen()
            }
        }

    private val launchSameAppInSplitScreenReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                launchSameAppInSplitScreen()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        registerReceiver(
            launchDifferentAppInSplitScreenReceiver,
            IntentFilter(LAUNCH_DIFFERENT_APP_IN_SPLIT_SCREEN_ACTION),
            RECEIVER_EXPORTED
        )

        registerReceiver(
            launchSameAppInSplitScreenReceiver,
            IntentFilter(LAUNCH_SAME_APP_IN_SPLIT_SCREEN_ACTION),
            RECEIVER_EXPORTED
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(launchDifferentAppInSplitScreenReceiver)
        unregisterReceiver(launchSameAppInSplitScreenReceiver)
    }

    private fun launchDifferentAppInSplitScreen() =
        startActivity(
            Intent().apply {
                setComponent(ComponentName(packageName, SPLIT_DIFFERENT_ACTIVITY_NAME))
                addFlags(
                    FLAG_ACTIVITY_NEW_TASK or
                        FLAG_ACTIVITY_LAUNCH_ADJACENT or
                        FLAG_ACTIVITY_MULTIPLE_TASK or
                        FLAG_ACTIVITY_NEW_DOCUMENT
                )
            }
        )

    private fun launchSameAppInSplitScreen() =
        startActivity(
            Intent().apply {
                setComponent(this@AppClipsScreenshotActivity.componentName)
                addFlags(
                    FLAG_ACTIVITY_NEW_TASK or
                        FLAG_ACTIVITY_LAUNCH_ADJACENT or
                        FLAG_ACTIVITY_MULTIPLE_TASK or
                        FLAG_ACTIVITY_NEW_DOCUMENT
                )
            }
        )

    companion object {
        const val LAUNCH_DIFFERENT_APP_IN_SPLIT_SCREEN_ACTION: String =
            "LAUNCH_DIFFERENT_APP_IN_SPLIT_SCREEN_ACTION"
        const val LAUNCH_SAME_APP_IN_SPLIT_SCREEN_ACTION: String =
            "LAUNCH_SAME_APP_IN_SPLIT_SCREEN_ACTION"
        private const val SPLIT_DIFFERENT_ACTIVITY_NAME: String =
            "android.platform.helpers.notesrole.AppClipsScreenshotActivity.Split"

        fun getIntent(context: Context): Intent =
            Intent(context, AppClipsScreenshotActivity::class.java).addFlags(FLAG_ACTIVITY_NEW_TASK)
    }
}
