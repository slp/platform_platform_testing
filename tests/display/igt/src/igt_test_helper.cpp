/*
 * Copyright (C) 2024 Google LLC.
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

#include "include/igt_test_helper.h"

#include <android-base/logging.h>
#include <gtest/gtest.h>

#include <array>
#include <cstdio>
#include <cstdlib>
#include <memory>
#include <optional>
#include <sstream>

namespace igt {
namespace {
enum class TestResult { kPass, kFail, kSkip, kUnknown };

std::optional<std::string> runCommand(const std::string &cmd) {
  // Gtest runs from root /, so the command should start from there.
  std::unique_ptr<FILE, decltype(&pclose)> pipe(popen(cmd.c_str(), "r"),
                                                pclose);
  if (!pipe) {
    ADD_FAILURE() << "popen() failed! Could not find or run the binary.";
    return std::nullopt;
  }

  std::array<char, 128> buffer;
  std::string result;
  while (fgets(buffer.data(), buffer.size(), pipe.get()) != nullptr) {
    result += buffer.data();
  }
  return result;
}

TestResult getSubtestTestResultFromLog(const std::string &log,
                                       const std::string &subTestName) {
  if (log.find("Subtest " + subTestName + ": FAIL") != std::string::npos) {
    return TestResult::kFail;
  } else if (log.find("Subtest " + subTestName + ": SKIP") !=
             std::string::npos) {
    return TestResult::kSkip;
  } else if (log.find("Subtest " + subTestName + ": SUCCESS") !=
             std::string::npos) {
    return TestResult::kPass;
  } else {
    return TestResult::kUnknown;
  }
}

TestResult getTestResultFromLog(std::string &log) {
  std::for_each(log.begin(), log.end(), [](char &c) { c = ::tolower(c); });

  if (log.find("fail") != std::string::npos) {
    return TestResult::kFail;
  } else if (log.find("skip") != std::string::npos) {
    return TestResult::kSkip;
  } else if (log.find("success") != std::string::npos) {
    return TestResult::kPass;
  } else {
    return TestResult::kUnknown;
  }
}

std::string generateFailureLog(const std::string &log,
                               const std::string_view &desc,
                               const std::string_view &rationale) {

  std::stringstream failureMessage;
  failureMessage << log << std::endl;
  failureMessage << "**What the test is doing**: " << desc << std::endl;
  failureMessage << "**Why the test should be fixed**: " << rationale
                 << std::endl;

  return failureMessage.str();
}

void presentTestResult(TestResult result, const std::string &log,
                       const std::string_view &desc,
                       const std::string_view &rationale) {
  switch (result) {
  case TestResult::kPass:
    SUCCEED();
    break;
  case TestResult::kFail:
    ADD_FAILURE() << generateFailureLog(log, desc, rationale);
    break;
  case TestResult::kSkip:
    GTEST_SKIP() << log;
    break;
  case TestResult::kUnknown:
    ADD_FAILURE() << "Could not determine test result.\n" << log;
    break;
  default:
    ADD_FAILURE() << log;
    break;
  }
}
} // namespace

// static
std::string IgtTestHelper::generateGTestName(
    const ::testing::TestParamInfo<IgtSubtestParams> &info) {
  std::string dashedName(info.param.name);

  // Many subtest names include %s and %d which are not valid GTest names.
  size_t pos = dashedName.find("%s");
  while (pos != std::string::npos) {
    dashedName.erase(pos, 2);
    pos = dashedName.find("%s", pos);
  }
  pos = dashedName.find("%d");
  while (pos != std::string::npos) {
    dashedName.erase(pos, 2);
    pos = dashedName.find("%d", pos);
  }

  // convert test-name to PascalCase
  std::stringstream ss(dashedName);
  std::string word;
  std::string pascalCaseName;
  while (std::getline(ss, word, '-')) {
    if (!word.empty()) {
      // Capitalize the first letter
      word[0] = std::toupper(word[0]);
      pascalCaseName += word;
    }
  }

  return pascalCaseName;
}

void IgtTestHelper::runSubTest(const IgtSubtestParams &subtest) {
  CHECK(test_name_.size());
  std::optional<std::string> log =
      runCommand(test_name_ + " --run-subtest " + subtest.name);
  if (!log.has_value())
    return;

  TestResult result = getSubtestTestResultFromLog(log.value(), subtest.name);
  presentTestResult(result, log.value(), subtest.desc, subtest.rationale);
}

void IgtTestHelper::runTest(const std::string &desc,
                            const std::string &rationale) {
  CHECK(test_name_.size());

  std::optional<std::string> log = runCommand(test_name_);
  if (!log.has_value())
    return;

  TestResult result = getTestResultFromLog(log.value());
  presentTestResult(result, log.value(), desc, rationale);
}

} // namespace igt
