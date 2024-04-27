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

"""iperf test util."""
import time
from mobly import utils
from mobly.controllers import android_device

# IPv4, 10 sec, 1 stream
DEFAULT_IPV4_CLIENT_ARGS = '-t 10 -P1'
DEFAULT_IPV4_SERVER_ARGS = '-J'
GROUP_OWNER_IPV4_ADDR_LEN_MAX = 15
IPERF_SERVER_START_DELAY_SEC = 1
IPERF_DEBUG_TIME_SEC = 300


class IPerfServerOnDevice:
  """Class that handles iperf3 server operations on device."""

  def __init__(self, serial, arg=DEFAULT_IPV4_SERVER_ARGS):
    self.iperf_str = 'adb -s {serial} shell iperf3 -s {arg}'.format(
        serial=serial, arg=arg
    )
    self.iperf_process = None
    self.started = False

  def start(self):
    """Starts iperf server on specified port."""
    if self.started:
      return

    cmd = self.iperf_str
    self.iperf_process = utils.start_standing_subprocess(cmd, shell=True)
    self.started = True

  def stop(self):
    if self.started:
      utils.stop_standing_subprocess(self.iperf_process)
      self.started = False


def run_iperf_test(
    ad_network_client: android_device.AndroidDevice,
    ad_network_owner: android_device.AndroidDevice,
) -> int:
  """Run iperf test from ad_network_client to ad_network_owner.

  Args:
    ad_network_client: android device that is the client in the iperf test.
    ad_network_owner: android device that is the server in the iperf test.

  Returns:
    speed in KB/s if there is a valid result or -1.
  """
  speed_kbyte_sec = -1

  group_owner_addr = get_group_owner_addr(ad_network_client)
  if not group_owner_addr:
    return speed_kbyte_sec
  client_arg = DEFAULT_IPV4_CLIENT_ARGS
  server_arg = DEFAULT_IPV4_SERVER_ARGS
  # Check if group owner address is an IPv6 address
  if len(group_owner_addr) > GROUP_OWNER_IPV4_ADDR_LEN_MAX:
    client_arg = DEFAULT_IPV4_CLIENT_ARGS + ' -6'
    server_arg = DEFAULT_IPV4_SERVER_ARGS + ' -6'

  server = IPerfServerOnDevice(ad_network_owner.serial, server_arg)

  try:
    ad_network_owner.log.info('Start iperf server')
    server.start()
    time.sleep(IPERF_SERVER_START_DELAY_SEC)
    ad_network_client.log.info('Start iperf client')
    success, result_list = ad_network_client.run_iperf_client(
        group_owner_addr, client_arg
    )
    result = ''.join(result_list)
    last_mbits_sec_index = result.rfind('Mbits/sec')
    if success and last_mbits_sec_index > 0:
      speed_mbps = int(
          float(result[:last_mbits_sec_index].strip().split(' ')[-1])
      )
      speed_kbyte_sec = int(speed_mbps * 1024 / 8)
    else:
      ad_network_client.log.info('Can not find valid iperf test result')
  except android_device.adb.AdbError:
    ad_network_client.log.info('run_iperf_client() failed')
    owner_ifconfig = get_ifconfig(ad_network_owner)
    client_ifconfig = get_ifconfig(ad_network_client)
    ad_network_client.log.info(client_ifconfig)
    ad_network_client.log.info(group_owner_addr)
    ad_network_client.log.info(client_arg)
    ad_network_owner.log.info(owner_ifconfig)
    # time.sleep(IPERF_DEBUG_TIME_SEC)
  else:
    server.stop()
  return speed_kbyte_sec


def get_ifconfig(
    ad: android_device.AndroidDevice,
) -> str:
  """Get network info from adb shell ifconfig."""
  return ad.adb.shell('ifconfig').decode('utf-8').strip()


def get_group_owner_addr(
    ad: android_device.AndroidDevice,
) -> str:
  """Get WFD group owner address from adb shell dumpsys wifip2p."""

  try:
    return (
        ad.adb.shell(
            'dumpsys wifip2p | egrep "groupOwnerAddress|groupOwnerIpAddress"'
        )
        .decode('utf-8')
        .strip()
        .split()[-1]
        .replace('/', '')
    )
  except android_device.adb.Error:
    ad.log.info('Failed to get group owner address.')
    return ''
