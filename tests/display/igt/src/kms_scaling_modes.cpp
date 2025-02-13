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
 * TEST: kms scaling modes
 * Category: Display
 * Description: Test display scaling modes
 * Functionality: edp, plane, scaling
 * Mega feature: General Display Features
 * Test category: functionality test
 */

class KmsScalingModesTests : public ::testing::TestWithParam<IgtSubtestParams>,
                             public IgtTestHelper {
public:
  KmsScalingModesTests() : IgtTestHelper("kms_scaling_modes") {}
};

TEST_F(KmsScalingModesTests, TestSysfsEdidTiming) {
  std::string desc = "Test display scaling modes";
  std::string rationale = "Functionality: edp, plane, scaling";
  runTest(desc, rationale);
}

} // namespace
} // namespace igt
