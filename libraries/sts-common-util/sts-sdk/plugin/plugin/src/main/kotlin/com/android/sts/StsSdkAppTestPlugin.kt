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

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import com.google.gson.Gson
import java.io.BufferedWriter
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy

// This is a restricted version of the "android" extension block provided by the AGP
// The interface is restricted to simplify the translation between AGP and the Android Build System
// More fields can be added here in response to needs that arise
// All fields must be JSON serializable for inclusion in the submission zip
// https://developer.android.com/guide/app-bundle/configure-base
// See "android_test_helper_app" at the "Soong reference files"
// https://ci.android.com/builds/latest/branches/aosp-build-tools/targets/linux/view/soong_build.html
open class AppTestExtension {
    // Sets app namespace and applicationId
    // https://developer.android.com/build/configure-app-module#set-namespace
    var name: String? = null
    var minSdk: Int? = null
    var targetSdk: Int? = null
    var compileSdk: Int? = 34 // default to Android 14
}

class StsSdkAppTestPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val namespace = "android.security.sts"
        val appTest = project.extensions.create("appTest", AppTestExtension::class.java)

        // The APK artifact will be published to the "stsSdkTestResource" configuration
        project.plugins.apply("com.android.sts.sdk.base")
        val stsSdkTestResourceConfiguration = project.configurations.getByName("stsSdkTestResource")

        // Add src directory as artifact
        project.artifacts.add(
            stsSdkTestResourceConfiguration.name,
            project.layout.projectDirectory.dir("src/main")
        ) { action ->
            // explicitly set the type to differentiate from other artifacts
            action.setType("source")
        }

        // Perform this work in the "afterEvaluate" callback because the extensions are not
        // initialized yet.
        project.afterEvaluate {
            project.plugins.apply("com.android.application")

            // Copy our restricted-scope AppTestExtension into the normal Android extension
            // BaseAppModuleExtension is internal to the AGP, but is widely used by similar
            // extensions and unlikely to break
            project.extensions.configure<BaseAppModuleExtension>("android") {
                // Base the id on the Gradle project name so each APK is guaranteed to have unique
                // id
                it.defaultConfig.applicationId = namespace + '.' + project.name.lowercase()
                // Explicitly set the namespace to reduce refactoring requirements for STS
                // integration
                it.namespace = namespace
                it.compileSdk = appTest.compileSdk
                it.defaultConfig.minSdk = appTest.minSdk
                it.defaultConfig.targetSdk = appTest.targetSdk
            }

            // Register callbacks for "onVariants" so that we can attach the APK to the
            // stsSdkTestResource configuration
            // This is the technique used by the "gradle-play-publisher" plugin
            // https://developer.android.com/reference/tools/gradle-api/8.3/com/android/build/api/variant/AndroidComponentsExtension
            // The order of this task is sensitive because the callbacks must be registered before
            // they are called internally.
            // The best time to register the callbacks is immediately when applying the plugin.
            val androidComponentsExtension =
                project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
            androidComponentsExtension.onVariants(androidComponentsExtension.selector().all()) {
                variant ->
                // We only care about the debug variant for now; release APKs don't have any
                // features needed for STS.
                // If necessary, add a field to AppTestExtension to control this
                val variantName = variant.name
                if (variantName == "debug") {
                    // This is the same as the "outgoing variant" for "*RuntimeElement" secondary
                    // variant "apk"
                    // It's the dir containing the APK; typically "build/outputs/apk/debug"
                    val apkDir = variant.artifacts.get(SingleArtifact.APK)

                    val copyAppTestTestcaseResourceTask =
                        project.tasks.register<Copy>(
                            "copyAppTestTestcaseResource-$variantName",
                            Copy::class.java
                        ) { task ->
                            task.from(apkDir) {
                                val apkName = project.name.replaceFirstChar { it.uppercase() }
                                // Rename to expected Soong module name
                                it.rename(".*\\.apk", apkName + ".apk")
                                // Only include apks (glob, not regex)
                                it.include("*.apk")
                            }
                            task.into(project.layout.buildDirectory.dir("sts-sdk/testcases/"))
                        }

                    project.artifacts.add(
                        stsSdkTestResourceConfiguration.name,
                        copyAppTestTestcaseResourceTask
                    ) { action ->
                        // explicitly set the type to differentiate from other artifacts
                        action.setType("resource")
                    }

                    val writeManifestTask =
                        project.tasks.register("writeManifestTask-$variantName") { task ->
                            task.outputs.file(
                                project.layout.buildDirectory.file("sts-sdk/sts-sdk-manifest.json")
                            )
                            task.doLast {
                                val writer = BufferedWriter(task.outputs.files.singleFile.writer())
                                val moduleManifest =
                                    StsSdkBasePlugin.ModuleManifest<AppTestExtension>(
                                        project,
                                        "AppTest",
                                        appTest
                                    )
                                writer.use { out -> out.write(Gson().toJson(moduleManifest)) }
                            }
                        }
                    project.artifacts.add(
                        stsSdkTestResourceConfiguration.name,
                        writeManifestTask
                    ) { action ->
                        // explicitly set the type to differentiate from other artifacts
                        action.setType("manifest")
                    }
                }
            }
        }
    }
}
