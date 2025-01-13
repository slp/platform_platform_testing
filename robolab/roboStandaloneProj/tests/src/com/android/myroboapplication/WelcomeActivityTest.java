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

package com.android.myroboapplication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import android.content.Intent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
public class WelcomeActivityTest {

    @Before
    public void setup() throws Exception {}

    @Test
    public void clickingLogin_shouldStartLoginActivity() throws Exception {
        System.out.println("\n\nSystem Properties:");
        System.getProperties().forEach((key, value) -> System.out.println(key + ": " + value));
        assertNotEquals(System.getProperty("robolectric.strict.mode"), "true");

       ActivityController<WelcomeActivity> controller =
                Robolectric.buildActivity(WelcomeActivity.class);
        controller.setup(); // Moves Activity to RESUMED state
        WelcomeActivity activity = controller.get();
        activity.findViewById(R.id.login).performClick();
        Intent expectedIntent = new Intent(activity, LoginActivity.class);
        Intent actual = ShadowApplication.getInstance().getNextStartedActivity();
        assertEquals(expectedIntent.getComponent(), actual.getComponent());
    }
}
