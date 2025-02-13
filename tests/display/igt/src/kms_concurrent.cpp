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
 * TEST: kms concurrent
 * Category: Display
 * Description: Test atomic mode setting concurrently with multiple planes and
 *              screen resolution
 */

class KmsConcurrentTests : public ::testing::TestWithParam<IgtSubtestParams>,
                           public IgtTestHelper {
public:
  KmsConcurrentTests() : IgtTestHelper("kms_concurrent") {}
};

IgtSubtestParams subtests[] = {
    // Full System Tests
    {.name = "multi-plane-atomic-lowres",
     .desc = "Test atomic mode setting concurrently with multiple planes and "
             "screen resolution.",
     .rationale = "concurrent operations"},
};

TEST_P(KmsConcurrentTests, RunSubTests) { runSubTest(GetParam()); }

INSTANTIATE_TEST_SUITE_P(KmsConcurrentTests, KmsConcurrentTests,
                         ::testing::ValuesIn(subtests),
                         IgtTestHelper::generateGTestName);

} // namespace
} // namespace igt
