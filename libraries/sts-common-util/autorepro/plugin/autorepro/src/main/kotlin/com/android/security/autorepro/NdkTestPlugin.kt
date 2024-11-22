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
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.gradle.LibraryExtension
import com.google.gson.Gson
import java.io.BufferedWriter
import java.io.File
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RelativePath
import org.gradle.api.tasks.Copy

open class NdkTestExtension {
    var minSdk: Int = 31 // oldest security-supported
    var targetSdk: Int = 34 // newest security-supported
    var compileSdk: Int = 34 // compile = target
}

class NdkTestPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val nativeTest = project.extensions.create("nativeTest", NdkTestExtension::class.java)

        val ndkTest = project.extensions.create("appTest", NdkTestExtension::class.java)

        val writeManifestTask =
            project.tasks.register("writeManifestTask") { task ->
                task.outputs.file(
                    project.layout.buildDirectory.file("autorepro/autorepro-manifest.json")
                )
                task.doLast {
                    val writer = BufferedWriter(task.outputs.files.singleFile.writer())
                    val moduleManifest =
                        PluginBase.ModuleManifest<NdkTestExtension>(project, "NdkTest", ndkTest)
                    writer.use { out -> out.write(Gson().toJson(moduleManifest)) }
                }
            }

        project.afterEvaluate {
            val rename = project.name + "_AutoReproPlaceholder"

            project.plugins.apply("com.android.library")
            project.extensions.configure<LibraryExtension>("android") {
                it.namespace = "com.android.security.autorepro"
                it.compileSdk = nativeTest.compileSdk
                it.externalNativeBuild.cmake.path = File("CMakeLists.txt")
                // externalNativeBuild.cmake.version will be automatically set
                it.defaultConfig.targetSdk = nativeTest.targetSdk
                it.defaultConfig.minSdk = nativeTest.minSdk

                // publish the pocs to the .aar artifact
                // https://developer.android.com/build/native-dependencies?agpversion=4.1#publish-native-libs-in-aars
                it.buildFeatures.prefabPublishing = true
                it.prefab.create(project.name)
            }

            val androidComponentsExtension =
                project.extensions.getByType(LibraryAndroidComponentsExtension::class.java)
            androidComponentsExtension.onVariants { variant ->
                val projectName = project.name
                val variantName = variant.name
                // Only select the "debug" variant
                if (variantName == "debug") {
                    val aarDir = variant.artifacts.get(SingleArtifact.AAR)
                    val copyNdkTestcaseResourceTasks =
                        PluginBase.Abi.entries.map { abi ->
                            val abiName = abi.attr.name
                            val aarPrefabPath =
                                "prefab/modules/$projectName/libs/android.$abiName/$projectName"
                            val copyNdkTestcaseResourceTask =
                                project.tasks.register<Copy>(
                                    "copyNdkTestcaseResource-$variantName-$abiName",
                                    Copy::class.java,
                                ) { task ->
                                    task.from(project.zipTree(aarDir)) { copySpec ->
                                        copySpec.include(aarPrefabPath)
                                        copySpec.rename(".*", "${rename}_sts${abi.attr.bitness}")
                                        copySpec.eachFile { fileCopyDetails ->
                                            // drop the path prefix
                                            fileCopyDetails.relativePath =
                                                RelativePath(
                                                    true /* endsWithFile */,
                                                    fileCopyDetails.relativePath.lastName,
                                                )
                                        }
                                        copySpec.includeEmptyDirs = false
                                    }
                                    task.into(
                                        project.layout.buildDirectory.dir(
                                            "autorepro/resources/$abiName"
                                        )
                                    )
                                }
                            Pair(abi, copyNdkTestcaseResourceTask)
                        }

                    // The artifact will be published to the "testResource" configuration.
                    PluginBase.applyConfiguration(
                        project = project,
                        sourceDirectoryArtifact = project.layout.projectDirectory.dir("src"),
                        manifestArtifact = writeManifestTask,
                        resourceArtifacts = copyNdkTestcaseResourceTasks,
                    )
                }
            }
        }
    }
}
