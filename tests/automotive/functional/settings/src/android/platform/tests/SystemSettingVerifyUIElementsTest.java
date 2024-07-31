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
import android.platform.helpers.IAutoSettingHelper;
import android.platform.helpers.IAutoSystemSettingsHelper;
import android.platform.helpers.IAutoUISettingsHelper;
import android.platform.helpers.SettingsConstants;

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SystemSettingVerifyUIElementsTest {
    private HelperAccessor<IAutoSettingHelper> mSettingHelper;
    private HelperAccessor<IAutoUISettingsHelper> mSettingsUIHelper;

    public SystemSettingVerifyUIElementsTest() throws Exception {
        mSettingHelper = new HelperAccessor<>(IAutoSettingHelper.class);
        mSettingsUIHelper = new HelperAccessor<>(IAutoUISettingsHelper.class);
    }

    @Before
    public void openSystemFacet() {
        mSettingHelper.get().openSetting(SettingsConstants.SYSTEM_SETTINGS);
        assertTrue(
                "System settings did not open",
                mSettingHelper.get().checkMenuExists("Languages & input"));
    }

    @After
    public void goBackToSettingsScreen() {
        mSettingHelper.get().goBackToSettingsScreen();
    }

    @Test
    public void testLanguagesinputSystemSettings() {
        mSettingsUIHelper.get().openUIOptions(AutomotiveConfigConstants.LANGUAGES_INPUT_IN_SYSTEM);

        assertTrue(
                "Languages displayed in Languages & input",
                mSettingsUIHelper.get().hasUIElement(AutomotiveConfigConstants.LANGUAGES_MENU));
        assertTrue(
                "Autofill service displayed in Languages & input",
                mSettingsUIHelper
                        .get()
                        .hasUIElement(
                                AutomotiveConfigConstants
                                        .LANGUAGE_SYSTEM_SETTINGS_AUTOFILL_SERVICE));
        assertTrue(
                "Keyboard service displayed in Languages & input",
                mSettingsUIHelper
                        .get()
                        .hasUIElement(AutomotiveConfigConstants.LANGUAGE_SYSTEM_SETTINGS_KEYBOARD));
        assertTrue(
                "Text to speechoutput displayed in Languages & input",
                mSettingsUIHelper
                        .get()
                        .hasUIElement(
                                AutomotiveConfigConstants
                                        .LANGUAGE_SYSTEM_SETTINGS_TEXT_TO_SPEECH_OUTPUT));
    }

    @Test
    public void testUnitSystemSettings() {
        mSettingsUIHelper.get().openUIOptions(AutomotiveConfigConstants.SYSTEM_SETTINGS_UNITS);
        assertTrue(
                "Speed displayed in Units",
                mSettingsUIHelper
                        .get()
                        .hasUIElement(AutomotiveConfigConstants.UNIT_SYSTEM_SETTINGS_SPEED));
        assertTrue(
                "Distance displayed in Units",
                mSettingsUIHelper
                        .get()
                        .hasUIElement(AutomotiveConfigConstants.UNIT_SYSTEM_SETTINGS_DISTANCE));
        assertTrue(
                "Temperature displayed in Units",
                mSettingsUIHelper
                        .get()
                        .hasUIElement(AutomotiveConfigConstants.UNIT_SYSTEM_SETTINGS_TEMPERATURE));
        assertTrue(
                "Pressure displayed in Units",
                mSettingsUIHelper
                        .get()
                        .hasUIElement(AutomotiveConfigConstants.UNIT_SYSTEM_SETTINGS_PRESSURE));
    }
}
