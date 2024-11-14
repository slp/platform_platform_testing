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
import android.platform.helpers.IAutoHomeHelper;
import android.platform.helpers.IAutoSettingHelper;
import android.platform.helpers.IAutoStatusBarHelper;

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class LaunchQuickControlsTest {

    private HelperAccessor<IAutoHomeHelper> mHomeHelper;
    private HelperAccessor<IAutoSettingHelper> mSettingHelper;
    private HelperAccessor<IAutoStatusBarHelper> mStatusBarHelper;

    public LaunchQuickControlsTest() {
        mHomeHelper = new HelperAccessor<>(IAutoHomeHelper.class);
        mSettingHelper = new HelperAccessor<>(IAutoSettingHelper.class);
        mStatusBarHelper = new HelperAccessor<>(IAutoStatusBarHelper.class);
    }

    @Before
    public void setup() {
        mHomeHelper.get().open();
    }

    @After
    public void goBackToHomeScreen() {
        mSettingHelper.get().exit();
    }

    @Test
    public void testQuickControlsOnStatusBar() {
        // Ensure the quick controls are opened twice, in a random order
        mStatusBarHelper.get().openBluetoothPalette();
        assertTrue("Bluetooth Palatte did not open", mStatusBarHelper.get().hasBluetoothSwitch());
        mStatusBarHelper.get().openNetworkPalette();
        assertTrue(
                "Network Palette did not open",
                mSettingHelper.get().checkMenuExists("Network & internet settings"));
        mHomeHelper.get().openBrightnessPalette();
        assertTrue(
                "Brightness palette did not open", mHomeHelper.get().hasDisplayBrightessPalette());
        mStatusBarHelper.get().openSoundPaletteOnStatusBar();
        assertTrue(
                "Sound Palette did not open",
                mStatusBarHelper
                        .get()
                        .isUIButtonPresentOnSoundPalette(
                                AutomotiveConfigConstants.SOUND_SETTING_INCALL));
        mStatusBarHelper.get().openNetworkPalette();
        assertTrue(
                "Network Palette did not open",
                mSettingHelper.get().checkMenuExists("Network & internet settings"));
        mStatusBarHelper.get().openSoundPaletteOnStatusBar();
        assertTrue(
                "Sound Palette did not open",
                mStatusBarHelper
                        .get()
                        .isUIButtonPresentOnSoundPalette(
                                AutomotiveConfigConstants.SOUND_SETTING_INCALL));
        mStatusBarHelper.get().openBluetoothPalette();
        assertTrue("Bluetooth Palatte did not open", mStatusBarHelper.get().hasBluetoothSwitch());
        mHomeHelper.get().openBrightnessPalette();
        assertTrue(
                "Brightness palette did not open", mHomeHelper.get().hasDisplayBrightessPalette());
        mStatusBarHelper.get().openSoundPaletteOnStatusBar();
        assertTrue(
                "Sound Palette did not open",
                mStatusBarHelper
                        .get()
                        .isUIButtonPresentOnSoundPalette(
                                AutomotiveConfigConstants.SOUND_SETTING_INCALL));
    }
}
