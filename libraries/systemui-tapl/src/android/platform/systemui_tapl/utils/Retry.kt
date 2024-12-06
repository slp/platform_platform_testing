/*
 * Copyright (C) 2023 The Android Open Source Project
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

/**
 * Run the function and retry when exceptions thrown.
 *
 * @param function The function which needs retry
 * @param retry Maximum retry count, including the first run
 * @param backoff Gap between retries in millis
 */
fun runWithRetry(function: Runnable, retry: Int = 3, backoff: Long = 1_000) {
    for (i in 1..retry) {
        try {
            function.run()
            return
        } catch (e: Exception) {
            if (i == retry) {
                throw RuntimeException("Execution failed after retry $retry times", e)
            } else {
                Thread.sleep(backoff)
            }
        }
    }
}
