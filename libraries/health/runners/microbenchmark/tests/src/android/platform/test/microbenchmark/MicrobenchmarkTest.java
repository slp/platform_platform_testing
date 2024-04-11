/*
 * Copyright (C) 2018 The Android Open Source Project
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
package android.platform.test.microbenchmark;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.os.Bundle;
import android.os.SystemClock;
import android.platform.test.microbenchmark.Microbenchmark.TerminateEarlyException;
import android.platform.test.rule.TestWatcher;
import android.platform.test.rule.TracePointRule;

import androidx.test.InstrumentationRegistry;
import androidx.test.internal.runner.TestRequestBuilder;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.JUnit4;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for the {@link Microbenchmark} runner.
 */
@RunWith(JUnit4.class)
public final class MicrobenchmarkTest {
    // Static logs are needed to validate dynamic rules and tests that use TestRequestBuilder, where
    // objects are instantiated with reflection and not directly accessible.
    private static List<String> sLogs = new ArrayList<>();
    // Holds the state of the instrumentation args before each test for restoring after. Some tests
    // need to manipulate the arguments directly, as the underlying object is instantiated through
    // reflection and thus not directly manipulate-able.
    private Bundle mArgumentsBeforeTest;

    @Before
    public void setUp() {
        sLogs.clear();
        mArgumentsBeforeTest = InstrumentationRegistry.getArguments();
    }

    @After
    public void restoreArguments() {
        InstrumentationRegistry.registerInstance(
                InstrumentationRegistry.getInstrumentation(), mArgumentsBeforeTest);
    }

    /**
     * Tests that iterations are respected for microbenchmark tests.
     */
    @Test
    public void testIterationCount() throws InitializationError {
        Bundle args = new Bundle();
        args.putString("iterations", "10");
        Microbenchmark microbench = new Microbenchmark(BasicTest.class, args);
        assertThat(microbench.testCount()).isEqualTo(10);
    }

    public static class BasicTest {
        @Test
        public void doNothingTest() { }
    }

    /**
     * Tests that {@link TracePointRule} and {@link TightMethodRule}s are properly ordered.
     *
     * Before --> TightBefore --> Trace (begin) --> Test --> Trace(end) --> TightAfter --> After
     */
    @Test
    public void testFeatureExecutionOrder() throws InitializationError {
        LoggingMicrobenchmark loggingRunner = new LoggingMicrobenchmark(LoggingTest.class);
        Result result = new JUnitCore().run(loggingRunner);
        assertThat(result.wasSuccessful()).isTrue();
        assertThat(sLogs)
                .containsExactly(
                        "before",
                        "tight before",
                        "begin: testMethod("
                                + "android.platform.test.microbenchmark.MicrobenchmarkTest$"
                                + "LoggingTest)",
                        "test",
                        "end",
                        "tight after",
                        "after")
                .inOrder();
    }

    /**
     * Test iterations number are added to the test name with default suffix.
     *
     * Before --> TightBefore --> Trace (begin) with suffix @1 --> Test --> Trace(end)
     *  --> TightAfter --> After --> Before --> TightBefore --> Trace (begin) with suffix @2
     *  --> Test --> Trace(end) --> TightAfter --> After
     */
    @Test
    public void testMultipleIterationsWithRename() throws InitializationError {
        Bundle args = new Bundle();
        args.putString("iterations", "2");
        args.putString("rename-iterations", "true");
        LoggingMicrobenchmark loggingRunner = new LoggingMicrobenchmark(LoggingTest.class, args);
        Result result = new JUnitCore().run(loggingRunner);
        assertThat(result.wasSuccessful()).isTrue();
        assertThat(sLogs)
                .containsExactly(
                        "before",
                        "tight before",
                        "begin: testMethod$1("
                                + "android.platform.test.microbenchmark.MicrobenchmarkTest$"
                                + "LoggingTest)",
                        "test",
                        "end",
                        "tight after",
                        "after",
                        "before",
                        "tight before",
                        "begin: testMethod$2("
                                + "android.platform.test.microbenchmark.MicrobenchmarkTest$"
                                + "LoggingTest)",
                        "test",
                        "end",
                        "tight after",
                        "after")
                .inOrder();
    }

