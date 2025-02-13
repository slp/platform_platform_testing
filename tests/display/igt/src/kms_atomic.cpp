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
 * TEST: kms atomic
 * Category: Display
 * Description: Test atomic modesetting API
 * Functionality: kms_core, plane
 * Mega feature: General Display Features
 * Test category: functionality test
 */

class KmsAtomicTests : public ::testing::TestWithParam<IgtSubtestParams>,
                       public IgtTestHelper {
public:
  KmsAtomicTests() : IgtTestHelper("kms_atomic") {}
};

IgtSubtestParams subtests[] = {
    // Low Level Validation Tests
    {.name = "atomic-invalid-params",
     .desc = "Test abuse the atomic ioctl directly in order to test "
             "various "
             "invalid conditions which the libdrm wrapper won't allow "
             "us to create",
     .rationale = "important for ensuring the robustness and security of the "
                  "atomic modesetting API"},
    {.name = "atomic-plane-damage",
     .desc = "Simple test cases to use FB_DAMAGE_CLIPS plane property",
     .rationale = "important for optimizing performance by only updating the "
                  "portion of the display that has changed"},
    {.name = "test-only",
     .desc = "Test to ensure that DRM_MODE_ATOMIC_TEST_ONLY really only "
             "touches the free-standing state objects and nothing else.",
     .rationale = "useful for validating a complex modeset configuration "
                  "before committing to it such"},
    {.name = "plane-primary-overlay-mutable-zpos",
     .desc = "Verify that the overlay plane can cover the primary one (and "
             "vice versa) by changing their zpos property",
     .rationale = "important for ensuring that the overlay can be displayed "
                  "correctly on top of or behind the primary plane"},
    {.name = "plane-immutable-zpos",
     .desc = "Verify the reported zpos property of planes by making sure only "
             "higher zpos planes cover the lower zpos ones",
     .rationale = "important for ensuring that the planes are displayed in the "
                  "correct order"}};

TEST_P(KmsAtomicTests, Run) { runSubTest(GetParam()); }

INSTANTIATE_TEST_SUITE_P(KmsAtomic, KmsAtomicTests,
                         ::testing::ValuesIn(subtests));

} // namespace
} // namespace igt
