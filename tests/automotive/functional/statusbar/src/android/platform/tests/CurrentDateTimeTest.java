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

import static junit.framework.Assert.assertEquals;

import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoStatusBarHelper;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CurrentDateTimeTest {

    private HelperAccessor<IAutoStatusBarHelper> mStatusBarHelper;

    public CurrentDateTimeTest() {
        mStatusBarHelper = new HelperAccessor<>(IAutoStatusBarHelper.class);
    }

    @Test
    public void testCurrentTime() {
        assertEquals(
                "Current local Time",
                mStatusBarHelper.get().getClockTime(),
                mStatusBarHelper
                        .get()
                        .getCurrentTimeWithTimeZone(mStatusBarHelper.get().getCurrentTimeZone()));
    }

    @Test
    public void testCurrentTimeZone() {
        assertEquals(
                "Current local Time Zone",
                mStatusBarHelper.get().getCurrentTimeZone(),
                mStatusBarHelper.get().getDeviceCurrentTimeZone());
    }
}
