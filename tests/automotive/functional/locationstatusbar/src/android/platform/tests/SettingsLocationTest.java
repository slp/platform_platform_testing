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

package android.platform.tests;

import static junit.framework.Assert.assertTrue;

import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoSettingHelper;
import android.platform.helpers.IAutoSettingsLocationHelper;
import android.platform.helpers.SettingsConstants;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.SetFlagsRule;
import android.platform.test.rules.ConditionalIgnore;
import android.platform.test.rules.ConditionalIgnoreRule;
import android.platform.test.rules.IgnoreOnPortrait;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SettingsLocationTest {
    @Rule public ConditionalIgnoreRule rule = new ConditionalIgnoreRule();

    private HelperAccessor<IAutoSettingsLocationHelper> mSettingLocationHelper;
    private HelperAccessor<IAutoSettingHelper> mSettingHelper;

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    public SettingsLocationTest() {
        mSettingHelper = new HelperAccessor<>(IAutoSettingHelper.class);
        mSettingLocationHelper = new HelperAccessor<>(IAutoSettingsLocationHelper.class);
    }

    @Before
    public void setup() {
        mSettingHelper.get().openSetting(SettingsConstants.LOCATION_SETTINGS);
        assertTrue(
                "Location settings did not open", mSettingHelper.get().checkMenuExists("Location"));
    }

    @Test
    @ConditionalIgnore(condition = IgnoreOnPortrait.class)
    @RequiresFlagsEnabled(
            com.android.car.settings.Flags.FLAG_REQUIRED_INFOTAINMENT_APPS_SETTINGS_PAGE)
    public void testToVerifyToggleLocation() {
        mSettingLocationHelper.get().locationAccess();
        boolean defaultState = mSettingLocationHelper.get().isLocationOn();
        String widgetShownMessage = "Location widget is displayed ";
        String widgetNotShownMessage = "Location widget is not displayed ";
        mSettingLocationHelper.get().toggleLocation(!defaultState);
        assertTrue(
                defaultState ? widgetShownMessage : widgetNotShownMessage,
                mSettingLocationHelper.get().hasMapsWidget() != defaultState);
        mSettingLocationHelper.get().toggleLocation(defaultState);
        assertTrue(
                defaultState ? widgetShownMessage : widgetNotShownMessage,
                mSettingLocationHelper.get().hasMapsWidget() == defaultState);
    }

    @Test
    public void testToCheckRecentlyAccessedOption() {
        assertTrue(
                "Recently accessed option is not displayed ",
                mSettingLocationHelper.get().hasRecentlyAccessed());
    }
}
