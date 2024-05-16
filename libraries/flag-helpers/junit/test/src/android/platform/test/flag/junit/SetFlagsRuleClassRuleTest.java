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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.platform.test.annotations.EnableFlags;
import android.platform.test.annotations.UsesFlags;
import android.platform.test.flag.util.FlagSetException;

import java.util.Map;

import org.junit.AssumptionViolatedException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@code SetFlagsRule} being used with annotations. */
@RunWith(JUnit4.class)
public final class SetFlagsRuleClassRuleTest extends SetFlagsRuleTestCommon {

    @Override
    protected Helper makeNullDefaultHelper() {
        throw new AssumptionViolatedException("Null default not supported for class rules");
    }

    @Override
    protected Helper makeParameterizedHelper(FlagsParameterization params) {
        return makeClassRuleHelper(params, /* watchFlags= */ true);
    }

    private Helper makeClassRuleHelper(FlagsParameterization params, boolean watchFlags) {
        SetFlagsRule.ClassRule classRule =
                watchFlags ? new SetFlagsRule.ClassRule(Flags.class) : new SetFlagsRule.ClassRule();
        SetFlagsRule rule = classRule.createSetFlagsRule(params);
        Helper helper = new Helper(rule);
        helper.setClassRule(classRule);
        return helper;
    }

    @Test
    public void setFlagsFailsIfNotWatched() {
        Helper helper = makeClassRuleHelper(/* params= */ null, /* watchFlags= */ false);
        SetFlagsRule setFlagsRule = helper.getSetFlagsRule();
        helper.setTestCode(
                        () -> {
                            setFlagsRule.enableFlags(Flags.FLAG_FLAG_NAME3);
                        })
                .prepareTest()
                .assertFailsWithType(FlagSetException.class);
    }

    @Test
    public void setFlagsCanBeMarkedAsWatchedWithUsesFlags() {
        @UsesFlags(Flags.class)
        class TestClass {}
        Helper helper = makeClassRuleHelper(/* params= */ null, /* watchFlags= */ false);
        SetFlagsRule setFlagsRule = helper.getSetFlagsRule();
        helper.setTestClass(TestClass.class)
                .setTestCode(
                        () -> {
                            setFlagsRule.enableFlags(Flags.FLAG_FLAG_NAME3);
                            assertTrue(Flags.flagName3());
                        })
                .prepareTest()
                .assertPasses();
    }

    @Test
    public void methodAnnotatedFlagsDoNotNeedToBeWatched() {
        makeClassRuleHelper(/* params= */ null, /* watchFlags= */ false)
                .addEnableFlags(Flags.FLAG_FLAG_NAME3)
                .setTestCode(
                        () -> {
                            assertTrue(Flags.flagName3());
                        })
                .prepareTest()
                .assertPasses();
    }

    @Test
    public void classAnnotatedFlagsDoNotNeedToBeWatched() {
        @EnableFlags(Flags.FLAG_FLAG_NAME3)
        class TestClass {}
        makeClassRuleHelper(/* params= */ null, /* watchFlags= */ false)
                .setTestClass(TestClass.class)
                .setTestCode(
                        () -> {
                            assertTrue(Flags.flagName3());
                        })
                .prepareTest()
                .assertPasses();
    }

    @Test
    public void parameterizedFlagsMustBeWatched() {
        FlagsParameterization params =
                new FlagsParameterization(Map.of(Flags.FLAG_FLAG_NAME3, true));
        makeClassRuleHelper(params, /* watchFlags= */ false)
                .prepareTest()
                .assertFailsWithType(FlagSetException.class);
    }

    @Test
    public void parameterizedFlagsCanBeMarkedAsWatchedWithUsesFlags() {
        @UsesFlags(Flags.class)
        class TestClass {}
        FlagsParameterization params =
                new FlagsParameterization(Map.of(Flags.FLAG_FLAG_NAME3, true));
        makeClassRuleHelper(params, /* watchFlags= */ false)
                .setTestClass(TestClass.class)
                .setTestCode(
                        () -> {
                            assertTrue(Flags.flagName3());
                        })
                .prepareTest()
                .assertPasses();
    }

