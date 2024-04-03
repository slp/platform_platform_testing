# BetoCQ test suite release history

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