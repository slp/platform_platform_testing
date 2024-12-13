"""
  Copyright (C) 2023 The Android Open Source Project

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
    1. Navigate to the Bluetooth settings page
    2. Disconnect - Connect Mobile Device via Layer1 - via Bluetooth Button
    3. Tap Device to see DisconnectedStatus in Layer2
    4. Reconnect - Via layer 1
    5. Tap device to see Connected status in Layer2

    "Layer Two" represents the device-specific screen (the screen you see when clicking the device in the bluetooth settings page.

"""

from bluetooth_test import bluetooth_base_test
from mobly import asserts
from utilities.main_utils import common_main
from utilities import constants
import logging

MOBILE_DEVICE_NAME = 'target'
AUTOMOTIVE_DEVICE_NAME = 'discoverer'

class BluetoothConnectionStatusOnLevelTwo(bluetooth_base_test.BluetoothBaseTest):


    def setup_test(self):
        # Pair the devices
        self.bt_utils.pair_primary_to_secondary()
        super().enable_recording()

    def test_connection_status_displayed_on_device_screen(self):
        # Log BT Connection State after pairing
        bt_connection_state=self.call_utils.get_bt_connection_status_using_adb_command(self.discoverer)
        logging.info("BT State after pairing : <%s>", bt_connection_state)

        # Open bluetooth settings.
        self.call_utils.open_bluetooth_settings()

        # Find the target device and disconnect it on the Level One page
        target_name = self.target.mbs.btGetName()
        self.call_utils.press_bluetooth_toggle_on_device(target_name)
        self.call_utils.wait_with_log(10)

        # Click on the target device.
        self.discoverer.mbs.pressDeviceInBluetoothSettings(target_name)

        # Confirm that target device displays "disconnected"
        self.call_utils.wait_with_log(10)
        summary = self.discoverer.mbs.getDeviceSummary()
        asserts.assert_true(
            constants.DISCONNECTED_SUMMARY_STATUS in summary,
            "Expected \'%s\' in device summary after disconnecting, but found none"
            % constants.DISCONNECTED_SUMMARY_STATUS
        )


if __name__ == '__main__':
    # Take test args
    common_main()