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

package com.android.sts.common;

import static com.google.common.truth.Truth.assertWithMessage;

import com.android.tradefed.testtype.DeviceJUnit4ClassRunner;
import com.android.tradefed.testtype.junit4.BaseHostJUnit4Test;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

/** Unit tests for {@link UserUtils}. */
@RunWith(DeviceJUnit4ClassRunner.class)
public class UserUtilsTest extends BaseHostJUnit4Test {
    private static final String TEST_USER_NAME = "TestUserForUserUtils";
    private static final String CMD_PM_LIST_USERS = "pm list users";

    @Before
    public void setUp() throws Exception {
        assertWithMessage("device already has the test user")
                .that(CommandUtil.runAndCheck(getDevice(), CMD_PM_LIST_USERS).getStdout())
                .doesNotContain(TEST_USER_NAME);
    }

    @After
    public void tearDown() throws Exception {
        assertWithMessage("did not clean up the test user")
                .that(CommandUtil.runAndCheck(getDevice(), CMD_PM_LIST_USERS).getStdout())
                .doesNotContain(TEST_USER_NAME);
    }

    @Test
    public void testUserUtilsNonRoot() throws Exception {
        assertWithMessage("must test with non rootable device")
                .that(getDevice().disableAdbRoot())
                .isTrue();
        try (AutoCloseable user =
                new UserUtils.SecondaryUser(getDevice()).name(TEST_USER_NAME).withUser()) {
            assertWithMessage("device should not implicitly root to create a user")
                    .that(getDevice().isAdbRoot())
                    .isFalse();
            assertWithMessage("did not create the test user")
                    .that(CommandUtil.runAndCheck(getDevice(), CMD_PM_LIST_USERS).getStdout())
                    .contains(TEST_USER_NAME);
        }
        assertWithMessage("device should not implicitly root to cleanup")
                .that(getDevice().isAdbRoot())
                .isFalse();
    }

    @Test
    public void testUserUtilsRoot() throws Exception {
        assertWithMessage("must test with rootable device")
                .that(getDevice().enableAdbRoot())
                .isTrue();
        try (AutoCloseable user =
                new UserUtils.SecondaryUser(getDevice()).name(TEST_USER_NAME).withUser()) {
            assertWithMessage(
                            "device should still be root after user creation if started with root")
                    .that(getDevice().isAdbRoot())
                    .isTrue();
            assertWithMessage("did not create the test user")
                    .that(CommandUtil.runAndCheck(getDevice(), CMD_PM_LIST_USERS).getStdout())
                    .contains(TEST_USER_NAME);
        }
        assertWithMessage("device should still be root after cleanup if started with root")
                .that(getDevice().isAdbRoot())
                .isTrue();
    }

    @Test
    public void testUserUtilsUserRestriction() throws Exception {
        assertWithMessage("must test with rootable device")
                .that(getDevice().enableAdbRoot())
                .isTrue();
        try (AutoCloseable user =
                new UserUtils.SecondaryUser(getDevice())
                        .name(TEST_USER_NAME)
                        .withUserRestrictions(Map.of("test_restriction", "1"))
                        .withUser()) {
            // Exception is thrown if any error occurs while setting user restriction above
        }
        assertWithMessage("device should still be root after cleanup if started with root")
                .that(getDevice().isAdbRoot())
                .isTrue();
    }

    @Test
    public void testUserUtilsSkipSetupWizardNonRoot() throws Exception {
        assertWithMessage("must test with non rootable device")
                .that(getDevice().disableAdbRoot())
                .isTrue();
        try (AutoCloseable user =
                new UserUtils.SecondaryUser(getDevice())
                        .name(TEST_USER_NAME)
                        .doSkipSetupWizard()
                        .withUser()) {
            // Exception is thrown if any error occurs while setting user restriction above
        }
        assertWithMessage("device should not implicitly root to cleanup")
                .that(getDevice().isAdbRoot())
                .isFalse();
    }

    @Test
    public void testUserUtilsSkipSetupWizardRoot() throws Exception {
        assertWithMessage("must test with rootable device")
                .that(getDevice().enableAdbRoot())
                .isTrue();
        try (AutoCloseable user =
                new UserUtils.SecondaryUser(getDevice())
                        .name(TEST_USER_NAME)
                        .doSkipSetupWizard()
                        .withUser()) {
            // Exception is thrown if any error occurs while setting user restriction above
        }
        assertWithMessage("device should still be root after cleanup if started with root")
                .that(getDevice().isAdbRoot())
                .isTrue();
    }
}
