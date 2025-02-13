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
 * TEST: kms content protection
 * Category: Display
 * Description: Test content protection (HDCP)
 * Functionality: hdcp1.4
 * Mega feature: HDCP
 * Test category: functionality test
 */

class KmsContentProtectionTests
    : public ::testing::TestWithParam<IgtSubtestParams>,
      public IgtTestHelper {
public:
  KmsContentProtectionTests() : IgtTestHelper("kms_content_protection") {}
};

IgtSubtestParams subtests[] = {
    // Fundamental Validation tests.
    {.name = "lic-type-0",
     .desc = "Test for the integrity of link for type-0 content",
     .rationale = "it's a hardware feature"},
    {.name = "lic-type-1",
     .desc = "Test for the integrity of link for type-1 content",
     .rationale = "it's a hardware feature"}};

TEST_P(KmsContentProtectionTests, RunSubTests) { runSubTest(GetParam()); }

INSTANTIATE_TEST_SUITE_P(KmsContentProtectionTests, KmsContentProtectionTests,
                         ::testing::ValuesIn(subtests),
                         IgtTestHelper::generateGTestName);

} // namespace
} // namespace igt
