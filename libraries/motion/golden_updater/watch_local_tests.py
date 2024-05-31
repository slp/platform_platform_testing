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

import http.server
import socketserver
import json
import re
import urllib.parse
from os import path
import socket
import argparse
import os
import subprocess
import sys
import tempfile
import webbrowser
import mimetypes
import hashlib
import shutil
import secrets

from collections import defaultdict


def main():
    parser = argparse.ArgumentParser(
        "Watches a connected device for golden file updates."
    )

    parser.add_argument(
        "--port",
        default=find_free_port(),
        type=int,
        help="Port to run test at watcher web UI on.",
    )
    parser.add_argument(
        "--serial", default="", help="The ADB device serial to pull goldens from."
    )

    parser.add_argument(
        "--watch",
        nargs="*",
        action="append",
        help="package:subdirectory where motion goldens are expected.",
    )

    parser.add_argument(
        "--android_build_top",
        default=os.environ.get("ANDROID_BUILD_TOP"),
        help="The root directory of the android checkout.",
    )

    parser.add_argument(
        "--clean",
        default=False,
        type=bool,
        help="Whether to clean the golden directory on device at startup.",
    )

    parser.add_argument(
        "--client_url",
        default="http://motion.teams.x20web.corp.google.com/",
        help="The URL where the client app is deployed.",
    )

    args = parser.parse_args()

    if args.android_build_top is None or not os.path.exists(args.android_build_top):
        print("ANDROID_BUILD_TOP not set. Have you sourced envsetup.sh?")
        sys.exit(1)

    global android_build_top
    android_build_top = args.android_build_top

    with tempfile.TemporaryDirectory() as tmpdir:
        global golden_watcher, this_server_address
        golden_watcher = GoldenFileWatcher(tmpdir, args.serial)

        for dir in golden_watcher.list_golden_output_directories():
            golden_watcher.add_remote_dir(dir)

        if args.watch is not None:
            for watching in args.watch:
                parts = watching.split(":", 1)
                package, output_dir = parts
                if len(parts) == 2:
                    golden_watcher.add_remote_dir(
                        f"/data/user/0/{package}/files/{output_dir}/"
                    )
                else:
                    print(f"skipping wrongly formatted watch arg [{watching}]")

        if args.clean:
            golden_watcher.clean()

        this_server_address = f"http://localhost:{args.port}"

        with socketserver.TCPServer(
            ("localhost", args.port), WatchWebAppRequestHandler, golden_watcher
        ) as httpd:
            uiAddress = f"{args.client_url}?token={secret_token}&port={args.port}"
            print(f"Open UI at {uiAddress}")
            webbrowser.open(uiAddress)
            try:
                httpd.serve_forever()
            except KeyboardInterrupt:
                print("Shutting down")


GOLDEN_ACCESS_TOKEN_HEADER = "Golden-Access-Token"
GOLDEN_ACCESS_TOKEN_LOCATION = os.path.expanduser("~/.config/motion-golden/.token")

secret_token = None
android_build_top = None
golden_watcher = None
this_server_address = None


