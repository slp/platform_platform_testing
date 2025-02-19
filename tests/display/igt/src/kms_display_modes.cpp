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
 * TEST: kms display modes
 * Category: Display
 * Description: Test Display Modes
 * Mega feature: General Display Features
 * Test category: functionality test
 */

class KmsDisplayModesTests : public ::testing::TestWithParam<IgtSubtestParams>,
                             public IgtTestHelper {
public:
  KmsDisplayModesTests() : IgtTestHelper("kms_display_modes") {}
};

IgtSubtestParams subtests[] = {
    // Fundamental Validation tests.
    {.name = "extended-mode-basic",
     .desc = "Test for validating display extended mode with a pair of "
             "connected displays",
     .rationale = "common use case"},
};

TEST_P(KmsDisplayModesTests, RunSubTests) { runSubTest(GetParam()); }

INSTANTIATE_TEST_SUITE_P(KmsDisplayModesTests, KmsDisplayModesTests,
                         ::testing::ValuesIn(subtests),
                         IgtTestHelper::generateGTestName);

} // namespace
} // namespace igt
