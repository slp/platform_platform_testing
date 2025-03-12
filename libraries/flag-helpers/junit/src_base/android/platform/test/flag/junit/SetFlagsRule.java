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

import static org.junit.Assume.assumeFalse;

import android.platform.test.flag.util.FlagReadException;
import android.platform.test.flag.util.FlagSetException;

import com.google.common.base.CaseFormat;
import com.google.common.collect.Sets;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** A {@link TestRule} that helps to set flag values in unit test. */
public final class SetFlagsRule implements TestRule {
    private static final String FAKE_FEATURE_FLAGS_IMPL_CLASS_NAME = "FakeFeatureFlagsImpl";
    private static final String REAL_FEATURE_FLAGS_IMPL_CLASS_NAME = "FeatureFlagsImpl";
    private static final String CUSTOM_FEATURE_FLAGS_CLASS_NAME = "CustomFeatureFlags";
    private static final String FEATURE_FLAGS_CLASS_NAME = "FeatureFlags";
    private static final String FEATURE_FLAGS_FIELD_NAME = "FEATURE_FLAGS";
    private static final String FLAGS_CLASS_NAME = "Flags";
    private static final String FLAG_CONSTANT_PREFIX = "FLAG_";
    private static final String SET_FLAG_METHOD_NAME = "setFlag";
    private static final String RESET_ALL_METHOD_NAME = "resetAll";
    private static final String IS_FLAG_FINALIZED_METHOD_NAME = "isFlagFinalized";
    private static final String IS_FLAG_READ_ONLY_OPTIMIZED_METHOD_NAME = "isFlagReadOnlyOptimized";

    // Store instances for entire life of a SetFlagsRule instance
    private final Map<Class<?>, Object> mFlagsClassToFakeFlagsImpl = new HashMap<>();
    private final Map<Class<?>, Object> mFlagsClassToRealFlagsImpl = new HashMap<>();

    // Store classes that are currently mutated by this rule
    private final Set<Class<?>> mMutatedFlagsClasses = new HashSet<>();

    // Any flags added to this list cannot be set imperatively (i.e. with enableFlags/disableFlags)
    private final Set<String> mLockedFlagNames = new HashSet<>();

    // listener to be called before setting a flag
    private final Listener mListener;

    // TODO(322377082): remove repackage prefix list
    private static final String[] REPACKAGE_PREFIX_LIST =
            new String[] {
                "", "com.android.internal.hidden_from_bootclasspath.",
            };
    private final Map<String, Set<String>> mPackageToRepackage = new HashMap<>();

    private final boolean mIsInitWithDefault;
    private FlagsParameterization mFlagsParameterization;
    private boolean mIsRuleEvaluating = false;

    public enum DefaultInitValueType {
        /**
         * Initialize flag value as null
         *
         * <p>Flag value need to be set before using
         */
        NULL_DEFAULT,

        /**
         * Initialize flag value with the default value from the device
         *
         * <p>If flag value is not overridden by adb, then the default value is from the release
         * configuration when the test is built.
         */
        DEVICE_DEFAULT,
    }

    public SetFlagsRule() {
        this(DefaultInitValueType.DEVICE_DEFAULT);
    }

    public SetFlagsRule(DefaultInitValueType defaultType) {
        this(defaultType, null);
    }

    public SetFlagsRule(@Nullable FlagsParameterization flagsParameterization) {
        this(DefaultInitValueType.DEVICE_DEFAULT, flagsParameterization);
    }

    public SetFlagsRule(
            DefaultInitValueType defaultType,
            @Nullable FlagsParameterization flagsParameterization) {
        this(defaultType, flagsParameterization, null);
    }

    private SetFlagsRule(
            DefaultInitValueType defaultType,
            @Nullable FlagsParameterization flagsParameterization,
            @Nullable Listener listener) {
        mIsInitWithDefault = defaultType == DefaultInitValueType.DEVICE_DEFAULT;
        mFlagsParameterization = flagsParameterization;
        if (flagsParameterization != null) {
            mLockedFlagNames.addAll(flagsParameterization.mOverrides.keySet());
        }
        mListener = listener;
    }

    /**
     * Set the FlagsParameterization to be used during this test. This cannot be used to override a
     * previous call, and cannot be called once the rule has been evaluated.
     */
    public void setFlagsParameterization(@Nonnull FlagsParameterization flagsParameterization) {
        Objects.requireNonNull(flagsParameterization, "FlagsParameterization cannot be cleared");
        if (mFlagsParameterization != null) {
            throw new AssertionError("FlagsParameterization cannot be overridden");
        }
        if (mIsRuleEvaluating) {
            throw new AssertionError("Cannot set FlagsParameterization once the rule is running");
        }
        ensureFlagsAreUnset();
        mFlagsParameterization = flagsParameterization;
        mLockedFlagNames.addAll(flagsParameterization.mOverrides.keySet());
    }

