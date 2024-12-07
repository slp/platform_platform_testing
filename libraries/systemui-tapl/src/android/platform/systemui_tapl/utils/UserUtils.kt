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

package android.platform.systemui_tapl.utils

import android.app.ActivityManager
import android.app.IUserSwitchObserver
import android.app.UserSwitchObserver
import android.platform.helpers.CommonUtils.println
import android.platform.uiautomator_helpers.DeviceHelpers.assertInvisible
import android.platform.uiautomator_helpers.TracingUtils.trace
import android.util.Log
import androidx.test.uiautomator.By
import androidx.test.uiautomator.StaleObjectException
import com.google.common.truth.Truth.assertWithMessage
import java.time.Duration
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/** DON'T USE FROM TESTS: TAPL user utilities to be used from ui and controller objects */
object UserUtils {
    private val TAG = UserUtils::class.java.simpleName
    private val UI_RESPONSE_USER_SWITCH_COMPLETE_TIMEOUT = Duration.ofMinutes(1)
    private val EVENT_USER_SWITCH_COMPLETE_TIMEOUT = Duration.ofMinutes(1)
    private const val DEBUG = false

    private val mActivityManager = ActivityManager.getService()

    /**
     * String displayed for the "Add guest" item in quick settings. The word "Guest" is shown in
     * quick settings regardless of whether there is already a guest on the device.
     */
    private val sUserSwitchingSelector = By.textStartsWith("Switching to ")
    private val sUserSwitchedSemaphore = Semaphore(0 /* permits */)
    private val sUserSwitchedObserver: IUserSwitchObserver =
        object : UserSwitchObserver() {
            override fun onUserSwitchComplete(newUserId: Int) {
                Log.i(TAG, "userSwitchComplete for $newUserId")
                sUserSwitchedSemaphore.release()
            }
        }

    init {
        if (DEBUG) println("$TAG#registerUserSwitchedObserver")
        mActivityManager.registerUserSwitchObserver(sUserSwitchedObserver, TAG)
    }

    private fun runThenWaitForUserSwitchCompleteEvent(switchUser: Runnable) {
        trace("switchUserAndWaitForUserSwitchedEvent") {
            if (DEBUG) println("$TAG#switchUserAndWaitForUserSwitchedEvent")
            sUserSwitchedSemaphore.drainPermits()
            switchUser.run()
            try {
                assertWithMessage("User switched event wasn't received")
                    .that(
                        sUserSwitchedSemaphore.tryAcquire(
                            /* permits */ 1,
                            EVENT_USER_SWITCH_COMPLETE_TIMEOUT.toSeconds(),
                            TimeUnit.SECONDS,
                        )
                    )
                    .isTrue()
            } catch (e: InterruptedException) {
                throw AssertionError("Interrupted while verifying user switched", e)
            }
        }
    }

    private fun waitUntilSwitchingUserDialogIsGone() {
        trace("waitUntilSwitchingUserDialogIsGone") {
            if (DEBUG) println("$TAG#waitUntilSwitchingUserDialogIsGone")
            try {
                sUserSwitchingSelector.assertInvisible(UI_RESPONSE_USER_SWITCH_COMPLETE_TIMEOUT) {
                    "Switching user dialog is not gone"
                }
            } catch (e: StaleObjectException) {
                // ignore
            }
        }
    }

    @JvmStatic
    fun runThenWaitUntilSwitchCompleted(switchUser: Runnable) {
        trace("switchUserAndWaitUntilSwitchCompleted") {
            if (DEBUG) println("$TAG#switchUserAndWaitUntilSwitchCompleted")
            runThenWaitForUserSwitchCompleteEvent(switchUser)
            waitUntilSwitchingUserDialogIsGone()
            // There's an incredible amount of jank, etc. after switching users. Wait a long
            // time.
            trace("wait a long time for jank to disappear") {
                try {
                    TimeUnit.SECONDS.sleep(EVENT_USER_SWITCH_COMPLETE_TIMEOUT.toSeconds())
                } catch (ignored: InterruptedException) {}
            }
        }
    }
}
