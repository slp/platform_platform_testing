#  Copyright (C) 2023 The Android Open Source Project
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

import argparse
import sys
# TODO(b/311467339): add asbl support for test runs in the lab
# from absl import flags
from mobly import test_runner


"""
Get Test Args

Extracts the test arguments from the System Args

usage:
  python3 <test> -- -c /tmp/config.yaml --test_args=k1=v1 --test_args=k2=v2

CATBox usage:
  catbox-tradefed run commandAndExit <test-plan> --mobly-options --test_args=k1=v1 --mobly-options --test_args=k2=v2

Returns: Dictionary with key-value pair
  e.g.
  {
    k1: v1,
    k2: v2
  }
"""
def get_test_args():
    parser = argparse.ArgumentParser(description='Parse Test Args.')
    group = parser.add_mutually_exclusive_group(required=False)
    group.add_argument('--test_args',
                          action='append',
                          nargs='+',
                          type=str,
                          help='A list of test args for the test.')
    parsed_test_args = parser.parse_known_args(sys.argv)[0]

    if not parsed_test_args.test_args:
        return {}

    test_args_list = [
        test_arg
        for test_args in parsed_test_args.test_args
        for test_arg in test_args
    ]
    test_args = {test_arg.split("=")[0]: test_arg.split("=")[1] for test_arg in test_args_list}
    return test_args

"""Pass test arguments after '--' to the test runner. Needed for Mobly Test Runner.

Splits the arguments vector by '--'. Anything before separtor is treated as absl flags.
Everything after is a Mobly Test Runner arguments. Example:

    python3 <test> --itreations=2 -- -c /tmp/config.yaml

Example usage:

    if __name__ == '__main__':
        common_main()
"""
def common_main():
    absl_argv = []
    if '--' in sys.argv:
        index = sys.argv.index('--')
        absl_argv = sys.argv[:index]
        sys.argv = sys.argv[:1] + sys.argv[index + 1:]
    # TODO(b/311467339): add asbl support for test runs in the lab
    # flags.FLAGS(absl_argv)
    test_runner.main()
