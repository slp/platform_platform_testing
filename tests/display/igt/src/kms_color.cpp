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
 * TEST: kms color
 * Category: Display
 * Description: Test Color Features at Pipe level
 * Functionality: colorspace
 * Mega feature: Color Management
 * Test category: functionality test
 */

class KmsColorTests : public ::testing::TestWithParam<IgtSubtestParams>,
                      public IgtTestHelper {
public:
  KmsColorTests() : IgtTestHelper("kms_color") {}
};

IgtSubtestParams subtests[] = {
    // Fundamental Validation tests.
    {.name = "deep-color",
     .desc = "Verify that deep color works correctly",
     .rationale = "essential for high-quality displays and accurate color "
                  "representation"},
    {.name = "degamma",
     .desc = "Verify that degamma LUT transformation works correctly",
     .rationale =
         "verifies that the degamma LUT is applied correctly by the hardware"},
    {.name = "gamma",
     .desc = "Verify that gamma LUT transformation works correctly",
     .rationale =
         "checks if the gamma LUT is applied correctly by the hardware"},
    {.name = "ctm-%s",
     .desc = "Check the color transformation",
     .rationale = "checks the hardware's ability to apply various color "
                  "transformations using CTMs"},

};

TEST_P(KmsColorTests, RunSubTests) { runSubTest(GetParam()); }

INSTANTIATE_TEST_SUITE_P(KmsColorTests, KmsColorTests,
                         ::testing::ValuesIn(subtests),
                         IgtTestHelper::generateGTestName);

} // namespace
} // namespace igt
