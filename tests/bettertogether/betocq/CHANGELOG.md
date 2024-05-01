# BetoCQ test suite release history

## 2.1

## New
* Add iperf test.

### Bug fixes
* Add the support of BLE scan throttling during 2G transfer (enabled by default)
* Remove the unnecessary flag overriding and rely on the production config instead.
* Add BT coex test to betocq.
* Change the success rate target to 98%.
* Add the check of AP connection. If AP is disconnected or connect to a wrong frequency on the target side, mark the test as failed.
* consolidate AP connection and speed check codes to one function.
* Add P2P frequency check for WFD/HS SCC test cases.
* Reduce 2G speed check from 3 to 2 MB/s until it is improved in NC.
* Remove AP frequency check for the test cases with empty wifi_ssid.
* Fix typo in DFS test cases and reduce BT transfer size by 50%.
* Skip p2p frequency check if wifi speed check is disabled or it is a DBS test.

## 2.0

### New
* BetoCQ test suite: more accurate connectivity quality test suite with better
coverage.
  * CUJ tests: Connectivity performance evaluation for the specific CUJs, such
  as Quickstart, Quickshare, etc.
  * Directed tests: Specific performance tests for mediums used by D2D
  connection.
  * Function tests: Tests for the basic functions used by D2D connection.

## 1.6

### New
* `nearby_share_stress_test.py` for testing Nearby Share using Wifi only.

### Fixes
* Change discovery medium to BLE only.
* Increase 1G file transfer timeout to 400s.
* Disable GMS auto-updates for the duration of the test.

## 1.5

### New
* `esim_transfer_stress_test.py` for testing eSIM transfer using Bluetooth only.
* `quick_start_stress_test.py` for testing the Quickstart flow using both
   Bluetooth and Wifi.
