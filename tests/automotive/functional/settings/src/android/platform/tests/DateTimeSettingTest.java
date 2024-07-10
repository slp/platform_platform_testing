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

import static org.junit.Assume.assumeFalse;

import android.app.time.Capabilities;
import android.app.time.TimeManager;
import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoDateTimeSettingsHelper;
import android.platform.helpers.IAutoSettingHelper;
import android.platform.helpers.SettingsConstants;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;

@RunWith(AndroidJUnit4.class)
public class DateTimeSettingTest {
    private HelperAccessor<IAutoDateTimeSettingsHelper> mDateTimeSettingsHelper;
    private HelperAccessor<IAutoSettingHelper> mSettingHelper;
    private TimeManager mTimeManager;

    private static final boolean IS_AM = true;
    private static final int DEFAULT_WAIT_TIME = 5000;
    private static final int HOUR = 9;
    private static final int MINUTE = 23;
    private static final int YEAR = 2020;
    private static final int MONTH = 3;
    private static final int DAY = 9;
    private static final LocalDate DATE = LocalDate.of(YEAR, MONTH, DAY);

    private static final String FULL_TIME_TWELVE_REGEX = "9:23[ \\x{202f}]AM";
    private static final String FULL_TIME_TWENTYFOUR = "09:23";
    private static final String TIME_ZONE = "Costa Rica";
    private static final String TIME_ZONE_FULL = "GMT-06:00 Central Standard Time";

    public DateTimeSettingTest() throws Exception {
        mDateTimeSettingsHelper = new HelperAccessor<>(IAutoDateTimeSettingsHelper.class);
        mSettingHelper = new HelperAccessor<>(IAutoSettingHelper.class);
        mTimeManager =
                InstrumentationRegistry.getInstrumentation()
                        .getContext()
                        .getSystemService(TimeManager.class);
    }


    @Before
    public void openDateTimeFacet() {
        InstrumentationRegistry.getInstrumentation()
                .getUiAutomation()
                .adoptShellPermissionIdentity("android.permission.MANAGE_TIME_AND_ZONE_DETECTION");
        mSettingHelper.get().openSetting(SettingsConstants.DATE_AND_TIME_SETTINGS);
    }

    @After
    public void goBackToSettingsScreen() {
        InstrumentationRegistry.getInstrumentation()
                .getUiAutomation()
                .dropShellPermissionIdentity();
        mSettingHelper.get().goBackToSettingsScreen();
    }

    @Test
    public void testSetDate() {
        assumeFalse(checkCapabilitiesNotPossessedAndAutoLocalTimeEnabled());
        mDateTimeSettingsHelper.get().setDate(DATE);
        assertTrue(mDateTimeSettingsHelper.get().getDate().equals(DATE));
    }

    @Test
    public void testSetTimeTwelveHourFormat() {
        assumeFalse(checkCapabilitiesNotPossessedAndAutoLocalTimeEnabled());
        mDateTimeSettingsHelper.get().setTimeInTwelveHourFormat(HOUR, MINUTE, IS_AM);
        assertTrue(mDateTimeSettingsHelper.get().getTime().matches(FULL_TIME_TWELVE_REGEX));
    }

    @Test
    public void testSetTimeTwentyFourHourFormat() {
        assumeFalse(checkCapabilitiesNotPossessedAndAutoLocalTimeEnabled());
        mDateTimeSettingsHelper.get().setTimeInTwentyFourHourFormat(HOUR, MINUTE);
        assertTrue(mDateTimeSettingsHelper.get().getTime().equals(FULL_TIME_TWENTYFOUR));
    }

    @Test
    public void testSetTimeZone() {
        assumeFalse(checkCapabilitiesNotPossessedAndAutoLocalTimeEnabled());
        mDateTimeSettingsHelper.get().setTimeZone(TIME_ZONE);
        assertTrue(mDateTimeSettingsHelper.get().getTimeZone().equals(TIME_ZONE_FULL));
    }

    @Test
    public void testSetTwentyFourHourFormat() {
        assumeFalse(checkCapabilitiesNotPossessedAndAutoLocalTimeEnabled());
        if (!mDateTimeSettingsHelper.get().isTwentyFourHourFormatEnabled()) {
            mDateTimeSettingsHelper.get().toggleTwentyFourHourFormatSwitch();
        }
        String currentUiTime = mDateTimeSettingsHelper.get().getTime();
        assertTrue(currentUiTime.indexOf("AM") == -1 && currentUiTime.indexOf("PM") == -1);
    }

    @Test
    public void testSetTwelveHourFormat() {
        assumeFalse(checkCapabilitiesNotPossessedAndAutoLocalTimeEnabled());
        if (mDateTimeSettingsHelper.get().isTwentyFourHourFormatEnabled()) {
            mDateTimeSettingsHelper.get().toggleTwentyFourHourFormatSwitch();
        }
        String currentUiTime = mDateTimeSettingsHelper.get().getTime();
        assertTrue(currentUiTime.indexOf("AM") != -1 || currentUiTime.indexOf("PM") != -1);
    }

    private boolean checkCapabilitiesNotPossessedAndAutoLocalTimeEnabled() {
        return (mTimeManager.getTimeCapabilitiesAndConfig()
                                        .getCapabilities()
                                        .getConfigureAutoDetectionEnabledCapability()
                                != Capabilities.CAPABILITY_POSSESSED
                        && mTimeManager.getTimeCapabilitiesAndConfig()
                                .getConfiguration()
                                .isAutoDetectionEnabled())
                || (mTimeManager.getTimeZoneCapabilitiesAndConfig()
                                        .getCapabilities()
                                        .getConfigureAutoDetectionEnabledCapability()
                                != Capabilities.CAPABILITY_POSSESSED
                        && mTimeManager.getTimeZoneCapabilitiesAndConfig()
                                .getConfiguration()
                                .isAutoDetectionEnabled());
    }
}
