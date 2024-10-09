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

package android.platform.test.flag.junit.example;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.Flags;
import android.platform.test.flag.junit.FlagsParameterization;
import android.platform.test.flag.junit.SetFlagsRule;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.List;

/**
 * Example for how to write a test using {@link SetFlagsRule}, {@link FlagsParameterization} and the
 * annotations {@link EnableFlags}, {@link DisableFlags}.
 *
 * <p>NOTE: when using {@link FlagsParameterization}, the {@link SetFlagsRule.ClassRule} needs to
 * know which flags to monitor. Since we have at least one {@link EnableFlags} or {@link
 * DisableFlags} annotation that refers to the class we're setting flags on, we don't need to do
 * anything else. If we did not, we would need to add {@link UsesFlags} to tell it.
 *
 * <p>@UsesFlags(Flags.cass)
 */
@RunWith(Parameterized.class)
public class ExampleFlagsParameterizedTest {

    @Parameters(name = "{0}")
    public static List<FlagsParameterization> getFlags() {
        return FlagsParameterization.allCombinationsOf(
                Flags.FLAG_FLAG_NAME3, Flags.FLAG_FLAG_NAME4);
    }

    @ClassRule
    public static final SetFlagsRule.ClassRule mSetFlagsClassRule = new SetFlagsRule.ClassRule();

    public ExampleFlagsParameterizedTest(FlagsParameterization flags) {
        mSetFlagsRule = mSetFlagsClassRule.createSetFlagsRule(flags);
    }

    @Rule public final SetFlagsRule mSetFlagsRule;

    // unused variables are used to call out when a flag is accessible
    // but will have different values depending on the parameterization.

    @Test
    public void runTestWithAllFlagCombinations() {
        boolean unusedFlag3 = Flags.flagName3();
        boolean unusedFlag4 = Flags.flagName4();
    }

    @Test
    @EnableFlags(Flags.FLAG_FLAG_NAME3)
    public void runTestWithFlag3Enabled() {
        assertTrue(Flags.flagName3());
        boolean unusedFlag4 = Flags.flagName4();
    }

    @Test
    @DisableFlags(Flags.FLAG_FLAG_NAME4)
    public void runTestWithFlag4Disabled() {
        boolean unusedFlag3 = Flags.flagName3();
        assertFalse(Flags.flagName4());
    }

    @Test
    @EnableFlags(Flags.FLAG_FLAG_NAME3)
    @DisableFlags(Flags.FLAG_FLAG_NAME4)
    public void runTestWithFlag3EnabledAndFlag4Disabled() {
        assertTrue(Flags.flagName3());
        assertFalse(Flags.flagName4());
    }

    @Test
    @EnableFlags({Flags.FLAG_FLAG_NAME3, Flags.FLAG_FLAG_NAME4})
    public void runTestWithTwoFlagsEnabled() {
        assertTrue(Flags.flagName3());
        assertTrue(Flags.flagName4());
    }
}
