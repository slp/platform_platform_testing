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
package android.platform.tests;

import static junit.framework.Assert.assertTrue;

import android.platform.helpers.AutomotiveConfigConstants;
import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoAppGridHelper;
import android.platform.helpers.IAutoGooglePlayHelper;

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AppsLaunchTest {

    private HelperAccessor<IAutoAppGridHelper> mAppGridHelper;
    private HelperAccessor<IAutoGooglePlayHelper> mGooglePlayHelper;

    public AppsLaunchTest() {
        mAppGridHelper = new HelperAccessor<>(IAutoAppGridHelper.class);
        mGooglePlayHelper = new HelperAccessor<>(IAutoGooglePlayHelper.class);
    }

    @Before
    public void exitAppGrid() {
        // Make sure app grid is not open before testing.
        mAppGridHelper.get().exit();
    }

    @After
    public void goBackToHomeScreen() {
        mAppGridHelper.get().goToHomePage();
    }

    @Test
    public void testOpenBluetoothMediaAppFromAppGrid() {
        // Open the APP Grid
        mAppGridHelper.get().open();
        assertTrue("App Grid is not open.", mAppGridHelper.get().isAppInForeground());
        mAppGridHelper.get().openApp("Bluetooth Audio");
        assertTrue(
                "Media app is not opened",
                mAppGridHelper
                        .get()
                        .checkPackageInForeground(AutomotiveConfigConstants.MEDIA_CENTER_PACKAGE));
    }

    @Test
    public void testOpenSMSAppFromAppGrid() {
        // Open the APP Grid
        mAppGridHelper.get().open();
        assertTrue("App Grid is not open.", mAppGridHelper.get().isAppInForeground());
        mAppGridHelper.get().openApp("SMS");
        assertTrue(
                "SMS app is not opened",
                mAppGridHelper
                        .get()
                        .checkPackageInForeground(AutomotiveConfigConstants.SMS_PACKAGE));
    }

    @Test
    public void testOpenPlayStoreAppFromAppGrid() {
        // Open the APP Grid
        mAppGridHelper.get().open();
        assertTrue("App Grid is not open.", mAppGridHelper.get().isAppInForeground());
        mAppGridHelper.get().openApp("Play Store");
        assertTrue("Play Store app is not opened", mGooglePlayHelper.get().isAppInForeground());
    }

    @Test
    public void testOpenNewsAppFromAppGrid() {
        // Open the APP Grid
        mAppGridHelper.get().open();
        assertTrue("App Grid is not open.", mAppGridHelper.get().isAppInForeground());
        mAppGridHelper.get().openApp("News");
        assertTrue(
                "News app is not opened",
                mAppGridHelper
                        .get()
                        .checkPackageInForeground(AutomotiveConfigConstants.NEWS_PACKAGE));
    }
}