    /**
     * Test iterations number are added to the test name with custom suffix.
     *
     * Before --> TightBefore --> Trace (begin) with suffix --1 --> Test --> Trace(end)
     *  --> TightAfter --> After --> Before --> TightBefore --> Trace (begin) with suffix --2
     *   --> Test --> Trace(end) --> TightAfter --> After
     */
    @Test
    public void testMultipleIterationsWithDifferentSuffix() throws InitializationError {
        Bundle args = new Bundle();
        args.putString("iterations", "2");
        args.putString("rename-iterations", "true");
        args.putString("iteration-separator", "--");
        LoggingMicrobenchmark loggingRunner = new LoggingMicrobenchmark(LoggingTest.class, args);
        Result result = new JUnitCore().run(loggingRunner);
        assertThat(result.wasSuccessful()).isTrue();
        assertThat(sLogs)
                .containsExactly(
                        "before",
                        "tight before",
                        "begin: testMethod--1("
                                + "android.platform.test.microbenchmark.MicrobenchmarkTest$"
                                + "LoggingTest)",
                        "test",
                        "end",
                        "tight after",
                        "after",
                        "before",
                        "tight before",
                        "begin: testMethod--2("
                                + "android.platform.test.microbenchmark.MicrobenchmarkTest$"
                                + "LoggingTest)",
                        "test",
                        "end",
                        "tight after",
                        "after")
                .inOrder();
    }

    /**
     * Test iteration number are not added to the test name when explictly disabled.
     *
     * Before --> TightBefore --> Trace (begin) --> Test --> Trace(end) --> TightAfter
     *  --> After
     */
    @Test
    public void testMultipleIterationsWithoutRename() throws InitializationError {
        Bundle args = new Bundle();
        args.putString("iterations", "1");
        args.putString("rename-iterations", "false");
        LoggingMicrobenchmark loggingRunner = new LoggingMicrobenchmark(LoggingTest.class, args);
        Result result = new JUnitCore().run(loggingRunner);
        assertThat(result.wasSuccessful()).isTrue();
        assertThat(sLogs)
                .containsExactly(
                        "before",
                        "tight before",
                        "begin: testMethod("
                                + "android.platform.test.microbenchmark.MicrobenchmarkTest"
                                + "$LoggingTest)",
                        "test",
                        "end",
                        "tight after",
                        "after")
                .inOrder();
    }

    /**
     * Test method iteration will iterate the inner-most test method N times.
     *
     * <p>Before --> TightBefore --> Trace (begin) --> Test x N --> Trace(end) --> TightAfter -->
     * After
     */
    @Test
    public void testMultipleMethodIterations() throws InitializationError {
        Bundle args = new Bundle();
        args.putString("iterations", "1");
        args.putString("method-iterations", "10");
        args.putString("rename-iterations", "false");
        LoggingMicrobenchmark loggingRunner = new LoggingMicrobenchmark(LoggingTest.class, args);
        Result result = new JUnitCore().run(loggingRunner);
        assertThat(result.wasSuccessful()).isTrue();
        assertThat(sLogs)
                .containsExactly(
                        "before",
                        "tight before",
                        "begin: testMethod("
                                + "android.platform.test.microbenchmark.MicrobenchmarkTest"
                                + "$LoggingTest)",
                        "test",
                        "test",
                        "test",
                        "test",
                        "test",
                        "test",
                        "test",
                        "test",
                        "test",
                        "test",
                        "end",
                        "tight after",
                        "after")
                .inOrder();
    }

