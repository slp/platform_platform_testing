/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <gtest/gtest.h>

#include <cstdlib>

#include "include/igt_test_helper.h"

namespace igt {
namespace {

/**
 * TEST: kms sysfs edid timing
 * Category: Display
 * Description: Test to check the time it takes to reprobe each connector.
 */

class KmsSysfsEdidTimingTests
    : public ::testing::TestWithParam<IgtSubtestParams>,
      public IgtTestHelper {
public:
  KmsSysfsEdidTimingTests() : IgtTestHelper("kms_sysfs_edid_timing") {}
};

TEST_F(KmsSysfsEdidTimingTests, TestSysfsEdidTiming) {
  std::string desc =
      "This test checks the time it takes to reprobe each connector and fails "
      "if either the time it takes for one reprobe is too long or if the mean "
      "time it takes to reprobe one connector is too long. Additionally, make "
      "sure that the mean time for all connectors is not too long.";
  std::string rationale = "we don't want tests taking forever to (re)probe";
  runTest(desc, rationale);
}

} // namespace
} // namespace igt
