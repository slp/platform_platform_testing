import logging
import re
import threading
from utilities import constants
import os



class VideoRecording:
    """Video recording and saving functions for reporting"""

    def __init__(self, device, class_name):
        self._device = device
        self._class_name = class_name.lower()
        self.thread = None

    # Enable screen recording for device
    def enable_screen_recording(self):
        ENABLE_SCREEN_RECORDING_COMMAND=(
            f'{constants.SCREEN_RECORDING_COMMAND} {self.get_screen_recording_path()}')
        logging.info("Enable screen recording with command  %s: ", ENABLE_SCREEN_RECORDING_COMMAND)

        def spin():
            self._device.adb.shell(ENABLE_SCREEN_RECORDING_COMMAND)

        self.thread = threading.Thread(target=spin)
        self.thread.start()

    # Stop screen recording for device
    def stop_screen_recording(self):
        logging.info("Stop screen recording on %s", self._device)
        self._device.adb.shell(constants.STOP_VIDEO_RECORDING)
        if self.thread is not None:
            self.thread.join()

    # Move recorded video file to logs
    def pull_recording_file(self, log_path):
        logging.info("Move recorded video file from %s to <%s>", self._device,
                     log_path)
        self._device.adb.pull([self.get_screen_recording_path(), log_path])

    # Delete video file from device
    def delete_screen_recording_from_device(self):
        DELETE_SCREEN_RECORDING_FILE = self.get_screen_recording_path()
        logging.info("Deleting file <%s> on device %s",
                     DELETE_SCREEN_RECORDING_FILE, self._device)
        DELETE_SCREEN_RECORDING_COMMAND = f'{constants.DELETE_SCREEN_RECORDING}{DELETE_SCREEN_RECORDING_FILE}'
        self._device.adb.shell(DELETE_SCREEN_RECORDING_COMMAND)

    # Generate screen recording path
    def get_screen_recording_path(self):
        logging.info("Generating Screen Recording Path for %s", self._device)
        m = f'{self._device}'.split('|')[1]
        device = m.split('>')[0]

        RECORDED_VIDEO_FILE_PATH = f'{constants.RECORDED_VIDEO_FILE_LOCATION}{device}{constants.RECORDED_VIDEO_FILE_OUTPUT_FILE}{self._class_name}'
        logging.info("Screen recording for %s is %s", self._device, RECORDED_VIDEO_FILE_PATH)
        return RECORDED_VIDEO_FILE_PATH