    /** Test that the microbenchmark will terminate if the battery is too low. */
    @Test
    public void testStopsEarly_ifBatteryLevelIsBelowThreshold() throws InitializationError {
        Bundle args = new Bundle();
        args.putString(Microbenchmark.MIN_BATTERY_LEVEL_OPTION, "50");
        args.putString(Microbenchmark.MAX_BATTERY_DRAIN_OPTION, "20");
        Microbenchmark runner = Mockito.spy(new Microbenchmark(LoggingTest.class, args));
        doReturn(49).when(runner).getBatteryLevel();

        RunNotifier notifier = Mockito.mock(RunNotifier.class);
        runner.run(notifier);

        ArgumentCaptor<Failure> failureCaptor = ArgumentCaptor.forClass(Failure.class);
        verify(notifier).fireTestFailure(failureCaptor.capture());

        Failure failure = failureCaptor.getValue();
        Throwable throwable = failure.getException();
        assertTrue(
                String.format(
                        "Exception was not a TerminateEarlyException. Instead, it was: %s",
                        throwable.getClass()),
                throwable instanceof TerminateEarlyException);
        assertThat(throwable)
                .hasMessageThat()
                .matches("Terminating early.*battery level.*threshold.");
    }

    /** Test that the microbenchmark will terminate if the battery is too low. */
    @Test
    public void testStopsEarly_ifBatteryDrainIsAboveThreshold() throws InitializationError {
        Bundle args = new Bundle();
        args.putString(Microbenchmark.MIN_BATTERY_LEVEL_OPTION, "40");
        args.putString(Microbenchmark.MAX_BATTERY_DRAIN_OPTION, "20");
        Microbenchmark runner = Mockito.spy(new Microbenchmark(LoggingTest.class, args));
        doReturn(80).doReturn(50).when(runner).getBatteryLevel();

        RunNotifier notifier = Mockito.mock(RunNotifier.class);
        runner.run(notifier);

        ArgumentCaptor<Failure> failureCaptor = ArgumentCaptor.forClass(Failure.class);
        verify(notifier).fireTestFailure(failureCaptor.capture());

        Failure failure = failureCaptor.getValue();
        Throwable throwable = failure.getException();
        assertTrue(
                String.format(
                        "Exception was not a TerminateEarlyException. Instead, it was: %s",
                        throwable.getClass()),
                throwable instanceof TerminateEarlyException);
        assertThat(throwable)
                .hasMessageThat()
                .matches("Terminating early.*battery drain.*threshold.");
    }

    /** Test that the microbenchmark will align starting with the battery charge counter. */
    @Test
    public void testAlignWithBatteryChargeCounter() throws InitializationError {
        Bundle args = new Bundle();
        args.putString("align-with-charge-counter", "true");
        args.putString("counter-decrement-timeout_ms", "5000");

        Microbenchmark runner = Mockito.spy(new Microbenchmark(LoggingTest.class, args));
        doReturn(99999)
                .doReturn(99999)
                .doReturn(99999)
                .doReturn(88888)
                .when(runner)
                .getBatteryChargeCounter();
        doReturn(10L).when(runner).getCounterPollingInterval();

        RunNotifier notifier = Mockito.mock(RunNotifier.class);

        Thread thread =
                new Thread(
                        new Runnable() {
                            public void run() {
                                runner.run(notifier);
                            }
                        });

        thread.start();
        SystemClock.sleep(20);
        verify(notifier, never()).fireTestStarted(any(Description.class));
        SystemClock.sleep(20);
        verify(notifier).fireTestStarted(any(Description.class));
    }

    /** Test that the microbenchmark counter alignment will time out if there's no change. */
    @Test
    public void testAlignWithBatteryChargeCounter_timesOut() throws InitializationError {
        Bundle args = new Bundle();
        args.putString("align-with-charge-counter", "true");
        args.putString("counter-decrement-timeout_ms", "30");

        Microbenchmark runner = Mockito.spy(new Microbenchmark(LoggingTest.class, args));
        doReturn(99999).when(runner).getBatteryChargeCounter();
        doReturn(10L).when(runner).getCounterPollingInterval();

        RunNotifier notifier = Mockito.mock(RunNotifier.class);

        Thread thread =
                new Thread(
                        new Runnable() {
                            public void run() {
                                runner.run(notifier);
                            }
                        });

        thread.start();
        SystemClock.sleep(20);
        verify(notifier, never()).fireTestStarted(any(Description.class));
        SystemClock.sleep(30);
        verify(notifier).fireTestStarted(any(Description.class));
    }

