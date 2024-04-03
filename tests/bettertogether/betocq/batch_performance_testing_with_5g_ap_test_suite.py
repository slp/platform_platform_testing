#  Copyright (C) 2024 The Android Open Source Project
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

"""This test suite batches all tests to run in sequence.

This requires a 5G AP to be ready and configured in testbed.
5G AP (wifi_5g_ssid): channel 36 (5180)
"""

import os
import sys

# Allows local imports to be resolved via relative path, so the test can be run
# without building.
_betocq_dir = os.path.dirname(os.path.dirname(__file__))
if _betocq_dir not in sys.path:
  sys.path.append(_betocq_dir)

from mobly import suite_runner

from betocq import base_betocq_suite
from betocq import nc_constants
from betocq.directed_tests import mcc_2g_wfd_indoor_5g_sta_test
from betocq.directed_tests import scc_5g_wfd_sta_test
from betocq.directed_tests import scc_5g_wlan_sta_test
from betocq.directed_tests import scc_indoor_5g_wfd_sta_test


class BetoCq5gApPerformanceTestSuite(base_betocq_suite.BaseBetocqSuite):
  """Add BetoCQ tests which requires a 5G STA AP to run in sequence."""

  def setup_suite(self, config):
    """Add BetoCQ tests which requires a 5G STA AP to the suite."""
    test_parameters = nc_constants.TestParameters.from_user_params(
        config.user_params
    )

    # add directed tests which requires 5G wlan AP - channel 36
    if (
        test_parameters.wifi_5g_ssid
        or test_parameters.use_auto_controlled_wifi_ap
    ):
      self.add_test_class(
          mcc_2g_wfd_indoor_5g_sta_test.Mcc2gWfdIndoor5gStaTest,
          config=config,
      )
      self.add_test_class(
          scc_5g_wfd_sta_test.Scc5gWfdStaTest,
          config=config,
      )
      self.add_test_class(
          scc_5g_wlan_sta_test.Scc5gWifiLanStaTest,
          config=config,
      )
      self.add_test_class(
          scc_indoor_5g_wfd_sta_test.SccIndoor5gWfdStaTest,
          config=config,
      )

if __name__ == '__main__':
  # Use suite_runner's `main`.
  suite_runner.run_suite_class()