    @Test
    public void setFlagsCannotBeReadBeforeSet() {
        Helper helper = makeDeviceDefaultHelper();
        SetFlagsRule setFlagsRule = helper.getSetFlagsRule();
        helper.setTestCode(
                        () -> {
                            assertFalse(Flags.flagName3());
                            setFlagsRule.enableFlags(Flags.FLAG_FLAG_NAME3);
                        })
                .prepareTest()
                .assertFailsWithType(FlagSetException.class);
    }

    @Test
    public void nonSetFlagsCanBeReadBeforeSet() {
        Helper helper = makeDeviceDefaultHelper();
        SetFlagsRule setFlagsRule = helper.getSetFlagsRule();
        helper.setTestCode(
                        () -> {
                            assertTrue(Flags.flagName4());
                            setFlagsRule.enableFlags(Flags.FLAG_FLAG_NAME3);
                            assertTrue(Flags.flagName3());
                        })
                .prepareTest()
                .assertPasses();
    }

    @Test
    public void setFlagsCannotBeReadBeforeTest() {
        Helper helper = makeDeviceDefaultHelper();
        SetFlagsRule setFlagsRule = helper.getSetFlagsRule();
        helper.setPreTestCode(
                        () -> {
                            assertFalse(Flags.flagName3());
                        })
                .setTestCode(
                        () -> {
                            setFlagsRule.enableFlags(Flags.FLAG_FLAG_NAME3);
                        })
                .prepareTest()
                .assertFailsWithType(FlagSetException.class);
    }

    @Test
    public void nonSetFlagsCanBeReadBeforeTest() {
        Helper helper = makeDeviceDefaultHelper();
        SetFlagsRule setFlagsRule = helper.getSetFlagsRule();
        helper.setPreTestCode(
                        () -> {
                            assertTrue(Flags.flagName4());
                        })
                .setTestCode(
                        () -> {
                            setFlagsRule.enableFlags(Flags.FLAG_FLAG_NAME3);
                            assertTrue(Flags.flagName3());
                        })
                .prepareTest()
                .assertPasses();
    }

    @Test
    public void methodAnnotatedFlagsCannotBeReadBeforeTest() {
        makeDeviceDefaultHelper()
                .addEnableFlags(Flags.FLAG_FLAG_NAME3)
                .setPreTestCode(
                        () -> {
                            assertFalse(Flags.flagName3());
                        })
                .prepareTest()
                .assertFailsWithType(FlagSetException.class);
    }

    @Test
    public void nonMethodAnnotedFlagsCanBeReadBeforeTest() {
        makeDeviceDefaultHelper()
                .addEnableFlags(Flags.FLAG_FLAG_NAME3)
                .setPreTestCode(
                        () -> {
                            assertTrue(Flags.flagName4());
                        })
                .setTestCode(
                        () -> {
                            assertTrue(Flags.flagName3());
                        })
                .prepareTest()
                .assertPasses();
    }

    @Test
    public void classAnnotatedFlagsCanBeReadBeforeTest() {
        @EnableFlags(Flags.FLAG_FLAG_NAME3)
        class TestClass {}
        makeDeviceDefaultHelper()
                .setTestClass(TestClass.class)
                .setPreTestCode(
                        () -> {
                            assertTrue(Flags.flagName4());
                        })
                .setTestCode(
                        () -> {
                            assertTrue(Flags.flagName3());
                        })
                .prepareTest()
                .assertPasses();
    }

    @Test
    public void nonClassAnnotedFlagsCanBeReadBeforeTest() {
        @EnableFlags(Flags.FLAG_FLAG_NAME3)
        class TestClass {}
        makeDeviceDefaultHelper()
                .setTestClass(TestClass.class)
                .setPreTestCode(
                        () -> {
                            assertTrue(Flags.flagName4());
                        })
                .setTestCode(
                        () -> {
                            assertTrue(Flags.flagName3());
                        })
                .prepareTest()
                .assertPasses();
    }