class WatchWebAppRequestHandler(http.server.BaseHTTPRequestHandler):

    def __init__(self, *args, **kwargs):
        self.root_directory = path.abspath(path.dirname(__file__))
        super().__init__(*args, **kwargs)

    def verify_access_token(self):
        token = self.headers.get(GOLDEN_ACCESS_TOKEN_HEADER)
        if not token or token != secret_token:
            self.send_response(403, "Bad authorization token!")
            return False

        return True

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header("Allow", "GET,POST,PUT")
        self.add_standard_headers()
        self.end_headers()
        self.wfile.write(b"GET,POST,PUT")

    def do_GET(self):

        parsed = urllib.parse.urlparse(self.path)

        if parsed.path == "/service/list":
            self.service_list_goldens()
        elif parsed.path.startswith("/golden/"):
            requested_file_start_index = parsed.path.find("/", len("/golden/") + 1)
            requested_file = parsed.path[requested_file_start_index + 1 :]
            print(requested_file)
            self.serve_file(golden_watcher.temp_dir, requested_file)
        else:
            self.send_error(404)

    def do_POST(self):
        if not self.verify_access_token():
            return

        content_type = self.headers.get("Content-Type")

        # refuse to receive non-json content
        if content_type != "application/json":
            self.send_response(400)
            return

        length = int(self.headers.get("Content-Length"))
        message = json.loads(self.rfile.read(length))

        parsed = urllib.parse.urlparse(self.path)
        if parsed.path == "/service/refresh":
            self.service_refresh_goldens(message["clear"])
        else:
            self.send_error(404)

    def do_PUT(self):
        if not self.verify_access_token():
            return

        parsed = urllib.parse.urlparse(self.path)
        query_params = urllib.parse.parse_qs(parsed.query)

        if parsed.path == "/service/update":
            self.service_update_golden(query_params["id"][0])
        else:
            self.send_error(404)

    def serve_file(self, root_directory, file_relative_to_root):
        resolved_path = path.abspath(path.join(root_directory, file_relative_to_root))

        print(resolved_path)
        print(root_directory)

        if path.commonprefix(
            [resolved_path, root_directory]
        ) == root_directory and path.isfile(resolved_path):
            self.send_response(200)
            self.send_header("Content-type", mimetypes.guess_type(resolved_path)[0])
            self.add_standard_headers()
            self.end_headers()
            with open(resolved_path, "rb") as f:
                self.wfile.write(f.read())

        else:
            self.send_error(404)

    def service_list_goldens(self):
        if not self.verify_access_token():
            return

        goldens_list = []

        for golden in golden_watcher.cached_goldens.values():

            golden_data = {}
            golden_data["id"] = golden.id
            golden_data["result"] = golden.result
            golden_data["label"] = golden.golden_identifier
            golden_data["goldenRepoPath"] = golden.golden_repo_path
            golden_data["updated"] = golden.updated

            if isinstance(golden, CachedMotionGolden):
                golden_data["type"] = "motion"
                golden_data["actualUrl"] = (
                    f"{this_server_address}/golden/{golden.checksum}/{golden.local_file[len(golden_watcher.temp_dir) + 1 :]}"
                )

                filmstrip_golden = golden.findFilmstrip()
                if filmstrip_golden is not None:
                    golden_data["filmstripUrl"] = (
                        f"{this_server_address}/golden/{filmstrip_golden.checksum}/{filmstrip_golden.actual_image}"
                    )

            elif (
                isinstance(golden, CachedScreenshotGolden)
                and not golden.is_debug_filmstrip
            ):
                golden_data["type"] = "screenshot"

                golden_data["label"] = golden.golden_identifier
                golden_data["actualUrl"] = (
                    f"{this_server_address}/golden/{golden.checksum}/{golden.actual_image}"
                )
                if golden.expected_image:
                    golden_data["expectedUrl"] = (
                        f"{this_server_address}/golden/{golden.checksum}/{golden.expected_image}"
                    )
                if golden.diff_image:
                    golden_data["diffUrl"] = (
                        f"{this_server_address}/golden/{golden.checksum}/{golden.diff_image}"
                    )

            else:
                continue

            goldens_list.append(golden_data)

        self.send_json(goldens_list)

    def service_refresh_goldens(self, clear):
        if clear:
            golden_watcher.clean()
        golden_watcher.refresh_all_golden_files()
        self.service_list_goldens()

    def service_update_golden(self, id):
        goldens = golden_watcher.cached_goldens.values()
        for golden in goldens:
            if golden.id != id:
                print("skip", golden.id)
                continue

            if isinstance(golden, CachedMotionGolden):
                shutil.copyfile(
                    golden.local_file,
                    path.join(android_build_top, golden.golden_repo_path),
                )

                golden.updated = True
                self.send_json({"result": "OK"})
                return
            elif isinstance(golden, CachedScreenshotGolden):
                shutil.copyfile(
                    path.join(golden_watcher.temp_dir, golden.actual_image),
                    path.join(android_build_top, golden.golden_repo_path),
                )

                golden.updated = True
                self.send_json({"result": "OK"})
                return

        self.send_error(400)

    def send_json(self, data):
        # Replace this with code that generates your JSON data
        data_encoded = json.dumps(data).encode("utf-8")
        self.send_response(200)
        self.send_header("Content-type", "application/json")
        self.add_standard_headers()
        self.end_headers()
        self.wfile.write(data_encoded)

    def add_standard_headers(self):
        self.send_header("Cache-Control", "no-cache, no-store, must-revalidate")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "POST, PUT, GET, OPTIONS")
        self.send_header(
            "Access-Control-Allow-Headers",
            GOLDEN_ACCESS_TOKEN_HEADER + ", Content-Type, Content-Length",
        )
        self.send_header("Access-Control-Expose-Headers", "Winscope-Proxy-Version")


