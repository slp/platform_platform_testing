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
 * TEST: kms plane cursor
 * Category: Display
 * Description: Tests cursor interactions with primary and overlay planes.
 * Functionality: cursor, plane
 * Mega feature: General Display Features
 * Test category: functionality test
 */

class KmsPlaneCursorTests : public ::testing::TestWithParam<IgtSubtestParams>,
                            public IgtTestHelper {
public:
  KmsPlaneCursorTests() : IgtTestHelper("kms_plane_cursor") {}
};

IgtSubtestParams subtests[] = {
    // Low Level Validation Tests
    {.name = "primary",
     .desc = "Tests atomic cursor positioning on primary plane",
     .rationale = "checks for inability to control the cursor position "
                  "accurately, impacting user interaction."},
};

TEST_P(KmsPlaneCursorTests, RunSubTests) { runSubTest(GetParam()); }

INSTANTIATE_TEST_SUITE_P(KmsPlaneCursorTests, KmsPlaneCursorTests,
                         ::testing::ValuesIn(subtests),
                         IgtTestHelper::generateGTestName);

} // namespace
} // namespace igt
