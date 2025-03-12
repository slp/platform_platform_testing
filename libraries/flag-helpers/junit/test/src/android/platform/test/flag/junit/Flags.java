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

package android.platform.test.flag.junit;

/** A Fake Flags to test the {@code AconfigFlagsValueProvider}. */
public class Flags {

    public static final String FLAG_FLAG_NAME3 = "android.platform.test.flag.junit.flag_name3";
    public static final String FLAG_FLAG_NAME4 = "android.platform.test.flag.junit.flag_name4";
    public static final String FLAG_RO_ENABLED = "android.platform.test.flag.junit.ro_enabled";
    public static final String FLAG_RO_DISABLED = "android.platform.test.flag.junit.ro_disabled";
    public static final String FLAG_FLAG_FINALIZED =
            "android.platform.test.flag.junit.flag_finalized";

    /** Returns the flag value. */
    public static boolean flagName3() {
        return FEATURE_FLAGS.flagName3();
    }

    /** Another flag. */
    public static boolean flagName4() {
        return FEATURE_FLAGS.flagName4();
    }

    public static boolean roEnabled() {
        return true;
    }

    public static boolean roDisabled() {
        return false;
    }

    public static void setFeatureFlags(FeatureFlags featureFlagsImpl) {
        FEATURE_FLAGS = featureFlagsImpl;
    }

    private static FeatureFlags FEATURE_FLAGS = new FeatureFlagsImpl();
}
