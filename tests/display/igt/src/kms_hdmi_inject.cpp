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
 * TEST: kms hdmi inject
 * Category: Display
 * Description: Tests for validating hdmi inject
 * Functionality: hdmi inject
 * Mega feature: General Display Features
 * Test category: functionality test
 */

class KmsHdmiInjectTests : public ::testing::TestWithParam<IgtSubtestParams>,
                           public IgtTestHelper {
public:
  KmsHdmiInjectTests() : IgtTestHelper("kms_hdmi_inject") {}
};

IgtSubtestParams subtests[] = {
    // Full System Tests
    {.name = "inject-4k",
     .desc = "Make sure that 4K modes exposed by DRM match the forced EDID and "
             "modesetting using it succeed.",
     .rationale = "EDID is solid"},
};

TEST_P(KmsHdmiInjectTests, RunSubTests) { runSubTest(GetParam()); }

INSTANTIATE_TEST_SUITE_P(KmsHdmiInjectTests, KmsHdmiInjectTests,
                         ::testing::ValuesIn(subtests),
                         IgtTestHelper::generateGTestName);

} // namespace
} // namespace igt
