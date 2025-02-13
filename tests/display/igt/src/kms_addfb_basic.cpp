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
 * TEST: kms addfb basic
 * Category: Display
 * Description: Sanity test for ioctls DRM_IOCTL_MODE_ADDFB2 &
 * DRM_IOCTL_MODE_RMFB.
 * Functionality: kms_gem_interop Mega feature: General
 * Display Features Test category: functionality test
 */

class KmsAddfbBasicTests : public ::testing::TestWithParam<IgtSubtestParams>,
                           public IgtTestHelper {
public:
  KmsAddfbBasicTests() : IgtTestHelper("kms_addfb_basic") {}
};

IgtSubtestParams subtests[] = {
    // Low Level Validation Tests
    {.name = "basic",
     .desc = "Check if addfb2 call works with given handle",
     .rationale = "fundamentral fb mgmt"},
};

TEST_P(KmsAddfbBasicTests, RunSubTests) { runSubTest(GetParam()); }

INSTANTIATE_TEST_SUITE_P(KmsAddfbBasicTests, KmsAddfbBasicTests,
                         ::testing::ValuesIn(subtests),
                         IgtTestHelper::generateGTestName);

} // namespace
} // namespace igt
