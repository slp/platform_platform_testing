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

/** GCL-accessible Business Logic utility for FridaUtils */
public class FridaUtilsBusinessLogicHandler {
    // {0} = FRIDA_PACKAGE (e.g. 'frida-inject')
    // {1} = fridaVersionThreshold (e.g. '16.4.8')
    // {2} = FRIDA_OS (e.g. 'android')
    // {3} = fridaAbi (e.g. 'arm64')
    // Obtained from BusinessLogic, e.g. "{0}-{1}-{2}-{3}.xz"
    private static String fridaFilenameTemplate;
    private static String fridaVersionThreshold;

    public static String getFridaFilenameTemplate() {
        return fridaFilenameTemplate;
    }

    public void setFridaFilenameTemplate(String template) {
        fridaFilenameTemplate = template;
    }

    public static Optional<String> getFridaVersion() {
        return Optional.ofNullable(fridaVersionThreshold);
    }

    public void setFridaVersion(String version) {
        fridaVersionThreshold = version;
    }
}
