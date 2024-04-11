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

import org.junit.runner.Description

/**
 * Rule that allows to control whether the avalanche visual suppression setting is enabled.
 * Also restores the setting to its original value at the end of the test.
 */

class AvalancheVisualSuppressionRule : TestWatcher() {

    override fun starting(description: Description?) {
        super.starting(description)
        executeShellCommand("settings put system notification_cooldown_enabled 0")
    }

    override fun finished(description: Description?) {
        super.finished(description)
        executeShellCommand("settings reset system notification_cooldown_enabled")
    }
}