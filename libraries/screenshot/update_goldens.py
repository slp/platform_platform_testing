#!/usr/bin/env python3

#
# Copyright 2024, The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
"""Script to update golden screenshot files from a temp directory with all files."""

import argparse
import os
import pathlib
import re
import sys
import shutil

ACTUAL_SCREENSHOT_FILE_LINE_PATTERN = r'image_location_test: "(?P<name>[^"]+)"'
GOLDEN_PATH_LINE_PATTERN = r'image_location_golden: "(?P<name>[^"]+)"'

def main():
  parser = argparse.ArgumentParser(
      'Update golden screenshots files in a temporary directory.')
  parser.add_argument(
      '--android-build-top',
      default='main',
      help='The Android build top directory.')
  parser.add_argument(
      '--source-directory',
      default='',
      help='The directory where all proto buffer and screenshot files are located.')

  args = parser.parse_args()

  if not args.source_directory:
    print('The directory which contain all proto buffer and screenshot files must be specified.')
    sys.exit(1)

  source_directory = os.path.normpath(args.source_directory)

  if not os.path.exists(source_directory):
    print('The source directory does not exist: ' + source_directory)
    sys.exit(1)

  if not os.path.isdir(source_directory):
    print('The specified path is not a directory: ' + source_directory)
    sys.exit(1)

  # Due to file renaming, a map from the desired actual screenshot file name
  # needs to be mapped to the file name after renaming should be pre-built.
  actual_screenshot_files = {}
  for filename in os.listdir(args.source_directory):
    if not (filename.endswith('.png') or filename.find('_actual') < 0):
      continue
    actual_screenshot_files[get_stripped_actual_screenshot_file(filename)] = filename

  for filename in os.listdir(args.source_directory):
    if not (filename.endswith('.txt') or filename.endswith('.textpb')):
      continue

    pb_file = open(os.path.join(args.source_directory, filename), 'r')
    actual_screenshot_file_name = ''
    golden_path = ''

    # Parse all text protos which contain information of golden path and actual
    # screenshot filenames.
    for lines in pb_file.readlines():
      match_actual = re.search(ACTUAL_SCREENSHOT_FILE_LINE_PATTERN, lines)
      if match_actual:
        actual_screenshot_file_name = match_actual.group('name')
        continue
      match_golden = re.search(GOLDEN_PATH_LINE_PATTERN, lines)
      if match_golden:
        golden_path = match_golden.group('name')

    if not golden_path or not actual_screenshot_file_name:
      print(
          f'Golden path: {golden_path} or '
          f'Actual screenshot name: {actual_screenshot_file_name} is empty')
      continue

    # Copy the actual file to the destination.
    src_path = os.path.join(
        args.source_directory,
        actual_screenshot_files[actual_screenshot_file_name])
    dest_path = os.path.join(pathlib.Path.home(), args.android_build_top, golden_path)
    shutil.copyfile(src_path, dest_path)
    print(f'Updated {dest_path}')

def get_stripped_actual_screenshot_file(original_file_name: str) -> str:
  first_index = original_file_name.find('.png_')
  if first_index < 0:
    return original_file_name
  else:
    return original_file_name[:first_index] + '.png'

if __name__ == '__main__':
  main()
