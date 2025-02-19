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
 * TEST: kms bw
 * Category: Display
 * Description: BW test with different resolutions
 * Functionality: kms_core
 * Mega feature: Display Latency/Bandwidth
 * Test category: functionality test
 */

class KmsBwTests : public ::testing::TestWithParam<IgtSubtestParams>,
                   public IgtTestHelper {
public:
  KmsBwTests() : IgtTestHelper("kms_flip") {}
};

constexpr std::string_view kDescription =
    "pushes the display pipeline's bandwidth to its limits using a "
    "high-resolution configuration";
constexpr std::string_view kRationale =
    "Failure could indicate the system can't handle demanding display tasks, "
    "leading to slowdowns or an inability to drive the display at its native "
    "resolution";

IgtSubtestParams subtests[] = {
    // Fundamental Validation tests.
    {.name = "linear-tiling-%d-displays-%s",
     .desc = kDescription,
     .rationale = kRationale},
    {.name = "connected-linear-tiling-%d-displays-%s",
     .desc = kDescription,
     .rationale = kRationale},

};

TEST_P(KmsBwTests, RunSubTests) { runSubTest(GetParam()); }

INSTANTIATE_TEST_SUITE_P(KmsBwTests, KmsBwTests, ::testing::ValuesIn(subtests),
                         IgtTestHelper::generateGTestName);

} // namespace
} // namespace igt
