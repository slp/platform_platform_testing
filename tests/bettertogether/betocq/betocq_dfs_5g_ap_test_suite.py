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

This requires a DFS 5G AP to be ready and configured in testbed.
DFS 5G AP(wifi_dfs_5g_ssid): channel 52 (5260)
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

from betocq.compound_tests import scc_5g_all_wifi_sta_test
from betocq.directed_tests import mcc_5g_hotspot_dfs_5g_sta_test
from betocq.directed_tests import mcc_5g_wfd_dfs_5g_sta_test
from betocq.directed_tests import scc_dfs_5g_hotspot_sta_test
from betocq.directed_tests import scc_dfs_5g_wfd_sta_test


class BetoCqPerformanceTestSuite(base_betocq_suite.BaseBetocqSuite):
  """Add all BetoCQ tests to run in sequence."""

  def setup_suite(self, config):
    """Add all BetoCQ tests to the suite."""
    test_parameters = nc_constants.TestParameters.from_user_params(
        config.user_params
    )
    # add directed/cuj tests which requires DFS 5G wlan AP - channel 52
    if (
        test_parameters.wifi_dfs_5g_ssid
        or test_parameters.use_auto_controlled_wifi_ap
    ):
      self.add_test_class(
          mcc_5g_hotspot_dfs_5g_sta_test.Mcc5gHotspotDfs5gStaTest,
          config=config,
      )
      self.add_test_class(
          mcc_5g_wfd_dfs_5g_sta_test.Mcc5gWfdDfs5gStaTest,
          config=config,
      )
      self.add_test_class(
          scc_dfs_5g_hotspot_sta_test.SccDfs5gHotspotStaTest,
          config=config,
      )
      self.add_test_class(
          scc_dfs_5g_wfd_sta_test.SccDfs5gWfdStaTest,
          config=config,
      )
      self.add_test_class(
          scc_5g_all_wifi_sta_test.Scc5gAllWifiStaTest,
          config=config,
      )

if __name__ == '__main__':
  # Use suite_runner's `main`.
  suite_runner.run_suite_class()
