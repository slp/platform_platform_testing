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

package android.platform.helpers;

public interface IMeetHelper extends IAppHelper {
    /**
     * Setup expectations: N/A.
     *
     * <p>This method will check if device is currently on the main page of Meet app by looking for the button object for starting a new meeting.
     *
     * @return true if the button is found and clickable, false otherwise
     */
    public boolean isOnMeetMainPage();

    /**
     * Setup expectations: N/A.
     *
     * <p>This method will check if device is currently in a meeting of Meet app by looking for the button object for leaving the meeting.
     *
     * @return true if the button is found and clickable, false otherwise
     */
    public boolean isInMeetingCall();

    /**
     * Setup expectations: Meet is open and on its main page.
     *
     * <p>This method will create and join a new meeting.
     */
    public void startMeeting();

    /**
     * Setup expectations: Meet is open and already in a meeting.
     *
     * <p>This method will leave the new meeting and back to main page.
     */
    public void leaveMeeting();

}
