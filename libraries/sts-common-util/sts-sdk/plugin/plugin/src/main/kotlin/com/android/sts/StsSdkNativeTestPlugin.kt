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

import com.android.build.gradle.LibraryExtension
import java.io.File
import org.gradle.api.Plugin
import org.gradle.api.Project

enum class Bitness(var string: String) {
    BITNESS_MULTI("multi"),
    BITNESS_32("32"),
    BITNESS_64("64"),
}

open class NativeTestExtension {
    var name: String? = null
    var minSdk: Int? = null
    var targetSdk: Int? = null
    var compileSdk: Int? = null
    var srcs: List<String> = listOf()
    var includeDirs: List<String> = listOf()
    var sharedLibs: List<String> = listOf()
    var headerLibs: List<String> = listOf()
    // tools/base/build-system/gradle-api/src/main/java/com/android/build/api/dsl/CmakeFlags.kt
    var cflags: List<String> = listOf()
}

class StsSdkNativeTestPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val nativeTest = project.extensions.create("nativeTest", NativeTestExtension::class.java)

        project.afterEvaluate {
            project.plugins.apply("com.android.library")
            project.extensions.configure<LibraryExtension>("android") {
                it.namespace = "com.android.sts"
                it.compileSdk = nativeTest.compileSdk
                it.externalNativeBuild.cmake.path = File("CMakeLists.txt")
                // don't need to set externalNativeBuild.cmake.version because it will automatically
                // be handled
                it.defaultConfig.targetSdk = nativeTest.targetSdk
                it.defaultConfig.minSdk = nativeTest.minSdk
            }
        }
    }
}
