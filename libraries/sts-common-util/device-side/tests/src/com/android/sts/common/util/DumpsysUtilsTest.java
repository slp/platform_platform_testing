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

package com.android.sts.common.util.tests;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import static com.android.compatibility.common.util.SystemUtil.runShellCommand;
import static com.android.sts.common.SystemUtil.poll;

import static com.google.common.truth.Truth.assertWithMessage;

import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.Until;

import com.android.sts.common.DumpsysUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Unit tests for {@link DumpsysUtils}. */
public class DumpsysUtilsTest {
    private String mActivityName;
    private UiDevice mUiDevice;
    private BySelector mSelector;

    @Before
    public void setUp() {
        runShellCommand("input keyevent KEYCODE_WAKEUP");
        runShellCommand("wm dismiss-keyguard");
        runShellCommand("input keyevent KEYCODE_HOME");

        Instrumentation instrumentation = getInstrumentation();
        Context context = instrumentation.getContext();
        mUiDevice = UiDevice.getInstance(instrumentation);
        Intent intent =
                new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ComponentName componentName = intent.resolveActivity(context.getPackageManager());
        mActivityName = componentName.flattenToString();
        mSelector = By.pkg(componentName.getPackageName());
        context.startActivity(intent);
    }

    @After
    public void tearDown() throws Exception {
        mUiDevice.pressHome();
        mUiDevice.wait(Until.gone(mSelector), 5_000L /* timeout */);
    }

    @Test
    public void testActivityResumed() throws Exception {
        assertWithMessage("Activity was not resumed")
                .that(poll(() -> DumpsysUtils.isActivityResumed(mActivityName)))
                .isTrue();
    }

    @Test
    public void testActivityVisible() throws Exception {
        assertWithMessage("Activity was not visible")
                .that(poll(() -> DumpsysUtils.isActivityVisible(mActivityName)))
                .isTrue();
    }

    @Test
    public void testActivityLaunched() throws Exception {
        assertWithMessage("Activity was not launched")
                .that(poll(() -> DumpsysUtils.isActivityLaunched(mActivityName)))
                .isTrue();
    }
}
