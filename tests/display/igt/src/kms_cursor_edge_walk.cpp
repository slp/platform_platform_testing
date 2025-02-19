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
 * TEST: kms cursor edge walk
 * Category: Display
 * Description: Test to check different cursor sizes by walking different edges
 * of screen
 * Functionality: cursor Mega feature: General Display Features Test
 * category: functionality test
 */

class KmsCursorEdgeWalkTests
    : public ::testing::TestWithParam<IgtSubtestParams>,
      public IgtTestHelper {
public:
  KmsCursorEdgeWalkTests() : IgtTestHelper("kms_cursor_edge_walk") {}
};

TEST_F(KmsCursorEdgeWalkTests, TestCursorEdgeWalk) {
  std::string desc = "Test to check different cursor sizes by walking "
                     "different edges of screen";
  std::string rationale = "Functionality: cursor";
  runTest(desc, rationale);
}

} // namespace
} // namespace igt
