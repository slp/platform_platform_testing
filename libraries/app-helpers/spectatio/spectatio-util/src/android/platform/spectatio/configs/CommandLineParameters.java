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

package android.platform.spectatio.configs;

import android.os.Bundle;

import androidx.test.platform.app.InstrumentationRegistry;

/**
 * Simple wrapper around {@link InstrumentationRegistry#getArguments()}. Since that method creates a
 * new Bundle instance each call, this class caches that Bundle.
 */
public class CommandLineParameters {
    private static final CommandLineParameters INSTANCE = new CommandLineParameters();

    private Bundle mArguments = InstrumentationRegistry.getArguments();

    /**
     * Retrieve a `-e {key} {value}` value from the instrumentation command
     *
     * @param key The key given to `-e`
     * @param defaultValue Value to return if this parameter wasn't specified
     * @return The value given to `-e`, or defaultValue if the key wasn't specified
     */
    public static String getValue(String key, String defaultValue) {
        return INSTANCE.mArguments.getString(key, defaultValue);
    }
}