class GoldenFileWatcher:

    def __init__(self, temp_dir, adb_serial):
        self.temp_dir = temp_dir
        self.adb_serial = adb_serial
        self.remote_dirs = set()

        # name -> CachedGolden
        self.cached_goldens = {}

    def add_remote_dir(self, remote_dir):
        self.remote_dirs.add(remote_dir)
        self.refresh_golden_files(remote_dir)

    def list_golden_output_directories(self):
        marker_name = ".motion_test_output_marker"
        command = f"find /data/user/0/ -type f -name {marker_name}"

        files = self.run_adb_command(["shell", command]).splitlines()
        print(f"Found {len(files)} motion directories")

        return [name[: -len(marker_name)] for name in files]

    def clean(self):
        self.cached_goldens = {}
        for remote_path in self.remote_dirs:
            self.run_adb_command(["shell", f"rm -rf {remote_path}"])

    def refresh_all_golden_files(self):
        for remote_path in self.remote_dirs:
            self.refresh_golden_files(remote_path)

    def refresh_golden_files(self, remote_dir):

        updated_goldens = self.list_golden_files(remote_dir)

        for golden_remote_file in updated_goldens:

            local_file = self.adb_pull(golden_remote_file)

            golden = None
            if local_file.endswith(".json"):
                golden = self.motion_golden(golden_remote_file, local_file)
            elif local_file.endswith("goldResult.textproto"):
                golden = self.screenshot_golden(
                    remote_dir, golden_remote_file, local_file
                )

            if golden != None:
                self.cached_goldens[golden_remote_file] = golden
            else:
                print(f"skipping unknonwn golden ")

    def motion_golden(self, remote_file, local_file):

        golden = CachedMotionGolden(remote_file, local_file)
        golden.checksum = hashlib.md5(open(local_file, "rb").read()).hexdigest()
        return golden

    def screenshot_golden(self, remote_dir, remote_file, local_file):
        golden = CachedScreenshotGolden(remote_file, local_file)

        if golden.actual_image:
            local_actual_image = self.adb_pull_image(remote_dir, golden.actual_image)
            golden.checksum = hashlib.md5(
                open(local_actual_image, "rb").read()
            ).hexdigest()
        if golden.expected_image:
            self.adb_pull_image(remote_dir, golden.expected_image)
        if golden.diff_image:
            self.adb_pull_image(remote_dir, golden.diff_image)

        return golden

    def list_golden_files(self, remote_dir):
        print(f"Polling for updated goldens")

        command = f"find {remote_dir} -type f \\( -name *.json -o -name *.textproto \\)"

        files = self.run_adb_command(["shell", command]).splitlines()
        print(f"Found {len(files)} files")

        return files

    def adb_pull(self, remote_file):
        local_file = os.path.join(self.temp_dir, os.path.basename(remote_file))
        self.run_adb_command(["pull", remote_file, local_file])
        self.run_adb_command(["shell", "rm", remote_file])
        return local_file

    def adb_pull_image(self, remote_dir, remote_file):
        remote_path = os.path.join(remote_dir, remote_file)
        local_path = os.path.join(self.temp_dir, remote_file)
        self.run_adb_command(["pull", remote_path, local_path])
        self.run_adb_command(["shell", "rm", remote_path])
        return local_path

    def run_adb_command(self, args):
        command = ["adb"]
        if self.adb_serial:
            command += ["-s", self.adb_serial]
        command += args
        return subprocess.run(command, check=True, capture_output=True).stdout.decode(
            "utf-8"
        )


