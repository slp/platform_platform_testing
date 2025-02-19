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
 * TEST: kms rotation crc
 * Category: Display
 * Description: Tests different rotations with different planes & formats
 * Functionality: plane, rotation
 * Mega feature: General Display Features
 * Test category: functionality test
 */

class KmsRotationCrcTests : public ::testing::TestWithParam<IgtSubtestParams>,
                            public IgtTestHelper {
public:
  KmsRotationCrcTests() : IgtTestHelper("kms_rotation_crc") {}
};

IgtSubtestParams subtests[] = {
    {.name = "%s-rotation-180",
     .desc = "Rotation test with 180 degree for (primary/sprite/cursor) planes",
     .rationale = "plane rotation"},
    {.name = "%s-rotation-%d",
     .desc = "Rotation test with (90/270) degree for (primary/sprite) planes "
             "of gen9+",
     .rationale = "plane rotation"},
    {.name = "bad-pixel-format",
     .desc = "Checking unsupported pixel format for gen9+ with 90 degree of "
             "rotation",
     .rationale = "plane rotation"},
    {.name = "bad-tiling",
     .desc = "Checking unsupported tiling for gen9+ with 90 degree of rotation",
     .rationale = "plane rotation"},
    {.name = "multiplane-rotation",
     .desc = "Rotation test on both planes by making them fully visible",
     .rationale = "plane rotation"},
    {.name = "multiplane-rotation-cropping-%s",
     .desc =
         "Rotation test on both planes by cropping left/(bottom/top) corner of "
         "primary plane and right/(bottom/top) corner of sprite plane",
     .rationale = "plane rotation"},
};

TEST_P(KmsRotationCrcTests, RunSubTests) { runSubTest(GetParam()); }

INSTANTIATE_TEST_SUITE_P(KmsRotationCrcTests, KmsRotationCrcTests,
                         ::testing::ValuesIn(subtests),
                         IgtTestHelper::generateGTestName);

} // namespace
} // namespace igt