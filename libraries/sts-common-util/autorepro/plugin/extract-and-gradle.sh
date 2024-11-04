#!/bin/bash

# Because the build environment is not passed to this script, please `m autorepro`.

# usage:
# $ m autorepro && ./extract-and-gradle.sh *commands*

# publish to local; useful for testing examples:
# $ m autorepro && ./extract-and-gradle.sh publishToMavenLocal

# build only:
# $ m autorepro && ./extract-and-gradle.sh assemble

# build and test:
# $ m autorepro && ./extract-and-gradle.sh build

# Exit on error
set -e

if [ -z "${ANDROID_HOST_OUT}" ]; then
  echo "ANDROID_HOST_OUT is not set. Did you source and lunch?"
  exit 1
fi

AUTOREPRO_OUT=/tmp/autorepro

# Remove output directory.
# This breaks incremental compile but in practice the difference is negligible.
rm -rf $AUTOREPRO_OUT

# -q quiet
# -o overwrite without prompting
# -d output directory
unzip -q -o $ANDROID_HOST_OUT/autorepro/autorepro.zip -d $AUTOREPRO_OUT

cd $AUTOREPRO_OUT
./gradlew $@
