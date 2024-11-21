"""
  Copyright (C) 2024 The Android Open Source Project

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.



  Test Steps:
  (0. Flash device)
  1. Verify by default BT should be ON always
  2. Disable Phone Button from Bluetooth Settings Page
  3. Disable Bluetooth Button and enable Bluetooth
  4. Verify After reconnecting Bluetooth - Phone-HFP remains disabled and not displayed

"""

import logging

from mobly import asserts
from utilities.main_utils import common_main
from bluetooth_test import bluetooth_base_test
from utilities import constants

class BluetoothDisablePhoneAfterReconnectTest(bluetooth_base_test.BluetoothBaseTest):

    def setup_test(self):
        # Pair the devices
        self.bt_utils.pair_primary_to_secondary()
        super().enable_recording()
        self.call_utils.press_home()

    def test_disable_enable_phone(self):
        # Log BT Connection State after pairing
        bt_connection_state=self.call_utils.get_bt_connection_status_using_adb_command(self.discoverer)
        logging.info("BT State after pairing : <%s>", bt_connection_state)

        # Navigate to the bluetooth settings page
        self.call_utils.open_bluetooth_settings_form_status_bar()
        target_name = self.target.mbs.btGetName()
        # Disable phone for the listed paired device via the preference button
        self.call_utils.press_phone_toggle_on_device(target_name)
        self.call_utils.wait_with_log(5)
        # Confirm that the phone button is unchecked
        asserts.assert_false(
            self.discoverer.mbs.isPhonePreferenceChecked(),
            "Expected phone button to be unchecked after pressing it.")

        # Tap Bluetooth button to Disable Bluetooth
        self.call_utils.press_bluetooth_toggle_on_device(self.target.mbs.btGetName())
        self.call_utils.wait_with_log(5)
        # Tap Grey Bluetooth Button to Enable Bluetooth
        self.call_utils.press_bluetooth_toggle_on_device(self.target.mbs.btGetName())
        self.call_utils.wait_with_log(10)
        # After reconnecting Bluetooth - Confirm that the phone button is unchecked
        asserts.assert_false(
            self.discoverer.mbs.isPhonePreferenceChecked(),
            "Expected phone button to be unchecked after pressing it.")

        self.call_utils.wait_with_log(constants.DEFAULT_WAIT_TIME_FIVE_SECS)

        # Go back to the bluetooth settings page and enable phone via the preference button
        self.call_utils.press_home()
        self.call_utils.open_bluetooth_settings()
        self.call_utils.press_phone_toggle_on_device(target_name)

        # Confirm that the phone button is re-enabled
        asserts.assert_true(
            self.discoverer.mbs.isPhonePreferenceChecked(),
            "Expected phone button to be checked after pressing it a second time.")

if __name__ == '__main__':
    # Take test args
    common_main()