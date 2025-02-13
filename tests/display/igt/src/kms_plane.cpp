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
 * TEST: kms plane
 * Category: Display
 * Description: Testes for KMS Plane
 * Mega feature: General Display Features
 * Test category: functionality test
 */

class KmsPlaneTests : public ::testing::TestWithParam<IgtSubtestParams>,
                      public IgtTestHelper {
public:
  KmsPlaneTests() : IgtTestHelper("kms_plane") {}
};

IgtSubtestParams subtests[] = {
    // Fundamental Validation tests.
    {.name = "planar-pixel-format-settings",
     .desc = "verify planar settings for pixel format are handled correctly",
     .rationale = "hardware correctly handles planar pixel formats"},
    {.name = "pixel-format",
     .desc = "verify the pixel formats for given plane and pipe",
     .rationale = "broader test of formats"},
    // eDP Validation Tests
    {.name = "plane-position-hole",
     .desc = "verify plane position using two planes to create a partially "
             "covered screen",
     .rationale = "ensure that the eDP panel can correctly display images from "
                  "multiple planes, avoiding issues like flickering or "
                  "incorrect layering of elements on the screen"},
};

TEST_P(KmsPlaneTests, RunSubTests) { runSubTest(GetParam()); }

INSTANTIATE_TEST_SUITE_P(KmsPlaneTests, KmsPlaneTests,
                         ::testing::ValuesIn(subtests),
                         IgtTestHelper::generateGTestName);

} // namespace
} // namespace igt
