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

import static platform.test.runner.parameterized.ParameterizedAndroidJunit4.isRunningOnAndroid;

import org.junit.Assert;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/**
 * Encapsulates reflection operations needed to instantiate and parameterize a test instance for
 * tests run with {@link ParameterizedAndroidJunit4}. This logic is independent of the platform/
 * environment on which the test is running, so it's shared by all of the runners that {@link
 * ParameterizedAndroidJunit4} delegates to.
 *
 * @see org.junit.runners.Parameterized
 * @see org.robolectric.ParameterizedRobolectricTestRunner
 * @see com.google.android.testing.rsl.robolectric.junit.ParametrizedRslTestRunner
 */
class ParameterizedRunnerDelegate {

    private final int mParametersIndex;
    private final String mName;

    ParameterizedRunnerDelegate(int parametersIndex, String name) {
        this.mParametersIndex = parametersIndex;
        this.mName = name;
    }

    public String getName() {
        return mName;
    }

    public Object createTestInstance(final TestClass testClass) throws Exception {
        Class<?> bootstrappedClass = testClass.getJavaClass();
        Constructor<?>[] constructors = bootstrappedClass.getConstructors();
        Assert.assertEquals(1, constructors.length);
        if (!fieldsAreAnnotated(testClass)) {
            return constructors[0].newInstance(computeParams(testClass));
        } else {
            Object instance = constructors[0].newInstance();
            injectParametersIntoFields(instance, testClass);
            return instance;
        }
    }

    private Object[] computeParams(final TestClass testClass) throws Exception {
        // Robolectric uses a different class loader when running the tests, so the parameters
        // objects
        // created by the test runner are not compatible with the parameters required by the test.
        // Instead, we compute the parameters within the test's class loader.
        try {
            List<Object> parametersList = getParametersList(testClass);
            if (mParametersIndex >= parametersList.size()) {
                throw new Exception(
                        "Re-computing the parameter list returned a different number of parameters"
                                + " values. Is the data() method of your test non-deterministic?");
            }
            Object parametersObj = parametersList.get(mParametersIndex);
            return (parametersObj instanceof Object[])
                    ? (Object[]) parametersObj
                    : new Object[] {parametersObj};
        } catch (ClassCastException e) {
            throw new Exception(
                    String.format(
                            "%s.%s() must return a Collection of arrays.",
                            testClass.getName(), mName),
                    e);
        } catch (Exception exception) {
            throw exception;
        } catch (Throwable throwable) {
            throw new Exception(throwable);
        }
    }

    @SuppressWarnings("unchecked")
    private void injectParametersIntoFields(Object testClassInstance, final TestClass testClass)
            throws Exception {
        // Robolectric uses a different class loader when running the tests, so referencing
        // Parameter
        // directly causes type mismatches. Instead, we find its class within the test's class
        // loader.
        Object[] parameters = computeParams(testClass);
        HashSet<Integer> parameterFieldsFound = new HashSet<>();
        for (Field field : testClassInstance.getClass().getFields()) {
            Annotation parameter = field.getAnnotation(Parameter.class);
            if (parameter != null) {
                int index = (int) parameter.annotationType().getMethod("value").invoke(parameter);
                parameterFieldsFound.add(index);
                try {
                    field.set(testClassInstance, parameters[index]);
                } catch (IllegalArgumentException e) {
                    throw new Exception(
                            String.format(
                                    "%s: Trying to set %s with the value %s that is not the right"
                                            + " type (%s instead of %s).",
                                    testClass.getName(),
                                    field.getName(),
                                    parameters[index],
                                    parameters[index].getClass().getSimpleName(),
                                    field.getType().getSimpleName()),
                            e);
                }
            }
        }
        if (parameterFieldsFound.size() != parameters.length) {
            throw new IllegalStateException(
                    String.format(
                            "Provided %d parameters, but only found fields for parameters: %s",
                            parameters.length, parameterFieldsFound));
        }
    }

    static void validateFields(List<Throwable> errors, TestClass testClass) {
        // Ensure that indexes for parameters are correctly defined
        if (fieldsAreAnnotated(testClass)) {
            List<FrameworkField> annotatedFieldsByParameter =
                    getAnnotatedFieldsByParameter(testClass);
            int[] usedIndices = new int[annotatedFieldsByParameter.size()];
            for (FrameworkField each : annotatedFieldsByParameter) {
                int index = each.getField().getAnnotation(Parameter.class).value();
                if (index < 0 || index > annotatedFieldsByParameter.size() - 1) {
                    errors.add(
                            new Exception(
                                    String.format(
                                            Locale.US,
                                            "Invalid @Parameter value: %d. @Parameter fields"
                                                + " counted: %d. Please use an index between 0 and"
                                                + " %d.",
                                            index,
                                            annotatedFieldsByParameter.size(),
                                            annotatedFieldsByParameter.size() - 1)));
                } else {
                    usedIndices[index]++;
                }
            }
            for (int index = 0; index < usedIndices.length; index++) {
                int numberOfUses = usedIndices[index];
                if (numberOfUses == 0) {
                    errors.add(
                            new Exception(
                                    String.format(
                                            Locale.US, "@Parameter(%d) is never used.", index)));
                } else if (numberOfUses > 1) {
                    errors.add(
                            new Exception(
                                    String.format(
                                            Locale.US,
                                            "@Parameter(%d) is used more than once (%d).",
                                            index,
                                            numberOfUses)));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<FrameworkField> getAnnotatedFieldsByParameter(TestClass testClass) {
        return testClass.getAnnotatedFields(Parameter.class);
    }

    static boolean fieldsAreAnnotated(TestClass testClass) {
        return !getAnnotatedFieldsByParameter(testClass).isEmpty();
    }

    @SuppressWarnings("unchecked")
    static List<Object> getParametersList(TestClass testClass) throws Throwable {
        return (List<Object>) getParametersMethod(testClass).invokeExplosively(null);
    }

    @SuppressWarnings("unchecked")
    static FrameworkMethod getParametersMethod(TestClass testClass) throws Exception {
        List<FrameworkMethod> methods = testClass.getAnnotatedMethods(Parameters.class);
        FrameworkMethod fallback = null;
        boolean isRunningOnDevice = isRunningOnAndroid();

        for (FrameworkMethod each : methods) {
            int modifiers = each.getMethod().getModifiers();
            if (Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers)) {
                switch (each.getAnnotation(Parameters.class).target()) {
                    case ALL -> fallback = each;
                    case DEVICE -> {
                        if (isRunningOnDevice) return each;
                    }
                    case DEVICE_LESS -> {
                        if (!isRunningOnDevice) return each;
                    }
                }
            }
        }
        if (fallback != null) {
            return fallback;
        }
        throw new Exception(
                String.format(
                        "No public static parameters method on class %s", testClass.getName()));
    }
}
