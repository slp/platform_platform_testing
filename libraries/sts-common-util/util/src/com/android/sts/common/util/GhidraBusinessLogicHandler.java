/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.sts.common.util;

import java.util.Optional;

/** GCL-accessible Business Logic utility for GhidraPreparer */
public class GhidraBusinessLogicHandler {
    private static String gitReleaseTagName; // eg. "Ghidra_11.0.1_build"
    private static String gitReleaseAssetName; // eg. "ghidra_11.0.1_PUBLIC_20240130.zip"

    public static Optional<String> getGitReleaseTagName() {
        return Optional.ofNullable(gitReleaseTagName);
    }

    public static Optional<String> getReleaseAssetName() {
        return Optional.ofNullable(gitReleaseAssetName);
    }

    public void setGitReleaseTagName(String tagName, String assetName) {
        gitReleaseTagName = tagName;
        gitReleaseAssetName = assetName;
    }
}