    /**
     * Enables the given flags.
     *
     * @param fullFlagNames The name of the flags in the flag class with the format
     *     {packageName}.{flagName}
     * @deprecated Annotate your test or class with <code>@EnableFlags(String...)</code> instead
     */
    @Deprecated
    public void enableFlags(String... fullFlagNames) {
        if (!mIsRuleEvaluating) {
            throw new IllegalStateException("Not allowed to set flags outside test and setup code");
        }
        for (String fullFlagName : fullFlagNames) {
            if (mLockedFlagNames.contains(fullFlagName)) {
                throw new FlagSetException(fullFlagName, "Not allowed to change locked flags");
            }
            setFlagValue(fullFlagName, true);
        }
    }

    /**
     * Disables the given flags.
     *
     * @param fullFlagNames The name of the flags in the flag class with the format
     *     {packageName}.{flagName}
     * @deprecated Annotate your test or class with <code>@DisableFlags(String...)</code> instead
     */
    @Deprecated
    public void disableFlags(String... fullFlagNames) {
        if (!mIsRuleEvaluating) {
            throw new IllegalStateException("Not allowed to set flags outside test and setup code");
        }
        for (String fullFlagName : fullFlagNames) {
            if (mLockedFlagNames.contains(fullFlagName)) {
                throw new FlagSetException(fullFlagName, "Not allowed to change locked flags");
            }
            setFlagValue(fullFlagName, false);
        }
    }

