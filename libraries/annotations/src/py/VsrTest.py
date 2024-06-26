#  Copyright (C) 2024 The Android Open Source Project
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

"""VsrTest decorator."""

from typing import List


class VsrTest(object):
  """Marks the type of test with purpose of asserting VSR requirements.

  Args:
      requirements: the list of VSR requirements.
  """

  def __init__(self, requirements: List[str] = []):
    self._requirements = requirements

  def __call__(self, func):
    return func
