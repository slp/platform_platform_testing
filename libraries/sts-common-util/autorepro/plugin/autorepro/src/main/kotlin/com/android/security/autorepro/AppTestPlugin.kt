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
package com.android.security.autorepro

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import com.google.gson.Gson
import java.io.BufferedWriter
import org.gradle.api.GradleException
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
    var minSdk: Int = 31 // oldest security-supported
    var targetSdk: Int = 34 // newest security-supported
    var compileSdk: Int = 34 // compile = target
}

class AppTestPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val namespace = "com.android.security"
        val appTest = project.extensions.create("appTest", AppTestExtension::class.java)

        val writeManifestTask =
            project.tasks.register("writeManifestTask") { task ->
                task.outputs.file(
                    project.layout.buildDirectory.file("autorepro/autorepro-manifest.json")
                )
                task.doLast {
                    val writer = BufferedWriter(task.outputs.files.singleFile.writer())
                    val moduleManifest =
                        PluginBase.ModuleManifest<AppTestExtension>(project, "AppTest", appTest)
                    writer.use { out -> out.write(Gson().toJson(moduleManifest)) }
                }
            }

        // Perform this work in the "afterEvaluate" callback because the extensions are not
        // initialized yet.
        project.afterEvaluate {
            project.plugins.apply("com.android.application")

            // https://developer.android.com/build/configure-app-module
            // "All characters must be alphanumeric or an underscore [a-zA-Z0-9_]."
            val nameRegex = """[a-zA-Z0-9_]+""".toRegex()
            if (!project.name.matches(nameRegex)) {
                throw GradleException("Project name doesn't match $nameRegex: ${project.name}")
            }

            // Because every module must be unique, append a placeholder to find/replace on import.
            // https://source.android.com/docs/setup/reference/androidbp
            // "every module must have a name property, and the value must be unique"
            val rename = project.name + "_AutoReproPlaceholder"

            // Copy our restricted-scope AppTestExtension into the normal Android extension
            // BaseAppModuleExtension is internal to the AGP, but is widely used by similar
            // extensions and unlikely to break
            project.extensions.configure<BaseAppModuleExtension>("android") {
                // Base on the Gradle project name so each APK is guaranteed to have unique id
                it.defaultConfig.applicationId = "$namespace.$rename"
                // Set the namespace to reduce refactoring requirements for STS integration
                // https://developer.android.com/build/configure-app-module#set-namespace
                it.namespace = namespace
                it.compileSdk = appTest.compileSdk
                it.defaultConfig.minSdk = appTest.minSdk
                it.defaultConfig.targetSdk = appTest.targetSdk
            }

            val implementationConfiguration = project.configurations.getByName("implementation")
            // TODO: https://github.com/gradle/gradle/issues/16634 - Can't use version catalog
            // because the extension is not registered by subproject or root project
            listOf(
                    "androidx.appcompat:appcompat:1.6.1",
                    "androidx.core:core:1.13.1",
                    "com.google.android.material:material:1.12.0",
                    "junit:junit:4.13.2",
                    "androidx.test.ext:junit:1.1.5",
                    "androidx.test.espresso:espresso-core:3.5.1",
                    "androidx.test.uiautomator:uiautomator:2.3.0",
                    "org.jetbrains.kotlin:kotlin-stdlib:1.8.22",
                )
                .forEach { dependencyString ->
                    implementationConfiguration.dependencies.add(
                        project.dependencies.create(dependencyString)
                    )
                }

            // Register callbacks for "onVariants" so that we can attach the APK to the
            // testResource configuration.
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
                    val copyAppTestcaseResourceTask =
                        project.tasks.register<Copy>(
                            "copyAppTestcaseResource-$variantName",
                            Copy::class.java,
                        ) { task ->
                            task.from(apkDir) {
                                // Rename to expected Soong module name
                                it.rename(".*\\.apk", rename + ".apk")
                                // Only include apks (glob, not regex)
                                it.include("*.apk")
                            }
                            task.into(project.layout.buildDirectory.dir("autorepro/testcases/"))
                        }

                    val copyAppTestcaseResourceTasks =
                        PluginBase.Abi.entries.map { abi -> Pair(abi, copyAppTestcaseResourceTask) }

                    // The APK artifact will be published to the "testResource" configuration.
                    PluginBase.applyConfiguration(
                        project = project,
                        sourceDirectoryArtifact = project.layout.projectDirectory.dir("src/main"),
                        manifestArtifact = writeManifestTask,
                        resourceArtifacts = copyAppTestcaseResourceTasks,
                    )
                }
            }
        }
    }
}