    /**
     * Test successive iteration will not be executed when the terminate on test fail
     * option is enabled.
     */
    @Test
    public void testTerminateOnTestFailOptionEnabled() throws InitializationError {
        Bundle args = new Bundle();
        args.putString("iterations", "2");
        args.putString("rename-iterations", "false");
        args.putString("terminate-on-test-fail", "true");
        LoggingMicrobenchmark loggingRunner = new LoggingMicrobenchmark(
                LoggingFailedTest.class, args);
        Result result = new JUnitCore().run(loggingRunner);
        assertThat(result.wasSuccessful()).isFalse();
        assertThat(sLogs)
                .containsExactly(
                        "before",
                        "tight before",
                        "begin: testMethod("
                                + "android.platform.test.microbenchmark.MicrobenchmarkTest"
                                + "$LoggingFailedTest)",
                        "end",
                        "after")
                .inOrder();
    }

    /**
     * Test successive iteration will be executed when the terminate on test fail
     * option is disabled.
     */
    @Test
    public void testTerminateOnTestFailOptionDisabled() throws InitializationError {
        Bundle args = new Bundle();
        args.putString("iterations", "2");
        args.putString("rename-iterations", "false");
        args.putString("terminate-on-test-fail", "false");
        LoggingMicrobenchmark loggingRunner = new LoggingMicrobenchmark(
                LoggingFailedTest.class, args);
        Result result = new JUnitCore().run(loggingRunner);
        assertThat(result.wasSuccessful()).isFalse();
        assertThat(sLogs)
                .containsExactly(
                        "before",
                        "tight before",
                        "begin: testMethod("
                                + "android.platform.test.microbenchmark.MicrobenchmarkTest"
                                + "$LoggingFailedTest)",
                        "end",
                        "after",
                        "before",
                        "tight before",
                        "begin: testMethod("
                                + "android.platform.test.microbenchmark.MicrobenchmarkTest"
                                + "$LoggingFailedTest)",
                        "end",
                        "after")
                .inOrder();
    }

    /** Test dynamic test rule injection. */
    @Test
    public void testDynamicTestRuleInjection() throws InitializationError {
        Bundle args = new Bundle();
        args.putString("iterations", "2");
        args.putString("rename-iterations", "false");
        args.putString("terminate-on-test-fail", "false");
        args.putString(
                Microbenchmark.DYNAMIC_INNER_TEST_RULES_OPTION, LoggingRule1.class.getName());
        args.putString(
                Microbenchmark.DYNAMIC_OUTER_TEST_RULES_OPTION, LoggingRule2.class.getName());
        LoggingMicrobenchmark loggingRunner =
                new LoggingMicrobenchmark(LoggingTestWithRules.class, args);
        new JUnitCore().run(loggingRunner);
        assertThat(sLogs)
                .containsExactly(
                        "hardcoded class rule starting",
                        "logging rule 2 starting",
                        "hardcoded test rule starting",
                        "logging rule 1 starting",
                        "before",
                        "tight before",
                        "begin: testMethod("
                                + "android.platform.test.microbenchmark.MicrobenchmarkTest"
                                + "$LoggingTestWithRules)",
                        "test",
                        "end",
                        "tight after",
                        "after",
                        "logging rule 1 finished",
                        "hardcoded test rule finished",
                        "logging rule 2 finished",
                        "logging rule 2 starting",
                        "hardcoded test rule starting",
                        "logging rule 1 starting",
                        "before",
                        "tight before",
                        "begin: testMethod("
                                + "android.platform.test.microbenchmark.MicrobenchmarkTest"
                                + "$LoggingTestWithRules)",
                        "test",
                        "end",
                        "tight after",
                        "after",
                        "logging rule 1 finished",
                        "hardcoded test rule finished",
                        "logging rule 2 finished",
                        "hardcoded class rule finished")
                .inOrder();
    }

