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

import android.platform.test.microbenchmark.Microbenchmark.NoMetricAfter;
import android.platform.test.microbenchmark.Microbenchmark.NoMetricBefore;
import android.platform.test.rule.TestWatcher;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for the {@link Functional} runner.
 */
@RunWith(JUnit4.class)
public final class FunctionalTest {
    // Static logs are needed to validate dynamic rules and tests that use TestRequestBuilder, where
    // objects are instantiated with reflection and not directly accessible.
    private static List<String> sLogs = new ArrayList<>();

    @Before
    public void before() {
        sLogs.clear();
    }

    @After
    public void after() {
        sLogs.clear();
    }

    @Test
    public void successTest_reportsSuccess() throws InitializationError {
        Functional runner = new Functional(LoggingTest.class);

        Result result = new JUnitCore().run(runner);

        assertThat(result.wasSuccessful()).isTrue();
        assertThat(sLogs)
                .containsExactly(
                        "@NoMetricRule starting",
                        "@Rule starting",
                        "@NoMetricBefore",
                        "@Before",
                        "@Test method body",
                        "@NoMetricAfter",
                        "@After",
                        "@Rule finished",
                        "@NoMetricRule finished")
                .inOrder();
    }
    @Test
    public void failedTest_reportsFailure() throws InitializationError {
        Functional runner = new Functional(LoggingFailedTest.class);

        Result result = new JUnitCore().run(runner);

        assertThat(result.wasSuccessful()).isFalse();
        assertThat(sLogs)
                .containsExactly(
                        "@NoMetricRule starting",
                        "@Rule starting",
                        "@NoMetricBefore",
                        "@Before",
                        "@NoMetricAfter",
                        "@After",
                        "@Rule finished",
                        "@NoMetricRule finished")
                .inOrder();
    }

    @Test
    public void testNoMetricBeforeFailure_reportsFailedTest() throws InitializationError {
        Functional runner = new Functional(LoggingNoMetricBeforeFailure.class);

        Result result = new JUnitCore().run(runner);

        assertThat(result.wasSuccessful()).isFalse();
        assertThat(result.getRunCount()).isEqualTo(1);
        assertThat(sLogs)
                .containsExactly(
                        "@NoMetricRule starting",
                        "@Rule starting",
                        "@NoMetricAfter",
                        "@After",
                        "@Rule finished",
                        "@NoMetricRule finished")
                .inOrder();
    }

    @Test
    public void testNoMetricAfterFailure_runsTestBodyAndReportsFailedTest() throws InitializationError {
        Functional runner = new Functional(LoggingNoMetricAfterFailure.class);

        Result result = new JUnitCore().run(runner);

        assertThat(result.wasSuccessful()).isFalse();
        assertThat(result.getRunCount()).isEqualTo(1);
        assertThat(sLogs)
                .containsExactly(
                        "@NoMetricRule starting",
                        "@Rule starting",
                        "@NoMetricBefore",
                        "@Before",
                        "@Test method body",
                        "@NoMetricAfter",
                        "@After",
                        "@Rule finished",
                        "@NoMetricRule finished")
                .inOrder();
    }

    /**
     * A test that logs {@link Before}, {@link After}, {@link Test} included,
     * used in conjunction with {@link Functional} to
     * determine all {@link Statement}s are evaluated in the proper order.
     */
    @RunWith(Functional.class)
    public static class LoggingTest {
        @Microbenchmark.NoMetricRule
        public NoMetricRule noMetricRule = new NoMetricRule();

        @Rule
        public LoggingRule loggingRule = new LoggingRule();

        @NoMetricBefore
        public void noMetricBeforeMethod() {
            sLogs.add("@NoMetricBefore");
        }

        @Before
        public void beforeMethod() {
            sLogs.add("@Before");
        }

        @Test
        public void testMethod() {
            sLogs.add("@Test method body");
        }

        @After
        public void afterMethod() {
            sLogs.add("@After");
        }

        @NoMetricAfter
        public void noMetricAfterMethod() {
            sLogs.add("@NoMetricAfter");
        }
    }

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

    public static class LoggingTestCreationFailure extends LoggingTest {
        public LoggingTestCreationFailure() {
            throw new RuntimeException("I failed.");
        }
    }

    public static class LoggingNoMetricBeforeFailure extends LoggingTest {
        @NoMetricBefore
        public void noMetricBeforeFailure() {
            throw new RuntimeException("I failed.");
        }
    }

    public static class LoggingNoMetricAfterFailure extends LoggingTest {
        @NoMetricAfter
        public void noMetricAfterFailure() {
            throw new RuntimeException("I failed.");
        }
    }

    public static class LoggingRule extends TestWatcher {
        @Override
        public void starting(Description description) {
            sLogs.add("@Rule starting");
        }

        @Override
        public void finished(Description description) {
            sLogs.add("@Rule finished");
        }
    }

    public static class NoMetricRule extends TestWatcher {
        @Override
        public void starting(Description description) {
            sLogs.add("@NoMetricRule starting");
        }

        @Override
        public void finished(Description description) {
            sLogs.add("@NoMetricRule finished");
        }
    }
}
