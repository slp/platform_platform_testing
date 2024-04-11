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

This requires a 2G AP to be ready and configured in testbed.
2G AP (wifi_2g_ssid): channel 6 (2437)
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
from betocq.compound_tests import mcc_5g_all_wifi_non_dbs_2g_sta_test
from betocq.compound_tests import scc_2g_all_wifi_sta_test
from betocq.compound_tests import scc_5g_all_wifi_dbs_2g_sta_test
from betocq.directed_tests import ble_performance_test
from betocq.directed_tests import bt_performance_test
from betocq.directed_tests import mcc_5g_wfd_non_dbs_2g_sta_test
from betocq.directed_tests import scc_2g_wfd_sta_test
from betocq.directed_tests import scc_2g_wlan_sta_test
from betocq.directed_tests import scc_5g_wfd_dbs_2g_sta_test
from betocq.function_tests import beto_cq_function_group_test


class BetoCq2gApPerformanceTestSuite(base_betocq_suite.BaseBetocqSuite):
  """Add all BetoCQ tests which requires 2G STA AP or no AP to run in sequence."""

  def setup_suite(self, config):
    """Add all BetoCQ tests which requires 2G STA AP or no AP to the suite."""
    test_parameters = nc_constants.TestParameters.from_user_params(
        config.user_params
    )

    # add function tests if required
    if test_parameters.run_function_tests_with_performance_tests:
      self.add_test_class(
          beto_cq_function_group_test.BetoCqFunctionGroupTest,
          config=config,
      )

    # add bt and ble test
    self.add_test_class(
        bt_performance_test.BtPerformanceTest, config=config
    )
    # TODO(kaishi): enable BLE test when it is ready

    # add directed/cuj tests which requires 2G wlan AP - channel 6
    if (
        test_parameters.wifi_2g_ssid
        or test_parameters.use_auto_controlled_wifi_ap
    ):
      self.add_test_class(
          mcc_5g_wfd_non_dbs_2g_sta_test.Mcc5gWfdNonDbs2gStaTest,
          config=config,
      )
      self.add_test_class(
          scc_2g_wfd_sta_test.Scc2gWfdStaTest, config=config
      )
      self.add_test_class(
          scc_2g_wlan_sta_test.Scc2gWlanStaTest,
          config=config,
      )
      self.add_test_class(
          scc_5g_wfd_dbs_2g_sta_test.Scc5gWfdDbs2gStaTest,
          config=config,
      )
      self.add_test_class(
          mcc_5g_all_wifi_non_dbs_2g_sta_test.Mcc5gAllWifiNonDbs2gStaTest,
          config=config,
      )
      self.add_test_class(
          scc_2g_all_wifi_sta_test.Scc2gAllWifiStaTest, config=config
      )
      self.add_test_class(
          scc_5g_all_wifi_dbs_2g_sta_test.Scc5gAllWifiDbs2gStaTest,
          config=config,
      )

if __name__ == '__main__':
  # Use suite_runner's `main`.
  suite_runner.run_suite_class()