class CachedGolden:

    def __init__(self, remote_file, local_file):
        self.id = hashlib.md5(remote_file.encode("utf-8")).hexdigest()
        self.remote_file = remote_file
        self.local_file = local_file
        self.updated = False
        self.checksum = "0"


class CachedMotionGolden(CachedGolden):

    def __init__(self, remote_file, local_file):
        motion_golden_data = None
        with open(local_file, "r") as json_file:
            motion_golden_data = json.load(json_file)
        metadata = motion_golden_data["//metadata"]

        self.result = metadata["result"]
        self.golden_repo_path = metadata["goldenRepoPath"]
        self.golden_identifier = metadata["goldenIdentifier"]
        self.test_identifier = metadata["filmstripTestIdentifier"]

        with open(local_file, "w") as json_file:
            del motion_golden_data["//metadata"]
            json.dump(motion_golden_data, json_file, indent=2)

        super().__init__(remote_file, local_file)

    def findFilmstrip(self):
        for golden in golden_watcher.cached_goldens.values():
            if not isinstance(golden, CachedScreenshotGolden):
                continue
            if not golden.is_debug_filmstrip:
                continue
            if golden.actual_image.find("motion_debug_filmstrip_") == -1:
                continue
            if golden.golden_repo_path.find(self.golden_identifier) >= 0:
                return golden

        return None


class CachedScreenshotGolden(CachedGolden):

    def __init__(self, remote_file, local_file):
        self.is_debug_filmstrip = local_file.find("motion_debug_filmstrip_") >= 0

        metadata = parse_text_proto(local_file)

        self.golden_repo_path = metadata["image_location_golden"]
        self.actual_image = metadata["image_location_test"]

        match = re.search(r"_actual_(.*?)\.png$", self.actual_image)
        if match:
            self.golden_identifier = match.group(1)

        self.expected_image = metadata["image_location_reference"]
        self.diff_image = metadata["image_location_diff"]
        self.result = metadata["result_type"]

        super().__init__(remote_file, local_file)


def find_free_port():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind(("", 0))  # Bind to a random free port provided by the OS
        return s.getsockname()[1]  # Get the port number


def parse_text_proto(filename):
    data = defaultdict(dict)
    level = 0

    with open(filename, "r") as file:
        for line in file:
            line = line.strip()

            if line.endswith("{"):
                level += 1
                continue

            if line == "}":
                level -= 1
                continue

            # not consuming nested messages for now
            if not line or line.startswith("#") or level > 0:
                continue

            key, value = line.split(":", 1)
            if not key or not value:
                continue

            key, value = key.strip(), value.strip()

            if value.startswith('"') and value.endswith('"'):
                value = value[1:-1]

            data[key] = value

    return data


def get_token() -> str:
    try:
        with open(GOLDEN_ACCESS_TOKEN_LOCATION, "r") as token_file:
            token = token_file.readline()
            return token
    except IOError:
        token = secrets.token_hex(32)
        os.makedirs(os.path.dirname(GOLDEN_ACCESS_TOKEN_LOCATION), exist_ok=True)
        try:
            with open(GOLDEN_ACCESS_TOKEN_LOCATION, "w") as token_file:
                token_file.write(token)
            os.chmod(GOLDEN_ACCESS_TOKEN_LOCATION, 0o600)
        except IOError:
            print(
                "Unable to save persistent token {} to {}".format(
                    token, GOLDEN_ACCESS_TOKEN_LOCATION
                )
            )
        return token


if __name__ == "__main__":
    secret_token = get_token()
    main()
