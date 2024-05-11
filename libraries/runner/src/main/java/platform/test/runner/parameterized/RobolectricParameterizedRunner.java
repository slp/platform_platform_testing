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

package platform.test.runner.parameterized;

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.TestClass;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.internal.SandboxTestRunner;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;

import java.util.List;

/**
 * Parameterized runner for vanilla (non-RSL) Robolectric tests.
 *
 * @see org.robolectric.ParameterizedRobolectricTestRunner
 */
public class RobolectricParameterizedRunner extends RobolectricTestRunner {

    private final int mParametersIndex;
    private final String mName;

    public RobolectricParameterizedRunner(Class<?> type, int parametersIndex, String name)
            throws InitializationError {
        super(type);
        mParametersIndex = parametersIndex;
        mName = name;
    }

    @Override
    protected String getName() {
        return mName;
    }

    @Override
    protected String testName(final FrameworkMethod method) {
        return method.getName() + getName();
    }

    @Override
    protected void validateConstructor(List<Throwable> errors) {
        validateOnlyOneConstructor(errors);
        if (ParameterizedRunnerDelegate.fieldsAreAnnotated(getTestClass())) {
            validateZeroArgConstructor(errors);
        }
    }

    @Override
    public String toString() {
        return "RobolectricParameterizedRunner " + getName();
    }

    @Override
    protected void validateFields(List<Throwable> errors) {
        super.validateFields(errors);
        ParameterizedRunnerDelegate.validateFields(errors, getTestClass());
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected SandboxTestRunner.HelperTestRunner getHelperTestRunner(Class bootstrappedTestClass) {
        try {
            return new HelperTestRunner(bootstrappedTestClass) {
                @Override
                protected void validateConstructor(List<Throwable> errors) {
                    RobolectricParameterizedRunner.this.validateOnlyOneConstructor(errors);
                }

                @Override
                protected Object createTest() throws Exception {
                    // The test object needs to be created in a different class loader
                    // than the runner
                    return ReflectionHelpers.callStaticMethod(
                            getTestClass().getJavaClass().getClassLoader(),
                            RobolectricParameterizedRunner.class.getName(),
                            "createTestInstance",
                            ClassParameter.from(TestClass.class, getTestClass()),
                            ClassParameter.from(Integer.TYPE, mParametersIndex),
                            ClassParameter.from(String.class, mName));
                }

                @Override
                public String toString() {
                    return "HelperTestRunner for " + RobolectricParameterizedRunner.this;
                }
            };
        } catch (InitializationError initializationError) {
            throw new RuntimeException(initializationError);
        }
    }

    /**
     * Utility method called using reflection so that the test can be created in a different
     * classLoader
     */
    public static Object createTestInstance(TestClass testClass, int parametersIndex, String name)
            throws Exception {
        return new ParameterizedRunnerDelegate(parametersIndex, name).createTestInstance(testClass);
    }
}
