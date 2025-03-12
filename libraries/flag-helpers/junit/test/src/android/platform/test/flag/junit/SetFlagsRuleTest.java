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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.platform.test.flag.util.FlagSetException;

import org.junit.AssumptionViolatedException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/** Unit tests for {@code SetFlagsRule}. */
@RunWith(Parameterized.class)
public final class SetFlagsRuleTest {

    @Rule public final SetFlagsRule mSetFlagsRule;

    @Parameterized.Parameters(name = "isInitWithDefault={0}")
    public static Object[] data() {
        return new Boolean[] {false, true};
    }

    private boolean mIsInitWithDefault;

    public SetFlagsRuleTest(boolean isInitWithDefault) {
        this.mIsInitWithDefault = isInitWithDefault;
        mSetFlagsRule =
                new SetFlagsRule(
                        isInitWithDefault
                                ? SetFlagsRule.DefaultInitValueType.DEVICE_DEFAULT
                                : SetFlagsRule.DefaultInitValueType.NULL_DEFAULT);
    }

    @Test
    public void setFlagValues() {
        mSetFlagsRule.enableFlags(Flags.FLAG_FLAG_NAME3);
        mSetFlagsRule.disableFlags(Flags.FLAG_FLAG_NAME4);
        assertTrue(Flags.flagName3());
        assertFalse(Flags.flagName4());
    }

    @Test
    public void setFlagValuesMultipleTimes() {
        mSetFlagsRule.enableFlags(Flags.FLAG_FLAG_NAME3);
        assertTrue(Flags.flagName3());

        mSetFlagsRule.disableFlags("android.platform.test.flag.junit.flag_name3");
        assertFalse(Flags.flagName3());

        mSetFlagsRule.enableFlags("android.platform.test.flag.junit.flag_name3");
        assertTrue(Flags.flagName3());

        mSetFlagsRule.disableFlags("android.platform.test.flag.junit.flag_name3");
        assertFalse(Flags.flagName3());
    }

    @Test
    public void flagsShouldResetAfterOneTest() {
        assertFalse(Flags.flagName3());
        assertTrue(Flags.flagName4());
    }

    @Test
    public void setFlagsAfterOneTest() {
        mSetFlagsRule.enableFlags(Flags.FLAG_FLAG_NAME3, Flags.FLAG_FLAG_NAME4);
        assertTrue(Flags.flagName3());
        assertTrue(Flags.flagName4());

        mSetFlagsRule.disableFlags(Flags.FLAG_FLAG_NAME3, Flags.FLAG_FLAG_NAME4);
        assertFalse(Flags.flagName3());
        assertFalse(Flags.flagName4());
    }

    @Test
    public void invalidFlagName_throwException() {
        assertThrows(
                FlagSetException.class,
                () -> {
                    mSetFlagsRule.enableFlags("flagName3");
                });
        assertThrows(
                FlagSetException.class,
                () -> {
                    mSetFlagsRule.enableFlags("com.fake.package.flag_name3");
                });
    }

    @Test
    public void skipReadOnlyOptimizedFlag() {
        assertThrows(
                AssumptionViolatedException.class,
                () -> {
                    mSetFlagsRule.enableFlags(Flags.FLAG_RO_ENABLED);
                });
        assertThrows(
                AssumptionViolatedException.class,
                () -> {
                    mSetFlagsRule.disableFlags(Flags.FLAG_RO_ENABLED);
                });
        assertThrows(
                AssumptionViolatedException.class,
                () -> {
                    mSetFlagsRule.enableFlags(Flags.FLAG_RO_DISABLED);
                });
        assertThrows(
                AssumptionViolatedException.class,
                () -> {
                    mSetFlagsRule.disableFlags(Flags.FLAG_RO_DISABLED);
                });
    }

    @Test
    public void skipFinalizedFlag() {
        assertThrows(
                AssumptionViolatedException.class,
                () -> {
                    mSetFlagsRule.disableFlags(Flags.FLAG_FLAG_FINALIZED);
                });
    }
}
