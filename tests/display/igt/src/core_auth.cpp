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

class CoreAuthTests : public ::testing::TestWithParam<IgtSubtestParams>,
                      public IgtTestHelper {
public:
  CoreAuthTests() : IgtTestHelper("core_auth") {}
};

IgtSubtestParams subtests[] = {
    {.name = "getclient-simple",
     .desc = "Check drm client is always authenticated",
     .rationale = "ensuring that auth works correctly is probably P0 from a "
                  "security perspective"},
    {.name = "getclient-master-drop",
     .desc =
         "Use 2 clients, check second is authenticated even when first dropped",
     .rationale = "ensuring that auth works correctly is probably P0 from a "
                  "security perspective"},
    {.name = "basic-auth",
     .desc = "Test magic numbers for master and slave",
     .rationale = "ensuring that auth works correctly is probably P0 from a "
                  "security perspective"},
};

TEST_P(CoreAuthTests, RunSubTests) { runSubTest(GetParam()); }

INSTANTIATE_TEST_SUITE_P(CoreAuthTests, CoreAuthTests,
                         ::testing::ValuesIn(subtests),
                         IgtTestHelper::generateGTestName);

} // namespace
} // namespace igt
