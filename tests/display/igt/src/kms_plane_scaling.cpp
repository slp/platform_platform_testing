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
 * TEST: kms plane scaling
 * Category: Display
 * Description: Test display plane scaling
 * Functionality: plane, scaling
 * Mega feature: General Display Features
 * Test category: functionality test
 */

class KmsPlaneScalingTests : public ::testing::TestWithParam<IgtSubtestParams>,
                             public IgtTestHelper {
public:
  KmsPlaneScalingTests() : IgtTestHelper("kms_plane_scaling") {}
};

IgtSubtestParams subtests[] = {
    // Fundamental Validation tests.
    {.name = "plane-scaler-unity-scaling-with-rotation",
     .desc = "Tests scaling with rotation, unity scaling",
     .rationale =
         "checks if the hardware can correctly combine scaling and rotation. "
         "Even with unity scaling, the hardware needs to perform the rotation "
         "while maintaining the image quality and avoiding artifacts"},
    {.name = "plane-scaler-with-clipping-clamping-rotation",
     .desc = "Tests scaling with clipping and clamping, rotation",
     .rationale =
         "checks if the hardware can correctly handle scaling with additional "
         "constraints (clipping and clamping) while also performing rotation"},
    // eDP Validation Tests
    {.name = "plane-scaler-unity-scaling-with-pixel-format",
     .desc = "Tests scaling with pixel formats, unity scaling",
     .rationale = "failure indicate problems with displaying content at the "
                  "native resolution of the eDP panel, potentially causing "
                  "blurry or distorted images"},
    {.name = "plane-downscale-factor-0-5-with-pixel-format",
     .desc = "Tests downscaling with pixel formats for 0.5 scaling factor.",
     .rationale =
         "checks if the eDP panel can correctly downscale images, which is "
         "important for scenarios like displaying a lower-resolution video on "
         "the high-resolution panel, preventing issues like incorrect scaling "
         "artifacts or a black screen"},
};

TEST_P(KmsPlaneScalingTests, RunSubTests) { runSubTest(GetParam()); }

INSTANTIATE_TEST_SUITE_P(KmsPlaneScalingTests, KmsPlaneScalingTests,
                         ::testing::ValuesIn(subtests),
                         IgtTestHelper::generateGTestName);

} // namespace
} // namespace igt
