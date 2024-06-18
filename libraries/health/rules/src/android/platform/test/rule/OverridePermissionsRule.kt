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

package android.platform.test.rule

import android.content.pm.PackageManager
import android.os.Process
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.runner.Description

/**
 * A rule that allows to apply overrides (revoke or grant) for Android permissions
 */
class OverridePermissionsRule : TestWatcher() {

    private val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
    private val overrides: MutableList<PermissionOverride> = arrayListOf()

    fun overridePermission(
        permission: String,
        @PackageManager.PermissionResult operation: Int,
        uid: Int = Process.myUid()
    ) {
        val override = PermissionOverride(
            uid = uid,
            permission = permission,
            operation = operation
        )
        overrides.add(override)
        applyOverride(override)
    }

    override fun finished(description: Description?) {
        overrides.forEach {
            resetOverride(it)
        }
    }

    private fun applyOverride(override: PermissionOverride) {
        uiAutomation.addOverridePermissionState(
            override.uid,
            override.permission,
            override.operation
        )
    }

    private fun resetOverride(override: PermissionOverride) {
        uiAutomation.removeOverridePermissionState(
            override.uid,
            override.permission,
        )
    }

    private data class PermissionOverride(
        val uid: Int,
        val permission: String,
        @PackageManager.PermissionResult val operation: Int
    )
}