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

import com.google.gson.Gson
import java.io.BufferedWriter
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy

class StsSdkJavaHostTestPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val pluginJarUrl = this.javaClass.getProtectionDomain().getCodeSource().getLocation()

        project.plugins.apply("java-library")
        val compileOnlyConfiguration = project.configurations.getByName("compileOnly")
        val dependencies =
            project.dependencies.create(
                project.zipTree(pluginJarUrl).matching { filter ->
                    filter.include(
                        listOf(
                            "sts-tradefed-tools/sts-tradefed.jar",
                            "sts-tradefed-tools/tradefed.jar",
                            "sts-tradefed-tools/compatibility-host-util.jar",
                            "sts-tradefed-tools/sts-host-util.jar",
                            "sts-tradefed-tools/hamcrest-library.jar",
                        )
                    )
                }
            )
        compileOnlyConfiguration.dependencies.add(dependencies)

        compileOnlyConfiguration.dependencies
        val copyTestcasesResourcesTask =
            project.tasks.register<Copy>("copyTestcasesResourcesJar", Copy::class.java) { task ->
                task.from(project.tasks.getByName("jar")) { copySpec ->
                    copySpec.rename(".*", "HostsideTest.jar")
                }
                task.from(project.zipTree(pluginJarUrl)) { copySpec ->
                    // only include the single file instead of all jar contents
                    copySpec.include("HostsideTest.config")
                }
                task.into(project.layout.buildDirectory.dir("android-sts/testcases"))
            }

        // The jar artifact will be published to the "stsSdkTestResource" configuration
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

        project.artifacts.add(stsSdkTestResourceConfiguration.name, copyTestcasesResourcesTask) {
            action ->
            // explicitly set the type to differentiate from other artifacts
            action.setType("resource")
        }

        val writeManifestTask =
            project.tasks.register("writeManifestTask") { task ->
                task.outputs.file(
                    project.layout.buildDirectory.file("sts-sdk/sts-sdk-manifest.json")
                )
                task.doLast {
                    val writer = BufferedWriter(task.outputs.files.singleFile.writer())
                    // Note: this module has no extension so using Kotlin Unit as void/null
                    val moduleManifest =
                        StsSdkBasePlugin.ModuleManifest<Unit>(project, "JavaHostTest", Unit)
                    writer.use { out -> out.write(Gson().toJson(moduleManifest)) }
                }
            }
        project.artifacts.add(stsSdkTestResourceConfiguration.name, writeManifestTask) { action ->
            // explicitly set the type to differentiate from other artifacts
            action.setType("manifest")
        }
    }
}
