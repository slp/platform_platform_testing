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

package android.platform.test.flag.junit;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/** @hide */
public class CustomFeatureFlags implements FeatureFlags {

    private BiPredicate<String, Predicate<FeatureFlags>> mGetValueImpl;

    public CustomFeatureFlags(BiPredicate<String, Predicate<FeatureFlags>> getValueImpl) {
        mGetValueImpl = getValueImpl;
    }

    @Override
    public boolean flagName3() {
        return getValue(Flags.FLAG_FLAG_NAME3, FeatureFlags::flagName3);
    }

    @Override
    public boolean flagName4() {
        return getValue(Flags.FLAG_FLAG_NAME4, FeatureFlags::flagName4);
    }

    @Override
    public boolean roEnabled() {
        return getValue(Flags.FLAG_RO_ENABLED, FeatureFlags::roEnabled);
    }

    @Override
    public boolean roDisabled() {
        return getValue(Flags.FLAG_RO_DISABLED, FeatureFlags::roDisabled);
    }

    protected boolean getValue(String flagName, Predicate<FeatureFlags> getter) {
        return mGetValueImpl.test(flagName, getter);
    }

    public List<String> getFlagNames() {
        return Arrays.asList(
                Flags.FLAG_FLAG_NAME3,
                Flags.FLAG_FLAG_NAME4,
                Flags.FLAG_RO_ENABLED,
                Flags.FLAG_RO_DISABLED,
                Flags.FLAG_FLAG_FINALIZED);
    }

    public boolean isFlagReadOnlyOptimized(String flagName) {
        return mReadOnlyFlagSet.contains(flagName);
    }

    private Set<String> mReadOnlyFlagSet =
            new HashSet<>(Arrays.asList(Flags.FLAG_RO_ENABLED, Flags.FLAG_RO_DISABLED, ""));

    private Map<String, Integer> mFinalizedFlags =
            new HashMap<>(
                    Map.ofEntries(
                            Map.entry(Flags.FLAG_FLAG_FINALIZED, 36),
                            Map.entry("", Integer.MAX_VALUE)));

    public boolean isFlagFinalized(String flagName) {
        if (!mFinalizedFlags.containsKey(flagName)) {
            return false;
        }
        return 99 >= mFinalizedFlags.get(flagName);
    }
}
