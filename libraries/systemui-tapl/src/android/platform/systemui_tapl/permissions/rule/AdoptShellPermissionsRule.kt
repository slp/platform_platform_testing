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

package android.platform.systemui_tapl.permissions.rule

import android.app.UiAutomation
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Adopts shell [permissions] identity for the [uiAutomation].
 *
 * @see
 *   cts/common/device-side/util-axt/src/com/android/compatibility/common/util/AdoptShellPermissionsRule.java
 * @see UiAutomation.adoptShellPermissionIdentity
 */
class AdoptShellPermissionsRule(
    vararg permissions: String,
    private val uiAutomation: UiAutomation =
        InstrumentationRegistry.getInstrumentation().uiAutomation,
) : TestWatcher() {

    private val permissionSet: Set<String> = permissions.toSet()

    init {
        require(permissionSet.isNotEmpty())
    }

    override fun starting(description: Description?) {
        super.starting(description)
        for (permission in permissionSet) {
            uiAutomation.adoptShellPermissionIdentity(permission)
        }
        Log.d("AdoptShellPermissionsRule", "Adopted identities=${permissionSet}")
    }

    override fun finished(description: Description?) {
        uiAutomation.dropShellPermissionIdentity()
        Log.d("AdoptShellPermissionsRule", "Dropped adopted identities")
        super.finished(description)
    }
}
