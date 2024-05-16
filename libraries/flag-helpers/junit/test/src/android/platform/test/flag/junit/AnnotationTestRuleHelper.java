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

import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;

import com.google.auto.value.AutoAnnotation;

import org.junit.Assert;
import org.junit.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

class AnnotationTestRuleHelper {
    private Class<?> mTestClass = null;
    private TestRule mClassRule = null;
    private TestCode mPreTestCode = null;
    private TestRule mTestRule = null;
    private TestCode mTestCode = null;
    private List<Annotation> mMethodAnnotations = new ArrayList<>();

    AnnotationTestRuleHelper(TestRule testRule) {
        mTestRule = testRule;
    }

    /** Set a ClassRule to invoke before the main TestRule */
    AnnotationTestRuleHelper setClassRule(TestRule classRule) {
        mClassRule = classRule;
        return this;
    }

    /** set test class with annotations for the example test */
    AnnotationTestRuleHelper setTestClass(Class<?> testClass) {
        mTestClass = testClass;
        return this;
    }

    /** Add {@code @RequiresFlagsEnabled} with the given arguments to the test method. */
    AnnotationTestRuleHelper addRequiresFlagsEnabled(String... values) {
        mMethodAnnotations.add(createRequiresFlagsEnabled(values));
        return this;
    }

    /** Add {@code @RequiresFlagsDisabled} with the given arguments to the test method. */
    AnnotationTestRuleHelper addRequiresFlagsDisabled(String... values) {
        mMethodAnnotations.add(createRequiresFlagsDisabled(values));
        return this;
    }

    /** Add {@code @EnableFlags} with the given arguments to the test method. */
    AnnotationTestRuleHelper addEnableFlags(String... values) {
        mMethodAnnotations.add(createEnableFlags(values));
        return this;
    }

    /** Add {@code @DisableFlags} with the given arguments to the test method. */
    AnnotationTestRuleHelper addDisableFlags(String... values) {
        mMethodAnnotations.add(createDisableFlags(values));
        return this;
    }

    /** produce a Description for the test class */
    private Description buildClassDescription() {
        if (mTestClass == null) {
            return Description.createSuiteDescription("testClass");
        } else {
            return Description.createSuiteDescription(mTestClass);
        }
    }

    /** produce a Description for the test method */
    private Description buildTestDescription() {
        Annotation[] methodAnnotations = mMethodAnnotations.toArray(new Annotation[0]);
        if (mTestClass == null) {
            return Description.createTestDescription("testClass", "testMethod", methodAnnotations);
        } else {
            return Description.createTestDescription(mTestClass, "testMethod", methodAnnotations);
        }
    }

    /**
     * set the test code that executes within the ClassRule but before the TestRule. This
     * approximates the time period where test class initialization.
     */
    AnnotationTestRuleHelper setPreTestCode(TestCode preTestCode) {
        mPreTestCode = preTestCode;
        return this;
    }

    /** set the test code that executes within the TestRule */
    AnnotationTestRuleHelper setTestCode(TestCode testCode) {
        mTestCode = testCode;
        return this;
    }

    PreparedTest prepareTest() {
        // Wrap the test code in a Statement
        final Statement testCodeStatement =
                new Statement() {
                    public void evaluate() throws Throwable {
                        if (mTestCode != null) {
                            mTestCode.evaluate();
                        }
                    }
                };

        // Wrap the test code with the TestRule
        Description testDescription = buildTestDescription();
        final Statement testRuleStatement = mTestRule.apply(testCodeStatement, testDescription);

        // Wrap to prefix the entire test with the preTestCode
        final Statement preTestStatement;
        if (mPreTestCode == null) {
            preTestStatement = testRuleStatement;
        } else {
            preTestStatement =
                    new Statement() {
                        public void evaluate() throws Throwable {
                            mPreTestCode.evaluate();
                            testRuleStatement.evaluate();
                        }
                    };
        }

        // Wrap the entire test with the ClassRule
        final Statement classRuleStatement;
        if (mClassRule == null) {
            classRuleStatement = preTestStatement;
        } else {
            Description classDescription = buildClassDescription();
            classDescription.addChild(testDescription);
            classRuleStatement = mClassRule.apply(preTestStatement, classDescription);
        }

        // Wrap the statement in a PreparedTest which can assert on the result of running the test
        return new PreparedTest(classRuleStatement);
    }

