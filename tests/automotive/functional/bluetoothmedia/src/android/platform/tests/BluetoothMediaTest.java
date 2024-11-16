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

import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoAppGridHelper;
import android.platform.helpers.IAutoMediaHelper;

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BluetoothMediaTest {
    private HelperAccessor<IAutoAppGridHelper> mAppGridHelper;
    private HelperAccessor<IAutoMediaHelper> mAutoMediaHelper;

    public BluetoothMediaTest() throws Exception {
        mAppGridHelper = new HelperAccessor<>(IAutoAppGridHelper.class);
        mAutoMediaHelper = new HelperAccessor<>(IAutoMediaHelper.class);
    }

    @Before
    public void openAppGrid() {
        // Open the APP Grid
        mAppGridHelper.get().open();
    }

    @After
    public void goBackToHomeScreen() {
        mAppGridHelper.get().goToHomePage();
    }

    @Test
    public void testBluetoothMediaDefaultState() {
        mAppGridHelper.get().openApp("Bluetooth Audio");
        assertTrue(
                "Bluetooth audio disconnected label is not present",
                mAutoMediaHelper.get().isBluetoothAudioDisconnectedLabelVisible());
        assertTrue(
                "Connect to Bluetooth label is not present",
                mAutoMediaHelper.get().isConnectToBluetoothLabelVisible());
    }
}