    /** Test dynamic class rule injection. */
    @Test
    public void testDynamicClassRuleInjection() throws InitializationError {
        Bundle args = new Bundle();
        args.putString("iterations", "2");
        args.putString("rename-iterations", "false");
        args.putString("terminate-on-test-fail", "false");
        args.putString(
                Microbenchmark.DYNAMIC_INNER_CLASS_RULES_OPTION, LoggingRule1.class.getName());
        args.putString(
                Microbenchmark.DYNAMIC_OUTER_CLASS_RULES_OPTION, LoggingRule2.class.getName());
        LoggingMicrobenchmark loggingRunner =
                new LoggingMicrobenchmark(LoggingTestWithRules.class, args);
        new JUnitCore().run(loggingRunner);
        assertThat(sLogs)
                .containsExactly(
                        "logging rule 2 starting",
                        "hardcoded class rule starting",
                        "logging rule 1 starting",
                        "hardcoded test rule starting",
                        "before",
                        "tight before",
                        "begin: testMethod("
                                + "android.platform.test.microbenchmark.MicrobenchmarkTest"
                                + "$LoggingTestWithRules)",
                        "test",
                        "end",
                        "tight after",
                        "after",
                        "hardcoded test rule finished",
                        "hardcoded test rule starting",
                        "before",
                        "tight before",
                        "begin: testMethod("
                                + "android.platform.test.microbenchmark.MicrobenchmarkTest"
                                + "$LoggingTestWithRules)",
                        "test",
                        "end",
                        "tight after",
                        "after",
                        "hardcoded test rule finished",
                        "logging rule 1 finished",
                        "hardcoded class rule finished",
                        "logging rule 2 finished")
                .inOrder();
    }

    @Test
    public void testSupportsIterationRenamingWithAndroidXClassAnnotationInclusion()
            throws Exception {
        Bundle args = new Bundle();
        args.putString("iterations", "2");
        args.putString("rename-iterations", "true");
        injectArguments(args);
        Request request =
                new TestRequestBuilder()
                        // Should run because it has the annotation.
                        .addTestClass(
                                "android.platform.test.microbenchmark.MicrobenchmarkTest$"
                                        + "AnnotatedLoggingTest")
                        // Should not run because it does not have the annotation.
                        .addTestClass(
                                "android.platform.test.microbenchmark.MicrobenchmarkTest$"
                                        + "LoggingTest")
                        .addAnnotationInclusionFilter(
                                "android.platform.test.microbenchmark.MicrobenchmarkTest$"
                                        + "TestAnnotation")
                        .build();
        Result result = new JUnitCore().run(request);
        assertThat(result.wasSuccessful()).isTrue();
        assertThat(sLogs)
                .containsExactly(
                        "before",
                        "tight before",
                        "begin: testMethod$1("
                                + "android.platform.test.microbenchmark.MicrobenchmarkTest$"
                                + "AnnotatedLoggingTest)",
                        "test",
                        "end",
                        "tight after",
                        "after",
                        "before",
                        "tight before",
                        "begin: testMethod$2("
                                + "android.platform.test.microbenchmark.MicrobenchmarkTest$"
                                + "AnnotatedLoggingTest)",
                        "test",
                        "end",
                        "tight after",
                        "after")
                .inOrder();
    }

    @Test
    public void testSupportsIterationRenamingWithAndroidXClassAnnotationExclusion()
            throws Exception {
        Bundle args = new Bundle();
        args.putString("iterations", "2");
        args.putString("rename-iterations", "true");
        injectArguments(args);
        Request request =
                new TestRequestBuilder()
                        .addTestClass(
                                "android.platform.test.microbenchmark.MicrobenchmarkTest$"
                                        + "AnnotatedLoggingTest")
                        .addAnnotationExclusionFilter(
                                "android.platform.test.microbenchmark.MicrobenchmarkTest$"
                                        + "TestAnnotation")
                        .build();
        Result result = new JUnitCore().run(request);
        assertThat(result.wasSuccessful()).isTrue();
        assertThat(sLogs).isEmpty();
    }

