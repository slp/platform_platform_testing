#!/usr/bin/env python3
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

"""Preupload hook to perform necessary checks and formatting on python files."""

import argparse
import concurrent.futures
import multiprocessing
import pathlib
import shlex
import subprocess
import sys


def _filter_python_files(files: list[pathlib.Path]) -> list[pathlib.Path]:
  """Filter a list of files and return a new list of python files only."""
  return [file for file in files if file.suffix == '.py']


def _check_run_shell_command(cmd: str, cwd: str = None) -> None:
  """Run a shell command and raise error if failed."""
  if subprocess.run(shlex.split(cmd), cwd=cwd, check=False).returncode:
    print('Preupload files did not pass Asuite preupload hook script.')
    sys.exit(1)


def _run_python_lint(lint_bin: str, files: list[pathlib.Path]) -> None:
  """Run python lint binary on python files."""
  run_lint_on_file = lambda file: subprocess.run(
      shlex.split(f'{lint_bin} {file.as_posix()}'),
      check=False,
      capture_output=True,
  )

  cpu_count = multiprocessing.cpu_count()
  with concurrent.futures.ThreadPoolExecutor(max_workers=cpu_count) as executor:
    completed_processes = executor.map(
        run_lint_on_file, _filter_python_files(files)
    )

  has_format_issue = False
  for process in completed_processes:
    if not process.returncode:
      continue
    print(process.stdout.decode())
    has_format_issue = True

  if has_format_issue:
    sys.exit(1)


def _run_pylint(files: list[pathlib.Path]) -> None:
  """Run pylint on python files."""
  _run_python_lint('pylint', files)


def _run_gpylint(files: list[pathlib.Path]) -> None:
  """Run gpylint on python files if gpylint is available."""
  if subprocess.run(
      shlex.split('which gpylint'),
      check=False,
  ).returncode:
    print('gpylint not available. Will use pylint instead.')
    _run_pylint(files)
    return

  _run_python_lint('gpylint', files)


def _run_pyformat(files: list[pathlib.Path]) -> None:
  """Run pyformat on certain projects."""
  if subprocess.run(
      shlex.split('which pyformat'),
      check=False,
  ).returncode:
    print('pyformat not available. Will skip auto formatting.')
    return

  def _run_pyformat_on_file(file):
    completed_process = subprocess.run(
        shlex.split('pyformat --force_quote_type single ' + file.as_posix()),
        capture_output=True,
        check=False,
    )

    if completed_process.stdout:
      subprocess.run(
          shlex.split(
              'pyformat -i --force_quote_type single ' + file.as_posix()
          ),
          check=False,
      )
      return True
    return False

  cpu_count = multiprocessing.cpu_count()
  with concurrent.futures.ThreadPoolExecutor(max_workers=cpu_count) as executor:
    need_reformat = executor.map(
        _run_pyformat_on_file, _filter_python_files(files)
    )

  if any(need_reformat):
    print(
        'Reformatting completed. Please add the modified files to git and rerun'
        ' the repo preupload hook.'
    )
    sys.exit(1)


def get_preupload_files() -> list[pathlib.Path]:
  """Get the list of files to be uploaded."""
  parser = argparse.ArgumentParser()
  parser.add_argument('preupload_files', nargs='*', help='Files to upload.')
  args = parser.parse_args()
  files_to_upload = args.preupload_files
  if not files_to_upload:
    # When running by users directly, only consider:
    # added(A), renamed(R) and modified(M) files
    # and store them in files_to_upload.
    cmd = "git status --short | egrep [ARM] | awk '{print $NF}'"
    files_to_upload = subprocess.check_output(
        cmd, shell=True, encoding='utf-8'
    ).splitlines()
    if files_to_upload:
      print('Modified files: %s' % files_to_upload)
  file_paths_to_upload = [
      pathlib.Path(file).resolve() for file in files_to_upload
  ]
  return [file for file in file_paths_to_upload if file.exists()]


if __name__ == '__main__':
  preupload_files = get_preupload_files()
  _run_pyformat(preupload_files)
  _run_gpylint(preupload_files)