    private void ensureFlagsAreUnset() {
        if (!mFlagsClassToFakeFlagsImpl.isEmpty()) {
            throw new IllegalStateException("Some flags were set before the rule was initialized");
        }
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Throwable throwable = null;
                try {
                    if (mListener != null) {
                        mListener.onStartedEvaluating();
                    }
                    AnnotationsRetriever.FlagAnnotations flagAnnotations =
                            AnnotationsRetriever.getFlagAnnotations(description);
                    assertAnnotationsMatchParameterization(flagAnnotations, mFlagsParameterization);
                    flagAnnotations.assumeAllSetFlagsMatchParameterization(mFlagsParameterization);
                    if (mFlagsParameterization != null) {
                        ensureFlagsAreUnset();
                        for (Map.Entry<String, Boolean> pair :
                                mFlagsParameterization.mOverrides.entrySet()) {
                            setFlagValue(pair.getKey(), pair.getValue());
                        }
                    }
                    for (Map.Entry<String, Boolean> pair :
                            flagAnnotations.mSetFlagValues.entrySet()) {
                        setFlagValue(pair.getKey(), pair.getValue());
                    }
                    mLockedFlagNames.addAll(flagAnnotations.mRequiredFlagValues.keySet());
                    mLockedFlagNames.addAll(flagAnnotations.mSetFlagValues.keySet());
                    mIsRuleEvaluating = true;
                    base.evaluate();
                } catch (Throwable t) {
                    throwable = t;
                } finally {
                    mIsRuleEvaluating = false;
                    try {
                        resetFlags();
                    } catch (Throwable t) {
                        if (throwable != null) {
                            t.addSuppressed(throwable);
                        }
                        throwable = t;
                    }
                    try {
                        if (mListener != null) {
                            mListener.onFinishedEvaluating();
                        }
                    } catch (Throwable t) {
                        if (throwable != null) {
                            t.addSuppressed(throwable);
                        }
                        throwable = t;
                    }
                }
                if (throwable != null) throw throwable;
            }
        };
    }

    private static void assertAnnotationsMatchParameterization(
            AnnotationsRetriever.FlagAnnotations flagAnnotations,
            FlagsParameterization parameterization) {
        if (parameterization == null) return;
        Set<String> parameterizedFlags = parameterization.mOverrides.keySet();
        Set<String> requiredFlags = flagAnnotations.mRequiredFlagValues.keySet();
        // Assert that NO Annotation-Required flag is in the parameterization
        Set<String> parameterizedAndRequiredFlags =
                Sets.intersection(parameterizedFlags, requiredFlags);
        if (!parameterizedAndRequiredFlags.isEmpty()) {
            throw new AssertionError(
                    "The following flags have required values (per @RequiresFlagsEnabled or"
                            + " @RequiresFlagsDisabled) but they are part of the"
                            + " FlagParameterization: "
                            + parameterizedAndRequiredFlags);
        }
    }

    private void setFlagValue(String fullFlagName, boolean value) {
        if (!fullFlagName.contains(".")) {
            throw new FlagSetException(
                    fullFlagName, "Flag name is not the expected format {packgeName}.{flagName}.");
        }
        // Get all packages containing Flags referencing the same fullFlagName.
        Set<String> packageSet = getPackagesContainsFlag(fullFlagName);

        for (String packageName : packageSet) {
            setFlagValue(Flag.createFlag(fullFlagName, packageName), value);
        }
    }

    private Set<String> getPackagesContainsFlag(String fullFlagName) {
        return getAllPackagesForFlag(fullFlagName, mPackageToRepackage);
    }

    private static Set<String> getAllPackagesForFlag(
            String fullFlagName, Map<String, Set<String>> packageToRepackage) {

        String packageName = Flag.getFlagPackageName(fullFlagName);
        Set<String> packageSet = packageToRepackage.getOrDefault(packageName, new HashSet<>());

        if (!packageSet.isEmpty()) {
            return packageSet;
        }

        for (String prefix : REPACKAGE_PREFIX_LIST) {
            String repackagedName = String.format("%s%s", prefix, packageName);
            String flagClassName = String.format("%s.%s", repackagedName, FLAGS_CLASS_NAME);
            try {
                Class.forName(flagClassName, false, SetFlagsRule.class.getClassLoader());
                packageSet.add(repackagedName);
            } catch (ClassNotFoundException e) {
                // Skip if the class is not found
                // An error will be thrown if no package containing flags referencing
                // the passed in flag
            }
        }
        packageToRepackage.put(packageName, packageSet);
        if (packageSet.isEmpty()) {
            throw new FlagSetException(
                    fullFlagName,
                    "Cannot find package containing Flags class referencing to this flag.");
        }
        return packageSet;
    }

    private void setFlagValue(Flag flag, boolean value) {
        if (mListener != null) {
            mListener.onBeforeSetFlag(flag, value);
        }

        Object fakeFlagsImplInstance = null;

        Class<?> flagsClass = getFlagClassFromFlag(flag);
        fakeFlagsImplInstance = getOrCreateFakeFlagsImp(flagsClass);

        if (!mMutatedFlagsClasses.contains(flagsClass)) {
            // Replace FeatureFlags in Flags class with FakeFeatureFlagsImpl
            replaceFlagsImpl(flagsClass, fakeFlagsImplInstance);
            mMutatedFlagsClasses.add(flagsClass);
        }

        // If the test is trying to set the flag value on a read_only flag in an optimized build
        // skip this test, since it is not a valid testing case
        // The reason for skipping instead of throwning error here is all read_write flag will be
        // change to read_only in the final release configuration. Thus the test could be executed
        // in other release configuration cases
        // TODO(b/337449119): SetFlagsRule should still run tests that are consistent with the
        // read-only values of flags. But be careful, if a ClassRule exists, the value returned by
        // the original FeatureFlags instance may be overridden, and reading it may not be allowed.
        boolean isOptimized =
                verifyFlag(fakeFlagsImplInstance, flag, IS_FLAG_READ_ONLY_OPTIMIZED_METHOD_NAME);
        assumeFalse(
                String.format(
                        "Flag %s is read_only, and the code is optimized. "
                                + " The flag value should not be modified on this build"
                                + " Skip this test.",
                        flag.fullFlagName()),
                isOptimized);

        boolean isFinalized =
                verifyFlag(fakeFlagsImplInstance, flag, IS_FLAG_FINALIZED_METHOD_NAME);
        assumeFalse(
                String.format(
                        "Flag %s is finalized on this device. "
                                + " The flag value should not be turned off on this device"
                                + " Skip this test.",
                        flag.fullFlagName()),
                isFinalized && !value);

        // Set desired flag value in the FakeFeatureFlagsImpl
        setFlagValueInFakeFeatureFlagsImpl(fakeFlagsImplInstance, flag, value);
    }

    private static Class<?> getFlagClassFromFlag(Flag flag) {
        String className = flag.flagsClassName();
        Class<?> flagsClass = null;
        try {
            flagsClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new FlagSetException(
                    flag.fullFlagName(),
                    String.format(
                            "Can not load the Flags class %s to set its values. Please check the "
                                    + "flag name and ensure that the aconfig auto generated "
                                    + "library is in the dependency.",
                            className),
                    e);
        }
        return flagsClass;
    }

    private static Class<?> getFlagClassFromFlagsClassName(String className) {
        if (!className.endsWith("." + FLAGS_CLASS_NAME)) {
            throw new FlagSetException(
                    className,
                    "Can not watch this Flags class because it is not named 'Flags'. Please ensure"
                            + " your @UsesFlags() annotations only reference the Flags classes.");
        }
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new FlagSetException(
                    className,
                    "Cannot load this Flags class to set its values. Please check the flag name and"
                        + " ensure that the aconfig auto generated library is in the dependency.",
                    e);
        }
    }

    private boolean getFlagValue(Object featureFlagsImpl, Flag flag) {
        // Must be consistent with method name in aconfig auto generated code.
        String methodName = getFlagMethodName(flag);
        String fullFlagName = flag.fullFlagName();

        try {
            Object result =
                    featureFlagsImpl.getClass().getMethod(methodName).invoke(featureFlagsImpl);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
            throw new FlagReadException(
                    fullFlagName,
                    String.format(
                            "Flag type is %s, not boolean", result.getClass().getSimpleName()));
        } catch (NoSuchMethodException e) {
            throw new FlagReadException(
                    fullFlagName,
                    String.format(
                            "No method %s in the Flags class %s to read the flag value. Please"
                                    + " check the flag name.",
                            methodName, featureFlagsImpl.getClass().getName()),
                    e);
        } catch (ReflectiveOperationException e) {
            throw new FlagReadException(
                    fullFlagName,
                    String.format(
                            "Fail to get value of flag %s from instance %s",
                            fullFlagName, featureFlagsImpl.getClass().getName()),
                    e);
        }
    }

    private String getFlagMethodName(Flag flag) {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, flag.simpleFlagName());
    }

    private void setFlagValueInFakeFeatureFlagsImpl(
            Object fakeFeatureFlagsImpl, Flag flag, boolean value) {
        String fullFlagName = flag.fullFlagName();
        try {
            fakeFeatureFlagsImpl
                    .getClass()
                    .getMethod(SET_FLAG_METHOD_NAME, String.class, boolean.class)
                    .invoke(fakeFeatureFlagsImpl, fullFlagName, value);
        } catch (NoSuchMethodException e) {
            throw new FlagSetException(
                    fullFlagName,
                    String.format(
                            "Flag implementation %s is not fake implementation",
                            fakeFeatureFlagsImpl.getClass().getName()),
                    e);
        } catch (ReflectiveOperationException e) {
            throw new FlagSetException(fullFlagName, e);
        }
    }

    private static boolean verifyFlag(Object fakeFeatureFlagsImpl, Flag flag, String methodName) {
        String fullFlagName = flag.fullFlagName();
        try {
            boolean result =
                    (Boolean)
                            fakeFeatureFlagsImpl
                                    .getClass()
                                    .getMethod(methodName, String.class)
                                    .invoke(fakeFeatureFlagsImpl, fullFlagName);
            return result;
        } catch (NoSuchMethodException e) {
            // If the flag is generated under exported mode, then it doesn't have this method
            String simpleClassName = fakeFeatureFlagsImpl.getClass().getSimpleName();
            if (simpleClassName.equals(FAKE_FEATURE_FLAGS_IMPL_CLASS_NAME)) {
                return false;
            }
            if (simpleClassName.equals(CUSTOM_FEATURE_FLAGS_CLASS_NAME)) {
                return false;
            }
            throw new FlagSetException(
                    fullFlagName,
                    String.format(
                            "Cannot invoke %s. "
                                    + "Flag implementation %s is not fake implementation",
                            methodName, fakeFeatureFlagsImpl.getClass().getName()),
                    e);
        } catch (ReflectiveOperationException e) {
            throw new FlagSetException(fullFlagName, e);
        }
    }

    @Nonnull
    private Object getOrCreateFakeFlagsImp(Class<?> flagsClass) {
        Object fakeFlagsImplInstance = mFlagsClassToFakeFlagsImpl.get(flagsClass);
        if (fakeFlagsImplInstance != null) {
            return fakeFlagsImplInstance;
        }

        String packageName = flagsClass.getPackageName();
        String fakeClassName =
                String.format("%s.%s", packageName, FAKE_FEATURE_FLAGS_IMPL_CLASS_NAME);
        String interfaceName = String.format("%s.%s", packageName, FEATURE_FLAGS_CLASS_NAME);

        Object realFlagsImplInstance = readFlagsImpl(flagsClass);
        mFlagsClassToRealFlagsImpl.put(flagsClass, realFlagsImplInstance);

        try {
            Class<?> flagImplClass = Class.forName(fakeClassName);
            Class<?> flagInterface = Class.forName(interfaceName);
            fakeFlagsImplInstance =
                    flagImplClass
                            .getConstructor(flagInterface)
                            .newInstance(mIsInitWithDefault ? realFlagsImplInstance : null);
        } catch (ReflectiveOperationException e) {
            throw new UnsupportedOperationException(
                    String.format(
                            "Cannot create FakeFeatureFlagsImpl in Flags class %s.",
                            flagsClass.getName()),
                    e);
        }

        mFlagsClassToFakeFlagsImpl.put(flagsClass, fakeFlagsImplInstance);

        return fakeFlagsImplInstance;
    }

    private static void replaceFlagsImpl(Class<?> flagsClass, Object flagsImplInstance) {
        Field featureFlagsField = getFeatureFlagsField(flagsClass);
        try {
            featureFlagsField.set(null, flagsImplInstance);
        } catch (IllegalAccessException e) {
            throw new UnsupportedOperationException(
                    String.format(
                            "Cannot replace FeatureFlagsImpl to %s.",
                            flagsImplInstance.getClass().getName()),
                    e);
        }
    }

    private static Object readFlagsImpl(Class<?> flagsClass) {
        Field featureFlagsField = getFeatureFlagsField(flagsClass);
        try {
            return featureFlagsField.get(null);
        } catch (IllegalAccessException e) {
            throw new UnsupportedOperationException(
                    String.format(
                            "Cannot get FeatureFlags from Flags class %s.", flagsClass.getName()),
                    e);
        }
    }

    private static Field getFeatureFlagsField(Class<?> flagsClass) {
        Field featureFlagsField = null;
        try {
            featureFlagsField = flagsClass.getDeclaredField(FEATURE_FLAGS_FIELD_NAME);
        } catch (ReflectiveOperationException e) {
            throw new UnsupportedOperationException(
                    String.format(
                            "Cannot store FeatureFlagsImpl in Flag %s.", flagsClass.getName()),
                    e);
        }
        featureFlagsField.setAccessible(true);
        return featureFlagsField;
    }

    private void resetFlags() {
        String flagsClassName = null;
        try {
            for (Class<?> flagsClass : mMutatedFlagsClasses) {
                flagsClassName = flagsClass.getName();
                Object fakeFlagsImplInstance = mFlagsClassToFakeFlagsImpl.get(flagsClass);
                Object flagsImplInstance = mFlagsClassToRealFlagsImpl.get(flagsClass);
                // Replace FeatureFlags in Flags class with real FeatureFlagsImpl
                replaceFlagsImpl(flagsClass, flagsImplInstance);
                fakeFlagsImplInstance
                        .getClass()
                        .getMethod(RESET_ALL_METHOD_NAME)
                        .invoke(fakeFlagsImplInstance);
            }
            mMutatedFlagsClasses.clear();
        } catch (Exception e) {
            throw new FlagSetException(flagsClassName, e);
        }
    }

    /** An interface that provides hooks to the ClassRule. */
    private interface Listener {
        /** Called before a flag is set. */
        void onBeforeSetFlag(SetFlagsRule.Flag flag, boolean value);

        /** Called after the rule has started evaluating for a test. */
        void onStartedEvaluating();

        /** Called after the rule has finished evaluating for a test. */
        void onFinishedEvaluating();
    }

    /**
     * A @ClassRule which adds extra consistency checks for SetFlagsRule.
     * <li>Requires that tests monitor the Flags class of any flag that is set.
     * <li>Fails a test if a flag that is set was read before the test started.
     */
    public static class ClassRule implements TestRule {
        /** The flags classes that are requested to be watched during construction. */
        private final Set<Class<?>> mGlobalFlagsClassesToWatch = new HashSet<>();

        /** The flags packages that are allowed to be set, for quick per-flag lookup */
        private final Set<String> mSettableFlagsPackages = new HashSet<>();

        /** The mapping from the Flags classes to the real implementations */
        private final Map<Class<?>, Object> mFlagsClassToRealFlagsImpl = new HashMap<>();

        /** The mapping from the Flags classes to the watcher implementations */
        private final Map<Class<?>, Object> mFlagsClassToWatcherImpl = new HashMap<>();

        /** The flags classes that have actually been mutated */
        private final Set<Class<?>> mMutatedFlagsClasses = new HashSet<>();

        /** The flag values set by class annotations */
        private final Map<String, Boolean> mClassLevelSetFlagValues = new ConcurrentHashMap<>();

        /**
         * The individual flags which have been read from prior to tests starting, mapped to the
         * stack trace of the first read.
         */
        private final Map<String, FirstFlagRead> mFirstReadOutsideTestsByFlag =
                new ConcurrentHashMap<>();

        /**
         * The individual flags which have been read from within a test, mapped to the stack trace
         * of the first read.
         */
        private final Map<String, FirstFlagRead> mFirstReadWithinTestByFlag =
                new ConcurrentHashMap<>();

        /** repackage cache */
        private final Map<String, Set<String>> mPackageToRepackage = new HashMap<>();

        /** The depth of the ClassRule evaluating on potentially nested suites */
        private int mSuiteRunDepth = 0;

        /** Whether the SetFlagsRule is evaluating for a test */
        private boolean mIsTestRunning = false;

        /** Typical constructor takes an initial list flags classes to watch */
        public ClassRule(Class<?>... flagsClasses) {
            for (Class<?> flagsClass : flagsClasses) {
                mGlobalFlagsClassesToWatch.add(flagsClass);
            }
        }

        /** Listener to be notified of events in any created SetFlagsRule */
        private SetFlagsRule.Listener mListener =
                new SetFlagsRule.Listener() {
                    @Override
                    public void onBeforeSetFlag(SetFlagsRule.Flag flag, boolean value) {
                        if (!mIsTestRunning) {
                            throw new IllegalStateException("Inner rule should be running!");
                        }
                        assertFlagCanBeSet(flag, value);
                    }

                    @Override
                    public void onStartedEvaluating() {
                        if (mSuiteRunDepth == 0) {
                            throw new IllegalStateException("Outer rule should be running!");
                        }
                        if (mIsTestRunning) {
                            throw new IllegalStateException("Inner rule is still running!");
                        }
                        mIsTestRunning = true;
                    }

                    @Override
                    public void onFinishedEvaluating() {
                        if (!mIsTestRunning) {
                            throw new IllegalStateException("Inner rule did not start!");
                        }
                        mIsTestRunning = false;
                        checkAllFlagsWatchersRestored();
                        mFirstReadWithinTestByFlag.clear();
                    }
                };

        /**
         * Creates a SetFlagsRule which will work as normal, but additionally enforce the guarantees
         * about not setting flags that were read within the ClassRule
         */
        public SetFlagsRule createSetFlagsRule() {
            return createSetFlagsRule(null);
        }

        /**
         * Creates a SetFlagsRule with parameterization which will work as normal, but additionally
         * enforce the guarantees about not setting flags that were read within the ClassRule
         */
        public SetFlagsRule createSetFlagsRule(
                @Nullable FlagsParameterization flagsParameterization) {
            return new SetFlagsRule(
                    DefaultInitValueType.DEVICE_DEFAULT, flagsParameterization, mListener);
        }

        private boolean isFlagsClassMonitored(SetFlagsRule.Flag flag) {
            return mSettableFlagsPackages.contains(flag.flagPackageName());
        }

        private void assertFlagCanBeSet(SetFlagsRule.Flag flag, boolean value) {
            Exception firstReadWithinTest = mFirstReadWithinTestByFlag.get(flag.fullFlagName());
            if (firstReadWithinTest != null) {
                throw new FlagSetException(
                        flag.fullFlagName(),
                        "This flag was locked when it was read earlier in this test. To fix this"
                                + " error, always use @EnableFlags() and @DisableFlags() to set"
                                + " flags, which ensures flags are set before even any"
                                + " @Before-annotated setup methods.",
                        firstReadWithinTest);
            }
            Exception firstReadOutsideTest = mFirstReadOutsideTestsByFlag.get(flag.fullFlagName());
            if (firstReadOutsideTest != null) {
                throw new FlagSetException(
                        flag.fullFlagName(),
                        "This flag was locked when it was read outside of the test code; likely"
                                + " during initialization of the test class. To fix this error,"
                                + " move test fixture initialization code into your"
                                + " @Before-annotated setup method, and ensure you are using"
                                + " @EnableFlags() and @DisableFlags() to set flags.",
                        firstReadOutsideTest);
            }
            if (!isFlagsClassMonitored(flag)) {
                throw new FlagSetException(
                        flag.fullFlagName(),
                        "This flag's class is not monitored. Always use @EnableFlags() and"
                                + " @DisableFlags() on the class or method instead of"
                                + " .enableFlags() or .disableFlags() to prevent this error. When"
                                + " using FlagsParameterization, add `@UsesFlags("
                                + flag.flagPackageName()
                                + ".Flags.class)` to the test class. As a last resort, pass the"
                                + " Flags class to the constructor of your"
                                + " SetFlagsRule.ClassRule.");
            }
            // Detect errors where the rule messed up and set the wrong flag value.
            Boolean classLevelValue = mClassLevelSetFlagValues.get(flag.fullFlagName());
            if (classLevelValue != null && classLevelValue != value) {
                throw new FlagSetException(
                        flag.fullFlagName(),
                        "This flag's value was set at the class level to a different value.");
            }
        }

        private void checkInstanceOfRealFlagsImpl(Object actual) {
            if (!actual.getClass().getSimpleName().equals(REAL_FEATURE_FLAGS_IMPL_CLASS_NAME)) {
                throw new IllegalStateException(
                        String.format(
                                "Wrong impl type during setup: '%s' is not a %s",
                                actual, REAL_FEATURE_FLAGS_IMPL_CLASS_NAME));
            }
        }

        private void checkSameAs(Object expected, Object actual) {
            if (expected != actual) {
                throw new IllegalStateException(
                        String.format(
                                "Wrong impl instance found during teardown: expected %s but was %s",
                                expected, actual));
            }
        }

        private Object getOrCreateFlagReadWatcher(Class<?> flagsClass) {
            Object watcher = mFlagsClassToWatcherImpl.get(flagsClass);
            if (watcher != null) {
                return watcher;
            }
            Object flagsImplInstance = readFlagsImpl(flagsClass);
            // strict mode: ensure that the current impl is the real impl
            checkInstanceOfRealFlagsImpl(flagsImplInstance);
            // save the real impl for restoration later
            mFlagsClassToRealFlagsImpl.put(flagsClass, flagsImplInstance);
            watcher = newFlagReadWatcher(flagsClass, flagsImplInstance);
            mFlagsClassToWatcherImpl.put(flagsClass, watcher);
            return watcher;
        }

        private void recordFlagRead(String flagName) {
            if (mIsTestRunning) {
                mFirstReadWithinTestByFlag.computeIfAbsent(flagName, FirstFlagRead::new);
            } else {
                mFirstReadOutsideTestsByFlag.computeIfAbsent(flagName, FirstFlagRead::new);
            }
        }

        private Object newFlagReadWatcher(Class<?> flagsClass, Object flagsImplInstance) {
            String packageName = flagsClass.getPackageName();
            String customClassName =
                    String.format("%s.%s", packageName, CUSTOM_FEATURE_FLAGS_CLASS_NAME);
            BiPredicate<String, Predicate<Object>> getValueImpl =
                    (flagName, predicate) -> {
                        // Flags set at the class level pose no consistency risk
                        Boolean value = mClassLevelSetFlagValues.get(flagName);
                        if (value != null) {
                            return value;
                        }
                        recordFlagRead(flagName);
                        return predicate.test(flagsImplInstance);
                    };
            try {
                Class<?> customFlagsClass = Class.forName(customClassName);
                return customFlagsClass.getConstructor(BiPredicate.class).newInstance(getValueImpl);
            } catch (ReflectiveOperationException e) {
                throw new UnsupportedOperationException(
                        String.format(
                                "Cannot create CustomFeatureFlags in Flags class %s.",
                                flagsClass.getName()),
                        e);
            }
        }

        /** Get the package name of the flags in this class. This is the non-repackaged name. */
        private String getFlagPackageName(Class<?> flagsClass) {
            String classPackageName = flagsClass.getPackageName();
            String shortestPackageName = classPackageName;
            for (String prefix : REPACKAGE_PREFIX_LIST) {
                if (prefix.isEmpty()) continue;
                if (classPackageName.startsWith(prefix)) {
                    String unprefixedPackage = classPackageName.substring(prefix.length());
                    if (unprefixedPackage.length() < shortestPackageName.length()) {
                        shortestPackageName = unprefixedPackage;
                    }
                }
            }
            return shortestPackageName;
        }

        private void setupClassLevelFlagValues(Description description) {
            mClassLevelSetFlagValues.putAll(
                    AnnotationsRetriever.getFlagAnnotations(description).mSetFlagValues);
        }

        private void setupFlagsWatchers(Description description) {
            // Start with the static list of Flags classes to watch
            Set<Class<?>> flagsClassesToWatch = new HashSet<>(mGlobalFlagsClassesToWatch);
            // Collect the Flags classes from @UsedFlags annotations on the Descriptor
            Set<String> usedFlagsClasses = AnnotationsRetriever.getAllUsedFlagsClasses(description);
            for (String flagsClassName : usedFlagsClasses) {
                flagsClassesToWatch.add(getFlagClassFromFlagsClassName(flagsClassName));
            }
            // Now setup watchers on the provided Flags classes
            for (Class<?> flagsClass : flagsClassesToWatch) {
                setupFlagsWatcher(flagsClass, getFlagPackageName(flagsClass));
            }
            // Get all annotated flags and then the distinct packages for each flag
            Set<String> setFlags = AnnotationsRetriever.getAllAnnotationSetFlags(description);
            Set<String> extraFlagPackages = new HashSet<>();
            for (String setFlag : setFlags) {
                extraFlagPackages.add(Flag.getFlagPackageName(setFlag));
            }
            // Do not bother with flags that are already monitored
            extraFlagPackages.removeAll(mSettableFlagsPackages);
            // Expand packages to all repackaged versions, stored as Flag objects
            Set<Flag> extraWildcardFlags = new HashSet<>();
            for (String extraFlagPackage : extraFlagPackages) {
                String fullFlagName = extraFlagPackage + ".*";
                Set<String> packages = getAllPackagesForFlag(fullFlagName, mPackageToRepackage);
                for (String packageName : packages) {
                    Flag flag = Flag.createFlag(fullFlagName, packageName);
                    extraWildcardFlags.add(flag);
                }
            }
            // Set up watchers for each wildcard flag
            for (Flag flag : extraWildcardFlags) {
                Class<?> flagsClass = getFlagClassFromFlag(flag);
                setupFlagsWatcher(flagsClass, flag.flagPackageName());
            }
        }

        private void setupFlagsWatcher(Class<?> flagsClass, String flagPackageName) {
            if (mMutatedFlagsClasses.contains(flagsClass)) {
                throw new IllegalStateException(
                        String.format("Flags class %s is already mutated", flagsClass.getName()));
            }
            Object watcher = getOrCreateFlagReadWatcher(flagsClass);
            replaceFlagsImpl(flagsClass, watcher);
            mMutatedFlagsClasses.add(flagsClass);
            mSettableFlagsPackages.add(flagPackageName);
        }

        private void teardownFlagsWatchers() {
            try {
                for (Class<?> flagsClass : mMutatedFlagsClasses) {
                    Object flagsImplInstance = mFlagsClassToRealFlagsImpl.get(flagsClass);
                    // strict mode: ensure that the watcher is still in place
                    Object watcher = readFlagsImpl(flagsClass);
                    checkSameAs(mFlagsClassToWatcherImpl.get(flagsClass), watcher);
                    // Replace FeatureFlags in Flags class with real FeatureFlagsImpl
                    replaceFlagsImpl(flagsClass, flagsImplInstance);
                }
                mMutatedFlagsClasses.clear();
                mSettableFlagsPackages.clear();
                mFirstReadOutsideTestsByFlag.clear();
            } catch (IllegalStateException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to teardown Flags watchers", e);
            }
            if (mIsTestRunning) {
                throw new IllegalStateException("An inner SetFlagsRule is still running");
            }
            if (!mFirstReadWithinTestByFlag.isEmpty()) {
                throw new IllegalStateException("An inner SetFlagsRule did not fully clean up");
            }
        }

        private void checkAllFlagsWatchersRestored() {
            for (Class<?> flagsClass : mMutatedFlagsClasses) {
                Object watcher = readFlagsImpl(flagsClass);
                checkSameAs(mFlagsClassToWatcherImpl.get(flagsClass), watcher);
            }
        }

        @Override
        public Statement apply(Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    Throwable throwable = null;
                    final int initialDepth = mSuiteRunDepth;
                    try {
                        mSuiteRunDepth++;
                        if (initialDepth == 0) {
                            setupFlagsWatchers(description);
                            setupClassLevelFlagValues(description);
                        }
                        base.evaluate();
                    } catch (Throwable t) {
                        throwable = t;
                    } finally {
                        mSuiteRunDepth--;
                        try {
                            if (initialDepth == 0) {
                                mClassLevelSetFlagValues.clear();
                                teardownFlagsWatchers();
                            }
                            if (mSuiteRunDepth != initialDepth) {
                                throw new IllegalStateException(
                                        String.format(
                                                "Evaluations were not correctly nested: initial"
                                                        + " depth was %d but final depth was %d",
                                                initialDepth, mSuiteRunDepth));
                            }
                        } catch (Throwable t) {
                            if (throwable != null) {
                                t.addSuppressed(throwable);
                            }
                            throwable = t;
                        }
                    }
                    if (throwable != null) throw throwable;
                }
            };
        }
    }

    private static class FirstFlagRead extends Exception {
        FirstFlagRead(String flagName) {
            super(String.format("Flag '%s' was first read at this location:", flagName));
        }
    }

    private static class Flag {
        private static final String PACKAGE_NAME_SIMPLE_NAME_SEPARATOR = ".";
        private final String mFullFlagName;
        private final String mFlagPackageName;
        private final String mClassPackageName;
        private final String mSimpleFlagName;

        public static String getFlagPackageName(String fullFlagName) {
            int index = fullFlagName.lastIndexOf(PACKAGE_NAME_SIMPLE_NAME_SEPARATOR);
            return fullFlagName.substring(0, index);
        }

        public static Flag createFlag(String fullFlagName) {
            int index = fullFlagName.lastIndexOf(PACKAGE_NAME_SIMPLE_NAME_SEPARATOR);
            String packageName = fullFlagName.substring(0, index);
            return createFlag(fullFlagName, packageName);
        }

        public static Flag createFlag(String fullFlagName, String classPackageName) {
            if (!fullFlagName.contains(PACKAGE_NAME_SIMPLE_NAME_SEPARATOR)
                    || !classPackageName.contains(PACKAGE_NAME_SIMPLE_NAME_SEPARATOR)) {
                throw new IllegalArgumentException(
                        String.format(
                                "Flag %s is invalid. The format should be {packageName}"
                                        + ".{simpleFlagName}",
                                fullFlagName));
            }
            int index = fullFlagName.lastIndexOf(PACKAGE_NAME_SIMPLE_NAME_SEPARATOR);
            String flagPackageName = fullFlagName.substring(0, index);
            String simpleFlagName = fullFlagName.substring(index + 1);

            return new Flag(fullFlagName, flagPackageName, classPackageName, simpleFlagName);
        }

        private Flag(
                String fullFlagName,
                String flagPackageName,
                String classPackageName,
                String simpleFlagName) {
            this.mFullFlagName = fullFlagName;
            this.mFlagPackageName = flagPackageName;
            this.mClassPackageName = classPackageName;
            this.mSimpleFlagName = simpleFlagName;
        }

        public String fullFlagName() {
            return mFullFlagName;
        }

        public String flagPackageName() {
            return mFlagPackageName;
        }

        public String classPackageName() {
            return mClassPackageName;
        }

        public String simpleFlagName() {
            return mSimpleFlagName;
        }

        public String flagsClassName() {
            return String.format("%s.%s", classPackageName(), FLAGS_CLASS_NAME);
        }
    }
}
