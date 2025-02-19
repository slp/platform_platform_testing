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
 * TEST: kms pipe crc basic
 * Category: Display
 * Description: Tests behaviour of CRC
 * Functionality: crc
 * Mega feature: General Display Features
 * Test category: functionality test
 */

class KmsPipeCrcBasicTests : public ::testing::TestWithParam<IgtSubtestParams>,
                             public IgtTestHelper {
public:
  KmsPipeCrcBasicTests() : IgtTestHelper("kms_pipe_crc_basic") {}
};

IgtSubtestParams subtests[] = {
    // Fundamental Validation tests.
    {.name = "read-crc",
     .desc = "Test for pipe CRC reads",
     .rationale =
         "ensures we can read the CRC which we utilize in a lot of tests"},
    {.name = "hang-read-crc",
     .desc = "Hang test for pipe CRC read",
     .rationale = "Failure might indicate severe issues with hardware "
                  "stability and error recovery"},
    // Full System Tests
    {.name = "read-crc-frame-sequence",
     .desc = "Tests the pipe CRC read and ensure frame sequence.",
     .rationale = "verify that the display is outputting the correct pixels."},
    {.name = "suspend-read-crc",
     .desc = "Suspend test for pipe CRC reads",
     .rationale = "verify that the display is outputting the correct pixels."},
};

TEST_P(KmsPipeCrcBasicTests, RunSubTests) { runSubTest(GetParam()); }

INSTANTIATE_TEST_SUITE_P(KmsPipeCrcBasicTests, KmsPipeCrcBasicTests,
                         ::testing::ValuesIn(subtests),
                         IgtTestHelper::generateGTestName);

} // namespace
} // namespace igt
