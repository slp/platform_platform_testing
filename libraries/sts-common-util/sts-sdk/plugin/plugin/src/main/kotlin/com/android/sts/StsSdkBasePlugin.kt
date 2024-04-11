/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.sts

import org.gradle.api.Plugin
import org.gradle.api.Project

class StsSdkBasePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Export a configuration for users to add dependencies.
        // "stsSdkTestResource" represents Soong modules for the Tradefed testcases/ directory.
        // See "android_test_helper_app" and "cc_test" Soong modules.
        // Dependencies should map 1:1 to tradefed Android.bp files for easy reconstruction.
        project.configurations.create("stsSdkTestResource") { config ->
            config.isCanBeConsumed = true
            config.isCanBeResolved = false
        }
    }

    class ModuleManifest<T>(project: Project, type: String, resource: T) {
        val name: String = project.name
        val type: String = type
        val resource: T = resource
    }
}
