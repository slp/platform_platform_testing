#  Copyright (C) 2023 The Android Open Source Project
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

import logging

from bluetooth_test import bluetooth_base_test
from mobly import asserts
from utilities import constants
from utilities.common_utils import CommonUtils
from utilities.main_utils import common_main
from utilities.media_utils import MediaUtils
from utilities.video_utils_service import VideoRecording


class IsMediaStreamPairedWhenRadioLunchedTest(bluetooth_base_test.BluetoothBaseTest):

    def setup_class(self):
        super().setup_class()
        self.media_utils = MediaUtils(self.target, self.discoverer)
        self.common_utils = CommonUtils(self.target, self.discoverer)

    def setup_test(self):
        self.common_utils.grant_local_mac_address_permission()
        logging.info("\tInitializing video services on Target")
        self.video_utils_service_target = VideoRecording(self.target)
        logging.info("Enabling video recording for Target device")
        self.video_utils_service_target.enable_screen_recording()
        self.common_utils.enable_wifi_on_phone_device()

    def test_media_stream_when_radio_lunched(self):
        """Pair Mobile when Mobile device is streaming when Radio launched """
        self.media_utils.open_youtube_music_app()
        self.media_utils.open_media_app_on_hu()
        self.media_utils.open_media_apps_menu()
        asserts.assert_true(self.common_utils.has_ui_element_with_text(constants.RADIO_APP),
                            '<' + constants.RADIO_APP + '> has to be present on HU screen')
        self.media_utils.open_radio_app_on_hu()
        self.media_utils.tune_fm_radio_on_hu(constants.DEFAULT_FM_FREQUENCY)
        self.bt_utils.pair_primary_to_secondary()
        self.media_utils.open_media_app_on_hu()
        current_phone_song_title = self.media_utils.get_song_title_from_phone()
        current_hu_song_title = self.media_utils.get_song_title_from_hu()
        asserts.assert_true(current_phone_song_title == current_hu_song_title,
                            'Invalid song titles. '
                            'Song title on phone device and HU should be the same')

    def teardown_test(self):
        #  Close YouTube Music app
        self.media_utils.close_youtube_music_app()
        self.call_utils.press_home()
        logging.info("Stopping the screen recording on Target")
        self.video_utils_service_target.stop_screen_recording()
        logging.info("Pull the screen recording from Target")
        self.video_utils_service_target.pull_recording_file(self.log_path)
        logging.info("delete the screen recording from the Target")
        self.video_utils_service_target.delete_screen_recording_from_device()
        super().teardown_test()


if __name__ == '__main__':
    common_main()