    @Test
    public void parameterizedFlagsCannotBeReadBeforeTest() {
        FlagsParameterization params =
                new FlagsParameterization(Map.of(Flags.FLAG_FLAG_NAME3, true));
        makeParameterizedHelper(params)
                .setPreTestCode(
                        () -> {
                            assertFalse(Flags.flagName3());
                        })
                .prepareTest()
                .assertFailsWithType(FlagSetException.class);
    }

    @Test
    public void nonParameterizedFlagsCanBeReadBeforeTest() {
        FlagsParameterization params =
                new FlagsParameterization(Map.of(Flags.FLAG_FLAG_NAME3, true));
        makeParameterizedHelper(params)
                .setPreTestCode(
                        () -> {
                            assertTrue(Flags.flagName4());
                        })
                .setTestCode(
                        () -> {
                            assertTrue(Flags.flagName3());
                        })
                .prepareTest()
                .assertPasses();
    }

    @Test
    public void readingInsideOneTestDoesntAffectSettingInLaterTests() {
        SetFlagsRule.ClassRule classRule = new SetFlagsRule.ClassRule(Flags.class);
        new AnnotationTestRuleHelper(classRule)
                .setTestCode(
                        () -> {
                            // 1st test in the class reads a flag
                            new Helper(classRule.createSetFlagsRule())
                                    .setTestCode(
                                            () -> {
                                                Flags.flagName3();
                                            })
                                    .prepareTest()
                                    .assertPasses();
                            // 2nd test in the class sets the flag
                            new Helper(classRule.createSetFlagsRule())
                                    .addEnableFlags(Flags.FLAG_FLAG_NAME3)
                                    .prepareTest()
                                    .assertPasses();
                        })
                .prepareTest()
                .assertPasses();
    }

    @Test
    public void readingBetweenTestsFailsLaterTestsWhichSetTheFlag() {
        SetFlagsRule.ClassRule classRule = new SetFlagsRule.ClassRule(Flags.class);
        new AnnotationTestRuleHelper(classRule)
                .setTestCode(
                        () -> {
                            // 1st test in the class reads a flag, and passes
                            new Helper(classRule.createSetFlagsRule())
                                    .addEnableFlags(Flags.FLAG_FLAG_NAME3)
                                    .prepareTest()
                                    .assertPasses();

                            // Next; some code reads the flag, but not during a test.
                            // This potentially corrupts every subsequent test that sets this flag.
                            Flags.flagName3();

                            // 2nd test enables the flag, so it will fail
                            new Helper(classRule.createSetFlagsRule())
                                    .addEnableFlags(Flags.FLAG_FLAG_NAME3)
                                    .prepareTest()
                                    .assertFailsWithType(FlagSetException.class);

                            // 3rd test only sets the other flag, so it will pass
                            new Helper(classRule.createSetFlagsRule())
                                    .addEnableFlags(Flags.FLAG_FLAG_NAME4)
                                    .prepareTest()
                                    .assertPasses();

                            // 4th test disables the flag, so it will fail
                            new Helper(classRule.createSetFlagsRule())
                                    .addDisableFlags(Flags.FLAG_FLAG_NAME3)
                                    .prepareTest()
                                    .assertFailsWithType(FlagSetException.class);
                        })
                .prepareTest()
                .assertPasses();
    }

    @Test
    public void readingFlagsDuringOneSuiteDoesntBreakLaterSuites() {
        SetFlagsRule.ClassRule classRule = new SetFlagsRule.ClassRule(Flags.class);
        // 1st suite reads a flag before tests run
        new Helper(classRule.createSetFlagsRule())
                .setClassRule(classRule)
                .addEnableFlags(Flags.FLAG_FLAG_NAME3)
                .setPreTestCode(
                        () -> {
                            Flags.flagName3();
                        })
                .prepareTest()
                .assertFailsWithType(FlagSetException.class);
        // 2nd suite still passes
        new Helper(classRule.createSetFlagsRule())
                .setClassRule(classRule)
                .addEnableFlags(Flags.FLAG_FLAG_NAME3)
                .prepareTest()
                .assertPasses();
    }
}
