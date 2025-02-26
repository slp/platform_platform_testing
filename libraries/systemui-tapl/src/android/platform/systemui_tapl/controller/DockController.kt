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

package android.platform.systemui_tapl.controller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.platform.uiautomator_helpers.DeviceHelpers.context
import android.platform.uiautomator_helpers.DeviceHelpers.shell
import android.platform.uiautomator_helpers.WaitUtils.ensureThat
import android.util.Log
import java.time.Duration
import org.junit.rules.ExternalResource

/** Controller for manipulating dock status of a tablet. */
class DockController : ExternalResource() {
    private var lastDockState = UNKNOWN_DOCK_STATE

    private val dockChangedReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val newDockState = intent.getIntExtra(Intent.EXTRA_DOCK_STATE, UNKNOWN_DOCK_STATE)
                Log.i(
                    TAG,
                    "ACTION_DOCK_EVENT received, " +
                        "lastDockState: $lastDockState (${DOCK_STATE_NAMES[lastDockState]}), " +
                        "newDockState: $newDockState (${DOCK_STATE_NAMES[newDockState]})",
                )
                lastDockState = newDockState
            }
        }

    override fun before() {
        registerReceiver()
    }

    override fun after() {
        unregisterReceiver()
    }

    fun enterDockState() {
        if (DEBUG) Log.i(TAG, "enterDockState")
        // Fake Korlan Dock. See go/dock-manager-guide#fake-korlan-dock
        shell("dumpsys DockObserver set state ${Intent.EXTRA_DOCK_STATE_HE_DESK}")
        context.sendBroadcast(
            Intent("ACTION_DEBUG_DOCK")
                .putExtra("EXTRA_DEBUG_DOCK_PRODUCT", "korlan")
                .putExtra("EXTRA_DEBUG_DOCK_VERSION", "9.99.999999")
                .putExtra("EXTRA_DEBUG_DOCK_HGS_ID", "fake-hgs-id")
                .setClassName(context, DOCK_MANAGER_DEBUG_CONTROLLER_BROADCAST_RECEIVER)
        )
        waitUntilDocked()
    }

    fun exitDockState() {
        if (DEBUG) Log.i(TAG, "exitDockState")
        shell("dumpsys DockObserver set state ${Intent.EXTRA_DOCK_STATE_UNDOCKED}")
        context.sendBroadcast(
            Intent("ACTION_DEBUG_UNDOCK")
                .setClassName(context, DOCK_MANAGER_DEBUG_CONTROLLER_BROADCAST_RECEIVER)
        )
        waitUntilUndocked()
    }

    fun resetDockState() {
        if (DEBUG) Log.i(TAG, "resetDockState")
        shell("dumpsys DockObserver reset")
        context.sendBroadcast(
            Intent("ACTION_DEBUG_UNDOCK")
                .setClassName(context, DOCK_MANAGER_DEBUG_CONTROLLER_BROADCAST_RECEIVER)
        )
        waitUntilUndocked()
    }

    private fun isDocked(): Boolean {
        val docked = lastDockState in DOCKED_STATES
        if (DEBUG) Log.i(TAG, "isDocked: $docked")
        return docked
    }

    fun waitUntilDocked() {
        if (DEBUG) Log.i(TAG, "waitUntilDocked")
        ensureThat("device is docked", DEFAULT_DEADLINE) { isDocked() }
    }

    fun waitUntilUndocked() {
        if (DEBUG) Log.i(TAG, "waitUntilUndocked")
        ensureThat("device is undocked", DEFAULT_DEADLINE) { !isDocked() }
    }

    private fun registerReceiver() {
        val intentFilter = IntentFilter(Intent.ACTION_DOCK_EVENT)
        if (DEBUG) Log.i(TAG, "Registered event receiver")
        context.registerReceiver(dockChangedReceiver, intentFilter)
    }

    private fun unregisterReceiver() {
        if (DEBUG) Log.i(TAG, "Unregistered event receiver")
        context.unregisterReceiver(dockChangedReceiver)
    }

    companion object {
        private val TAG = DockController::class.java.simpleName
        private const val DOCK_MANAGER_DEBUG_CONTROLLER_BROADCAST_RECEIVER =
            "com.google.android.apps.nest.dockmanager.app/.service.DebugControllerBroadcastReceiver"
        private const val UNKNOWN_DOCK_STATE = -1
        private const val DEBUG = false
        private val DOCK_STATE_NAMES =
            mapOf(
                UNKNOWN_DOCK_STATE to "UNKNOWN_DOCK_STATE",
                Intent.EXTRA_DOCK_STATE_CAR to "EXTRA_DOCK_STATE_CAR",
                Intent.EXTRA_DOCK_STATE_DESK to "EXTRA_DOCK_STATE_DESK",
                Intent.EXTRA_DOCK_STATE_HE_DESK to "EXTRA_DOCK_STATE_HE_DESK",
                Intent.EXTRA_DOCK_STATE_LE_DESK to "EXTRA_DOCK_STATE_LE_DESK",
                Intent.EXTRA_DOCK_STATE_UNDOCKED to "EXTRA_DOCK_STATE_UNDOCKED",
            )
        private val DOCKED_STATES =
            arrayOf(
                Intent.EXTRA_DOCK_STATE_HE_DESK,
                Intent.EXTRA_DOCK_STATE_LE_DESK,
                Intent.EXTRA_DOCK_STATE_DESK,
            )
        private val DEFAULT_DEADLINE = Duration.ofSeconds(20)
    }
}
