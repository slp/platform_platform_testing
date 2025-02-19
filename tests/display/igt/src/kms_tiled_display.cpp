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
 * TEST: kms tiled display
 * Category: Display
 * Description: Test for Transcoder Port Sync for Display Port Tiled Displays
 * Mega feature: General Display Features
 * Test category: functionality test
 */

class KmsTiledDisplayTests : public ::testing::TestWithParam<IgtSubtestParams>,
                             public IgtTestHelper {
public:
  KmsTiledDisplayTests() : IgtTestHelper("kms_tiled_display") {}
};

IgtSubtestParams subtests[] = {
    // Fundamental Validation tests.
    {.name = "basic-test-pattern",
     .desc = "Make sure the Tiled CRTCs are synchronized and we get page flips "
             "for all tiled CRTCs in one vblank",
     .rationale = "Failure could lead to tearing or other visual artifacts"},
};

TEST_P(KmsTiledDisplayTests, RunSubTests) { runSubTest(GetParam()); }

INSTANTIATE_TEST_SUITE_P(KmsTiledDisplayTests, KmsTiledDisplayTests,
                         ::testing::ValuesIn(subtests),
                         IgtTestHelper::generateGTestName);

} // namespace
} // namespace igt
