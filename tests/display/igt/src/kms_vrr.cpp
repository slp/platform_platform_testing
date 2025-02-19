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
 * TEST: kms vrr
 * Category: Display
 * Description: Test to validate different features of VRR
 * Functionality: adaptive_sync
 * Mega feature: Adaptive Sync
 * Test category: functionality test
 */

class KmsVrrTests : public ::testing::TestWithParam<IgtSubtestParams>,
                    public IgtTestHelper {
public:
  KmsVrrTests() : IgtTestHelper("kms_vrr") {}
};

IgtSubtestParams subtests[] = {
    {.name = "flipline",
     .desc = "Make sure that flips happen at flipline decision boundary",
     .rationale = "smoother visual experience, especially in games and video"},
    {.name = "lobf",
     .desc =
         "Test to validate link-off between active frames in non-psr operation",
     .rationale = "ensures that the feature works correctly"},
};

TEST_P(KmsVrrTests, RunSubTests) { runSubTest(GetParam()); }

INSTANTIATE_TEST_SUITE_P(KmsVrrTests, KmsVrrTests,
                         ::testing::ValuesIn(subtests),
                         IgtTestHelper::generateGTestName);

} // namespace
} // namespace igt
