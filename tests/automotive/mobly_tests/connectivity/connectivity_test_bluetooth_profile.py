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

class BluetoothProfiles(bluetooth_base_test.BluetoothBaseTest):


    def setup_test(self):
        # Pair the devices
        self.bt_utils.pair_primary_to_secondary()

    def test_bluetooth_profiles(self):
       """Test is validating BT Profiles"""

       # Waiting for bt profiles sync
       self.call_utils.wait_with_log(5)

       # Validate BT Connection State after pairing
       bt_connection_state=self.call_utils.get_bt_connection_status_using_adb_command(self.discoverer)
       logging.info("BT State after pairing : <%s>", bt_connection_state)
       if 'STATE_CONNECTED' in bt_connection_state:
          logging.info('BT State connected')
       else:
          logging.info('BT State disconnected')

       asserts.assert_true('STATE_CONNECTED' in bt_connection_state, 'BT is not paired, correctly')

       # Validate HFP Profile after pairing
       bt_hfp_status_txt = self.call_utils.get_bt_profile_status_using_adb_command(self.discoverer, constants.BLUETOOTH_HFP)
       logging.info("BT HFP Status: <%s>", bt_hfp_status_txt)
       asserts.assert_true('HfpClientDeviceBlock' in bt_hfp_status_txt, 'HFP Profile not mapped')

       # Validate MAP Profile after pairing
       bt_map_status = self.call_utils.get_bt_profile_status_using_adb_command(self.discoverer, constants.BLUETOOTH_MAP)
       logging.info("Getting BT MAP Status: <%s>", bt_map_status)
       asserts.assert_true(bt_map_status, 'MAP Profile not mapped')

       # Validate AVRCP Profile after pairing
       bt_avrcp_status = self.call_utils.get_bt_profile_status_using_adb_command(self.discoverer, constants.BLUETOOTH_AVRCP)
       logging.info("Getting BT AVRCP Status: <%s>", bt_avrcp_status)
       asserts.assert_true(bt_avrcp_status, 'AVRCP Profile not mapped')

    def teardown_test(self):
        # Turn Bluetooth off on both devices.
        logging.info("Running basic test teardown.")
        self.call_utils.press_home()
        self.call_utils.press_phone_home_icon_using_adb_command(self.target)
        self.bt_utils.unpair()
        logging.info("Disable Bluetooth on Discoverer device")
        self.discoverer.mbs.btDisable()
        logging.info("Disable Bluetooth on Target device")
        self.target.mbs.btDisable()

if __name__ == '__main__':
    # Take test args
    common_main()