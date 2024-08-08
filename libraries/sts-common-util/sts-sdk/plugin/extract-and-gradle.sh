#!/bin/bash

# Because the build environment is not passed to this script, please `m sts-sdk`.

# usage:
# $ m sts-sdk && ./extract-and-gradle.sh *commands*

# publish to local; useful for testing examples:
# $ m sts-sdk && ./extract-and-gradle.sh publishToMavenLocal

# build only:
# $ m sts-sdk && ./extract-and-gradle.sh assemble

# build and test:
# $ m sts-sdk && ./extract-and-gradle.sh build

# Exit on error
set -e

if [ -z "${ANDROID_HOST_OUT}" ]; then
  echo "ANDROID_HOST_OUT is not set. Did you source and lunch?"
  exit 1
fi

STS_SDK_OUT=/tmp/sts-sdk

# Remove output directory.
# This breaks incremental compile but in practice the difference is negligible.
rm -rf $STS_SDK_OUT

# -q quiet
# -o overwrite without prompting
# -d output directory
unzip -q -o $ANDROID_HOST_OUT/sts-sdk/sts-sdk.zip -d $STS_SDK_OUT

cd $STS_SDK_OUT
./gradlew $@
