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
import android.platform.helpers.IAutoHomeHelper;
import android.platform.helpers.IAutoSettingHelper;
import android.platform.helpers.IAutoUserHelper;
import android.platform.helpers.MultiUserHelper;
import android.platform.helpers.SettingsConstants;
import android.platform.scenario.multiuser.MultiUserConstants;
import android.platform.helpers.AutomotiveConfigConstants;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ProfileIconTest {

    private static final String USER_NAME = MultiUserConstants.GUEST_NAME;
    private static final String GUEST = AutomotiveConfigConstants.HOME_GUEST_BUTTON;

    private final MultiUserHelper mMultiUserHelper = MultiUserHelper.getInstance();

    private HelperAccessor<IAutoUserHelper> mUsersHelper;
    private HelperAccessor<IAutoSettingHelper> mSettingHelper;
    private HelperAccessor<IAutoHomeHelper> mHomeHelper;

    public ProfileIconTest() {
        mHomeHelper = new HelperAccessor<>(IAutoHomeHelper.class);
        mUsersHelper = new HelperAccessor<>(IAutoUserHelper.class);
        mSettingHelper = new HelperAccessor<>(IAutoSettingHelper.class);
    }

    @Test
    public void testToVerifyGuestProfile() throws Exception {
        mUsersHelper.get().switchUsingUserIcon(GUEST);
        mSettingHelper.get().openSetting(SettingsConstants.PROFILE_ACCOUNT_SETTINGS);
        assertTrue(
                "Failed to switch from current user to Guest Profile.",
                mUsersHelper.get().getProfileNameFromSettings().contains(USER_NAME));
    }
}
