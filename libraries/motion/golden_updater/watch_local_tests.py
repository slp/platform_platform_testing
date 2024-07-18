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
import datetime


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
        "--serial",
        default=os.environ.get("ANDROID_SERIAL"),
        help="The ADB device serial to pull goldens from.",
    )

    parser.add_argument(
        "--android_build_top",
        default=os.environ.get("ANDROID_BUILD_TOP"),
        help="The root directory of the android checkout.",
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

    serial = args.serial
    if not serial:
        devices_response = subprocess.run(
            ["adb", "devices"], check=True, capture_output=True
        ).stdout.decode("utf-8")
        lines = [s for s in devices_response.splitlines() if s.strip()]

        if len(lines) == 1:
            print("no adb devices found")
            sys.exit(1)

        if len(lines) > 2:
            print("multiple adb devices found, specify --serial")
            sys.exit(1)

        serial = lines[1].split("\t")[0]

    adb_client = AdbClient(serial)
    if not adb_client.run_as_root():
        sys.exit(1)

    global android_build_top
    android_build_top = args.android_build_top

    with tempfile.TemporaryDirectory() as tmpdir:
        global golden_watcher, this_server_address
        golden_watcher = GoldenFileWatcher(tmpdir, adb_client)

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
                httpd.shutdown()
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
            return
        elif parsed.path.startswith("/golden/"):
            requested_file_start_index = parsed.path.find("/", len("/golden/") + 1)
            requested_file = parsed.path[requested_file_start_index + 1 :]
            print(requested_file)
            self.serve_file(golden_watcher.temp_dir, requested_file)
            return
        elif parsed.path.startswith("/expected/"):
            golden_id = parsed.path[len("/expected/") :]
            print(golden_id)

            goldens = golden_watcher.cached_goldens.values()
            for golden in goldens:
                if golden.id != golden_id:
                    continue

                self.serve_file(
                    android_build_top, golden.golden_repo_path, "application/json"
                )
                return

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

    def serve_file(self, root_directory, file_relative_to_root, mime_type=None):
        resolved_path = path.abspath(path.join(root_directory, file_relative_to_root))

        print(resolved_path)
        print(root_directory)

        if path.commonprefix(
            [resolved_path, root_directory]
        ) == root_directory and path.isfile(resolved_path):
            self.send_response(200)
            self.send_header(
                "Content-type", mime_type or mimetypes.guess_type(resolved_path)[0]
            )
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
            golden_data["testClassName"] = golden.test_class_name
            golden_data["testMethodName"] = golden.test_method_name
            golden_data["testTime"] = golden.test_time

            golden_data["actualUrl"] = (
                f"{this_server_address}/golden/{golden.checksum}/{golden.local_file[len(golden_watcher.temp_dir) + 1 :]}"
            )
            expected_file = path.join(android_build_top, golden.golden_repo_path)
            if os.path.exists(expected_file):
                golden_data["expectedUrl"] = (
                    f"{this_server_address}/expected/{golden.id}"
                )

            golden_data["videoUrl"] = (
                f"{this_server_address}/golden/{golden.checksum}/{golden.video_location}"
            )

            goldens_list.append(golden_data)

        self.send_json(goldens_list)

    def service_refresh_goldens(self, clear):
        if clear:
            golden_watcher.clean()
        golden_watcher.refresh_golden_files()
        self.service_list_goldens()

    def service_update_golden(self, id):
        goldens = golden_watcher.cached_goldens.values()
        for golden in goldens:
            if golden.id != id:
                print("skip", golden.id)
                continue

            shutil.copyfile(
                golden.local_file,
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
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "POST, PUT, GET, OPTIONS")
        self.send_header(
            "Access-Control-Allow-Headers",
            GOLDEN_ACCESS_TOKEN_HEADER
            + ", Content-Type, Content-Length, Range, Accept-ranges",
        )
        # Accept-ranges: bytes is needed for chrome to allow seeking the
        # video. At this time, won't handle ranges on subsequent gets,
        # but that is likely OK given the size of these videos and that
        # its local only.
        self.send_header("Accept-ranges", "bytes")


class GoldenFileWatcher:

    def __init__(self, temp_dir, adb_client):
        self.temp_dir = temp_dir
        self.adb_client = adb_client

        # name -> CachedGolden
        self.cached_goldens = {}
        self.refresh_golden_files()

    def clean(self):
        self.cached_goldens = {}

    def refresh_golden_files(self):
        command = f"find /data/user/0/ -type f -name *.actual.json"
        updated_goldens = self.run_adb_command(["shell", command]).splitlines()
        print(f"Updating goldens - found {len(updated_goldens)} files")

        for golden_remote_file in updated_goldens:
            local_file = self.adb_pull(golden_remote_file)

            golden = CachedGolden(golden_remote_file, local_file)
            if golden.video_location:
                self.adb_pull_image(golden.device_local_path, golden.video_location)

            self.cached_goldens[golden_remote_file] = golden

    def adb_pull(self, remote_file):
        local_file = os.path.join(self.temp_dir, os.path.basename(remote_file))
        self.run_adb_command(["pull", remote_file, local_file])
        self.run_adb_command(["shell", "rm", remote_file])
        return local_file

    def adb_pull_image(self, remote_dir, remote_file):
        remote_path = os.path.join(remote_dir, remote_file)
        local_path = os.path.join(self.temp_dir, remote_file)
        os.makedirs(os.path.dirname(local_path), exist_ok=True)
        self.run_adb_command(["pull", remote_path, local_path])
        self.run_adb_command(["shell", "rm", remote_path])
        return local_path

    def run_adb_command(self, args):
        return self.adb_client.run_adb_command(args)


class CachedGolden:

    def __init__(self, remote_file, local_file):
        self.id = hashlib.md5(remote_file.encode("utf-8")).hexdigest()
        self.remote_file = remote_file
        self.local_file = local_file
        self.updated = False
        self.test_time = datetime.datetime.now().isoformat()
        # Checksum is the time the test data was loaded, forcing unique URLs
        # every time the golden is reloaded
        self.checksum = hashlib.md5(self.test_time.encode("utf-8")).hexdigest()

        motion_golden_data = None
        with open(local_file, "r") as json_file:
            motion_golden_data = json.load(json_file)
        metadata = motion_golden_data["//metadata"]

        self.result = metadata["result"]
        self.golden_repo_path = metadata["goldenRepoPath"]
        self.golden_identifier = metadata["goldenIdentifier"]
        self.test_class_name = metadata["testClassName"]
        self.test_method_name = metadata["testMethodName"]
        self.device_local_path = metadata["deviceLocalPath"]
        self.video_location = None
        if "videoLocation" in metadata:
            self.video_location = metadata["videoLocation"]

        with open(local_file, "w") as json_file:
            del motion_golden_data["//metadata"]
            json.dump(motion_golden_data, json_file, indent=2)


class AdbClient:
    def __init__(self, adb_serial):
        self.adb_serial = adb_serial

    def run_as_root(self):
        root_result = self.run_adb_command(["root"])
        if "restarting adbd as root" in root_result:
            self.wait_for_device()
            return True
        if "adbd is already running as root" in root_result:
            return True

        print(f"run_as_root returned [{root_result}]")

        return False

    def wait_for_device(self):
        self.run_adb_command(["wait-for-device"])

    def run_adb_command(self, args):
        command = ["adb"]
        command += ["-s", self.adb_serial]
        command += args
        return subprocess.run(command, check=True, capture_output=True).stdout.decode(
            "utf-8"
        )


def find_free_port():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind(("", 0))  # Bind to a random free port provided by the OS
        return s.getsockname()[1]  # Get the port number


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
