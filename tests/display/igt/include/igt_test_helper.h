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

#pragma once

#include <gtest/gtest.h>

#include "android-base/logging.h"
#include <cstdlib>
#include <string>

namespace igt {

// Holds the IGT subtests details as outlined in go/igt-al
struct IgtSubtestParams {
  std::string name;
  std::string_view desc;
  // Describe the reason we care about running this test.
  std::string_view rationale;
};

class IgtTestHelper {
public:
  // A helper function used by `INSTANTIATE_TEST_SUITE_P` from `GTest` to return
  // a test name based for each item in the array of |IgtSubtestParams|.
  static std::string
  generateGTestName(const ::testing::TestParamInfo<IgtSubtestParams> &info);

protected:
  IgtTestHelper(const std::string test_name)
      : test_name_("/data/igt_tests/x86_64/" + test_name + "64") {
    DCHECK(test_name.length());
  }

  void runSubTest(const IgtSubtestParams &subtest);
  void runTest(const std::string &desc, const std::string &rationale);

private:
  const std::string test_name_ = "";
};

} // namespace igt
