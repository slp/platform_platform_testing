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
 * TEST: kms setmode
 * Category: Display
 * Description: Tests the mode by iterating through all valid/invalid
 * crtc/connector combinations
 * Functionality: kms_core Mega feature: General
 * Display Features Test category: functionality test
 */

class KmsSetmodeTests : public ::testing::TestWithParam<IgtSubtestParams>,
                        public IgtTestHelper {
public:
  KmsSetmodeTests() : IgtTestHelper("kms_setmode") {}
};

IgtSubtestParams subtests[] = {
    {.name = "basic",
     .desc = "Tests the vblank timing by iterating through all valid "
             "crtc/connector combinations",
     .rationale = "basic functionality"},
};

TEST_P(KmsSetmodeTests, RunSubTests) { runSubTest(GetParam()); }

INSTANTIATE_TEST_SUITE_P(KmsSetmodeTests, KmsSetmodeTests,
                         ::testing::ValuesIn(subtests),
                         IgtTestHelper::generateGTestName);

} // namespace
} // namespace igt
