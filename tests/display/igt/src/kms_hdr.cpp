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
 * TEST: kms hdr
 * Category: Display
 * Description: Test HDR metadata interfaces and bpc switch
 * Mega feature: HDR
 * Test category: functionality test
 */

class KmsHdrTests : public ::testing::TestWithParam<IgtSubtestParams>,
                    public IgtTestHelper {
public:
  KmsHdrTests() : IgtTestHelper("kms_hdr") {}
};

IgtSubtestParams subtests[] = {
    // Fundamental Validation tests.
    {.name = "bpc-switch",
     .desc = "Tests switching between different display output bpc modes",
     .rationale = "it's a hardware feature"},
    {.name = "invalid-hdr",
     .desc = "Test to ensure HDR is not enabled on non-HDR panel",
     .rationale = "it's a hardware feature"},
    {.name = "invalid-metadata-sizes",
     .desc = "Tests invalid HDR metadata sizes",
     .rationale = "it's a hardware feature"},

};

TEST_P(KmsHdrTests, RunSubTests) { runSubTest(GetParam()); }

INSTANTIATE_TEST_SUITE_P(KmsHdrTests, KmsHdrTests,
                         ::testing::ValuesIn(subtests),
                         IgtTestHelper::generateGTestName);

} // namespace
} // namespace igt