    private void injectArguments(Bundle extra) {
        Bundle args = mArgumentsBeforeTest.deepCopy();
        args.putAll(extra);
        InstrumentationRegistry.registerInstance(
                InstrumentationRegistry.getInstrumentation(), args);
    }

    /**
     * An extensions of the {@link Microbenchmark} runner that logs the start and end of collecting
     * traces. It also passes the operation log to the provided test {@code Class}, if it is a
     * {@link LoggingTest}. This is used for ensuring the proper order for evaluating test {@link
     * Statement}s.
     */
    public static class LoggingMicrobenchmark extends Microbenchmark {
        public LoggingMicrobenchmark(Class<?> klass) throws InitializationError {
            super(klass);
        }

        LoggingMicrobenchmark(Class<?> klass, Bundle arguments) throws InitializationError {
            super(klass, arguments);
        }

        @Override
        protected TracePointRule getTracePointRule() {
            return new LoggingTracePointRule();
        }

        class LoggingTracePointRule extends TracePointRule {
            @Override
            protected void beginSection(String sectionTag) {
                sLogs.add(String.format("begin: %s", sectionTag));
            }

            @Override
            protected void endSection() {
                sLogs.add("end");
            }
        }
    }

    /**
     * A test that logs {@link Before}, {@link After}, {@link Test}, and the logging {@link
     * TightMethodRule} included, used in conjunction with {@link LoggingMicrobenchmark} to
     * determine all {@link Statement}s are evaluated in the proper order.
     */
    @RunWith(LoggingMicrobenchmark.class)
    public static class LoggingTest {
        @Microbenchmark.TightMethodRule
        public TightRule orderRule = new TightRule();

        @Before
        public void beforeMethod() {
            sLogs.add("before");
        }

        @Test
        public void testMethod() {
            sLogs.add("test");
        }

        @After
        public void afterMethod() {
            sLogs.add("after");
        }

        class TightRule implements TestRule {
            @Override
            public Statement apply(Statement base, Description description) {
                return new Statement() {
                    @Override
                    public void evaluate() throws Throwable {
                        sLogs.add("tight before");
                        base.evaluate();
                        sLogs.add("tight after");
                    }
                };
            }
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface TestAnnotation {}

    @TestAnnotation
    @RunWith(LoggingMicrobenchmark.class)
    public static class AnnotatedLoggingTest extends LoggingTest {}

    public static class LoggingTestWithRules extends LoggingTest {
        @ClassRule
        public static TestRule hardCodedClassRule =
                new TestWatcher() {
                    @Override
                    public void starting(Description description) {
                        sLogs.add("hardcoded class rule starting");
                    }

                    @Override
                    public void finished(Description description) {
                        sLogs.add("hardcoded class rule finished");
                    }
                };

        @Rule
        public TestRule hardCodedRule =
                new TestWatcher() {
                    @Override
                    public void starting(Description description) {
                        sLogs.add("hardcoded test rule starting");
                    }

                    @Override
                    public void finished(Description description) {
                        sLogs.add("hardcoded test rule finished");
                    }
                };
    }

    public static class LoggingFailedTest extends LoggingTest {
        @Test
        public void testMethod() {
            throw new RuntimeException("I failed.");
        }
    }

    public static class LoggingRule1 extends TestWatcher {
        @Override
        public void starting(Description description) {
            sLogs.add("logging rule 1 starting");
        }

        @Override
        public void finished(Description description) {
            sLogs.add("logging rule 1 finished");
        }
    }

    public static class LoggingRule2 extends TestWatcher {
        @Override
        public void starting(Description description) {
            sLogs.add("logging rule 2 starting");
        }

        @Override
        public void finished(Description description) {
            sLogs.add("logging rule 2 finished");
        }
    }
}
