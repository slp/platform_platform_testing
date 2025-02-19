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
 * TEST: kms vblank
 * Category: Display
 * Description: Test speed of WaitVblank.
 * Mega feature: General Display Features
 * Test category: functionality test
 */

class KmsVblankTests : public ::testing::TestWithParam<IgtSubtestParams>,
                       public IgtTestHelper {
public:
  KmsVblankTests() : IgtTestHelper("kms_vblank") {}
};

IgtSubtestParams subtests[] = {
    // Fundamental Validation tests.
    {.name = "crtc-id",
     .desc = "Check the vblank and flip events works with given crtc id",
     .rationale = "If this fails, almost everything else display-related is "
                  "likely broken as well."},
    {.name = "ts-continuation-modeset-rpm",
     .desc = "Test TS continuty with DPMS & RPM while hanging by introducing "
             "NOHANG flag",
     .rationale = "Failure could point to serious issues with power management "
                  "and modesetting, leading to display instability during use"},
    {.name = "accuracy-idle",
     .desc = "Test Accuracy of vblank events while hanging by introducing "
             "NOHANG Flag",
     .rationale =
         "Inaccurate signals can lead to performance problems, visual "
         "artifacts, or even the inability to display an image at all"},
    // Low Level Validation Tests
    {.name = "wait-idle",
     .desc = "Time taken to wait for vblanks",
     .rationale = "checks for performance degradation and reduced system "
                  "responsiveness"},
    {.name = "wait-busy",
     .desc = "Time taken to wait for vblanks (during V-active)",
     .rationale = "checks for system instability and unresponsiveness under "
                  "heavy load"},
    {.name = "ts-continuation-idle",
     .desc = "TS continuty",
     .rationale = "checks for timing issues"},
};

TEST_P(KmsVblankTests, RunSubTests) { runSubTest(GetParam()); }

INSTANTIATE_TEST_SUITE_P(KmsVblankTests, KmsVblankTests,
                         ::testing::ValuesIn(subtests),
                         IgtTestHelper::generateGTestName);

} // namespace
} // namespace igt
