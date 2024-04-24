import logging
import threading
from utilities import constants


class VideoRecording:
    """Video recording and saving functions for reporting"""

    def __init__(self, device):
        self._device = device
        self.thread = None

    # Enable screen recording for device
    def enable_screen_recording(self):
        logging.info("Enable screen recording on %s", self._device)

        def spin():
            self._device.adb.shell(constants.ENABLE_SCREEN_RECORDING)

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
        self._device.adb.pull([constants.RECORDED_VIDEO_FILE_PATH, log_path])

    # Delete video file from device
    def delete_screen_recording_from_device(self):
        logging.info("Deleting file <%s> on device %s",
                     constants.RECORDED_VIDEO_FILE_PATH, self._device)
        self._device.adb.shell(constants.DELETE_SCREEN_RECORDING_FILE)
