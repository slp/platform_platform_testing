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
 * TEST: kms properties
 * Category: Display
 * Description: Test to validate the properties of all planes, crtc and
 * connectors
 * Functionality: kms_core Mega feature: General Display Features
 * Test category: functionality test
 */

class KmsPropertiesTests : public ::testing::TestWithParam<IgtSubtestParams>,
                           public IgtTestHelper {
public:
  KmsPropertiesTests() : IgtTestHelper("kms_properties") {}
};

IgtSubtestParams subtests[] = {
    // Fundamental Validation tests.
    {.name = "get_properties-sanity-atomic",
     .desc = "Test validates the properties of all planes, crtc and connectors "
             "with atomic commit",
     .rationale = "Failure here could lead to all sorts of unexpected behavior "
                  "- some props are reflection of HW caps"},
    // Full System Tests
    {.name = "plane-properties-atomic",
     .desc = "Tests plane properties with atomic commit",
     .rationale = "basic prop functionality for planes"},
    {.name = "crtc-properties-atomic",
     .desc = "Tests crtc properties with atomic commit",
     .rationale = "basic prop functionality for crtcs"},
    {.name = "connector-properties-atomic",
     .desc = "Tests connector properties with atomic commit",
     .rationale = "basic prop functionality for connectors"},
};

TEST_P(KmsPropertiesTests, RunSubTests) { runSubTest(GetParam()); }

INSTANTIATE_TEST_SUITE_P(KmsPropertiesTests, KmsPropertiesTests,
                         ::testing::ValuesIn(subtests),
                         IgtTestHelper::generateGTestName);

} // namespace
} // namespace igt