    static class PreparedTest {
        private final Statement mStatement;

        private PreparedTest(Statement statement) {
            mStatement = statement;
        }

        Throwable runAndReturnFailure() {
            try {
                mStatement.evaluate();
                return null;
            } catch (Throwable throwable) {
                return throwable;
            }
        }

        void assertFails() {
            assertFailsWithTypeAndMessage(AssertionError.class, null);
        }

        void assertFailsWithMessage(String expectedMessage) {
            assertFailsWithTypeAndMessage(AssertionError.class, expectedMessage);
        }

        void assertFailsWithType(Class<? extends Throwable> expectedError) {
            assertFailsWithTypeAndMessage(expectedError, null);
        }

        void assertFailsWithTypeAndMessage(
                Class<? extends Throwable> expectedError, String expectedMessage) {
            Throwable failure = runAndReturnFailure();
            Assert.assertNotNull("Test was expected to fail", failure);
            if (failure.getClass() != expectedError) {
                throw new AssertionError(
                        "Wrong error type; expected " + expectedError + " but was: " + failure,
                        failure);
            }
            if (expectedMessage != null) {
                String failureMessage = failure.getMessage();
                if (failureMessage == null || !failureMessage.contains(expectedMessage)) {
                    throw new AssertionError(
                            "Failure message should contain \""
                                    + expectedMessage
                                    + "\" but was: "
                                    + failureMessage,
                            failure);
                }
            }
        }

        void assertSkipped() {
            assertSkippedWithMessage(null);
        }

        void assertSkippedWithMessage(String expectedMessage) {
            Throwable failure = runAndReturnFailure();
            Assert.assertNotNull("Test was expected to be skipped but it ran and passed", failure);
            if (failure.getClass() != AssumptionViolatedException.class) {
                throw new AssertionError(
                        "Test was expected to be skipped but it ran and failed: " + failure,
                        failure);
            }
            if (expectedMessage != null) {
                String skippedMessage = failure.getMessage();
                if (skippedMessage == null || !skippedMessage.contains(expectedMessage)) {
                    throw new AssertionError(
                            "Test skip message should contain \""
                                    + expectedMessage
                                    + "\" but was: "
                                    + skippedMessage,
                            failure);
                }
            }
        }

        void assertPasses() {
            try {
                mStatement.evaluate();
            } catch (Throwable failure) {
                throw new AssertionError(
                        "Test was expected to pass, but failed with error: " + failure, failure);
            }
        }
    }

    @AutoAnnotation
    private static RequiresFlagsEnabled createRequiresFlagsEnabled(String[] value) {
        return new AutoAnnotation_AnnotationTestRuleHelper_createRequiresFlagsEnabled(value);
    }

    @AutoAnnotation
    private static RequiresFlagsDisabled createRequiresFlagsDisabled(String[] value) {
        return new AutoAnnotation_AnnotationTestRuleHelper_createRequiresFlagsDisabled(value);
    }

    @AutoAnnotation
    private static EnableFlags createEnableFlags(String[] value) {
        return new AutoAnnotation_AnnotationTestRuleHelper_createEnableFlags(value);
    }

    @AutoAnnotation
    private static DisableFlags createDisableFlags(String[] value) {
        return new AutoAnnotation_AnnotationTestRuleHelper_createDisableFlags(value);
    }

    /**
     * A variant of Junit's Statement class that is an interface, so that it can be implemented with
     * a lambda
     */
    public interface TestCode {
        void evaluate() throws Throwable;
    }
}
