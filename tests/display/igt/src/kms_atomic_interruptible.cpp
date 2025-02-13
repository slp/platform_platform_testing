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
 * TEST: kms atomic interruptible
 * Category: Display
 * Description: Tests that interrupt various atomic ioctls.
 * Mega feature: General Display Features
 * Test category: functionality test
 */

class KmsAtomicInterruptibleTests
    : public ::testing::TestWithParam<IgtSubtestParams>,
      public IgtTestHelper {
public:
  KmsAtomicInterruptibleTests() : IgtTestHelper("kms_atomic_interruptible") {}
};

IgtSubtestParams subtests[] = {
    // Full System Tests
    {.name = "atomic-setmode",
     .desc = "Validate atomic modeset by interruption",
     .rationale = "interruptibility"},
    {.name = "universal-setplane-primary",
     .desc = "Validate atomic setplane on primary by interruption",
     .rationale = "interruptibility"},
    {.name = "universal-setplane-cursor",
     .desc = "Validate atomic setplane on cursor by interruption",
     .rationale = "interruptibility, "
                  "otherwise it's not"},
};

TEST_P(KmsAtomicInterruptibleTests, RunSubTests) { runSubTest(GetParam()); }

INSTANTIATE_TEST_SUITE_P(KmsAtomicInterruptibleTests,
                         KmsAtomicInterruptibleTests,
                         ::testing::ValuesIn(subtests),
                         IgtTestHelper::generateGTestName);

} // namespace
} // namespace igt
