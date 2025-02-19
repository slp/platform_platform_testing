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
 * TEST: kms prop blob
 * Category: Display
 * Description: Tests behaviour of mass-data 'blob' properties.
 * Functionality: kms_core
 * Mega feature: General Display Features
 * Test category: functionality test
 */

class KmsPropBlobTests : public ::testing::TestWithParam<IgtSubtestParams>,
                         public IgtTestHelper {
public:
  KmsPropBlobTests() : IgtTestHelper("kms_prop_blob") {}
};

IgtSubtestParams subtests[] = {
    // Full System Tests
    {.name = "blob-prop-core",
     .desc = "Tests error handling when invalid property IDs are passed.",
     .rationale = "DRM property blob functionality"},
    {.name = "blob-prop-validate",
     .desc = "Tests error handling when incorrect blob size is passed.",
     .rationale = "DRM property blob functionality"},
    {.name = "blob-prop-lifetime",
     .desc = "Tests validates the lifetime of the properties created",
     .rationale = "DRM property blob functionality"},
};

TEST_P(KmsPropBlobTests, RunSubTests) { runSubTest(GetParam()); }

INSTANTIATE_TEST_SUITE_P(KmsPropBlobTests, KmsPropBlobTests,
                         ::testing::ValuesIn(subtests),
                         IgtTestHelper::generateGTestName);

} // namespace
} // namespace igt