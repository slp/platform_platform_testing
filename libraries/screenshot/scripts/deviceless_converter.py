#!/usr/bin/env python3

# Step to run:
# 1. run . build/envsetup.sh; lunch ...
# 2. Run this script from inside a terminal in sysui studio, as it needs JAVA_HOME to be setup.

import argparse, itertools, os, re, subprocess
from collections.abc import Generator
from modules_config import Config, ModuleConfig, TestModuleMapping

def runGradleCommand(outputFile: str, cfg: Config, mc: ModuleConfig) -> int:
    gradlew_dir: String = os.path.join(cfg.build_top, mc.gradlew_location)
    with open(os.path.expanduser(outputFile), "w") as of:
        ret = subprocess.run([os.path.join(gradlew_dir, "gradlew"), f"{mc.gradlew_target}"],
                             stdout=of, stderr=of, cwd=gradlew_dir)
        return ret.returncode

def check_java():
    expected_versions = ['17.0', '1.7', '21.0']
    java_home = os.environ.get('JAVA_HOME')
    if not java_home:
        print('JAVA_HOME is not set, exiting ...')
        exit (-2)
    java_binary = os.path.join(java_home, 'bin/java')
    print('Checking version for java binary:', java_binary)
    out = subprocess.check_output([java_binary, '-version'], stderr=subprocess.STDOUT).decode('utf-8')
    version = re.search(' version \"(\d+\.\d+).*\"', out).groups()[0]
    if not version in expected_versions:
        print('Your java version, version', version, ' does not match any of the expected versions', expected_versions)
        exit (-3)

def walkDir(dirname: str) -> Generator[str, None, None]:
    print("Processing dir:", dirname)
    for root, _, files in os.walk(dirname, topdown=False):
        for name in files:
            if not (name.endswith('.java') or name.endswith('.kt')):
                continue
            yield os.path.join(root, name)

def processFile(filename: str, cfg: Config, mc: ModuleConfig) -> None:
    print("Attempting roboify on: " , filename)

    # Move to multivalentTests
    new_filename = filename.replace("/tests/", "/tests/multivalentTests/")
    dirname = os.path.dirname(new_filename)
    basename = os.path.basename(new_filename)
    outputFile = f"{cfg.output_dir}/{basename}.err"
    os.makedirs(dirname, exist_ok=True)
    os.rename(filename, new_filename)

    # Attempt run
    exitValue = runGradleCommand(outputFile, cfg, mc)

    # Log and restore state if needed
    print("Test run ", "succeeded" if exitValue == 0  else "failed", " with exitValue: ", exitValue)
    if exitValue != 0 :
        os.rename(new_filename, filename)

    return exitValue == 0

def init():
    parser = argparse.ArgumentParser(
        description='This script attempts to convert device based unit tests to deviceless.')
    parser.add_argument('-m', '--module_pattern', required=True)
    check_java()
    return parser.parse_args()

def process(cfg: Config, successful: list[str], failed: list[str]):
    for mc in cfg.module_configs:
        for d in mc.dirs:
            for f in walkDir(os.path.join(cfg.build_top, d)):
                if any(excl in f for excl in mc.excludes):
                    print("Ignore file:", f)
                    continue
                ret = processFile(f, cfg, mc)
                (successful if ret else failed).append(f)

def main():
    args = init()
    successful: list[str] = []
    failed: list[str] = []

    module_configs = []

    for k, mc in TestModuleMapping.items():
        if not (args.module_pattern in k):
            continue
        print(f"Processing module:{k}, module_config:{mc}")
        module_configs.append(mc)

    if not module_configs:
        print(f"Could not match any modules that match: [{args.module_pattern}], exiting ...")
        exit(-5)

    cfg = Config(module_configs)
    if not cfg.build_top:
        print("ANDROID_BUILD_TOP env variable is not set, plase run lunch before invoking this script")
        exit(-1)

    process(cfg, successful, failed)

    print(f"Successes:\n{successful[:3]} ...")
    print(f"Failures:\n{failed}")
    return 0

if __name__ == "__main__":
    ret = main()
    exit(ret)
