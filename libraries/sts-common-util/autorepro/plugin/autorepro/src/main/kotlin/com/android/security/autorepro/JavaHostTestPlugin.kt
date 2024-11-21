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

import com.google.gson.Gson
import java.io.BufferedWriter
import java.util.regex.Pattern
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSetContainer

class JavaHostTestPlugin : Plugin<Project> {
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

        val writeManifestTask =
            project.tasks.register("writeManifestTask") { task ->
                task.outputs.file(
                    project.layout.buildDirectory.file("autorepro/autorepro-manifest.json")
                )
                task.doLast {
                    val writer = BufferedWriter(task.outputs.files.singleFile.writer())
                    // Note: this module has no extension so using Kotlin Unit as void/null
                    val moduleManifest =
                        PluginBase.ModuleManifest<Unit>(project, "JavaHostTest", Unit)
                    writer.use { out -> out.write(Gson().toJson(moduleManifest)) }
                }
            }

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
        val copyTestcasesResourcesTasks =
            PluginBase.Abi.entries.map { abi -> Pair(abi, copyTestcasesResourcesTask) }

        // The artifact will be published to the "testResource" configuration.
        PluginBase.applyConfiguration(
            project = project,
            sourceDirectoryArtifact = project.layout.projectDirectory.dir("src/main"),
            manifestArtifact = writeManifestTask,
            resourceArtifacts = copyTestcasesResourcesTasks,
        )

        val verifyPackageNamesTask =
            project.tasks.register("verifyPackageNames") { task ->
                task.doLast {
                    val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
                    sourceSets.getByName("main").java.forEach { file ->
                        val expectedPackagePrefix = "com.android.security.autorepro_placeholder"
                        val fileText = file.readText()
                        // Extract the package from the Java file
                        val packageRegex = """^package\s+(?<package>[\w.]+);?$"""
                        val packageMatcher =
                            Pattern.compile(packageRegex, Pattern.MULTILINE).matcher(fileText)
                        if (!packageMatcher.find()) {
                            throw GradleException("Could not find package pattern in file: $file")
                        }
                        val packageName = packageMatcher.group("package")
                        if (packageName == null) {
                            throw GradleException("Could not find package name in file: $file")
                        }
                        if (!packageName.startsWith(expectedPackagePrefix)) {
                            throw GradleException(
                                "Package name doesn't start with \"$expectedPackagePrefix\" in " +
                                    "file: $file"
                            )
                        }
                    }
                }
            }

        val verifyResourcesTask =
            project.tasks.register("verifyResources") { task ->
                task.doLast {
                    val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
                    val resourcesSourceSet = sourceSets.getByName("main").resources
                    val expectedResourceDirectory = "AutoReproPlaceholder"

                    val invalidPrefixResourceIterator =
                        resourcesSourceSet.asFileTree
                            .matching { patternFilterable ->
                                patternFilterable.exclude(expectedResourceDirectory)
                            }
                            .iterator()
                    if (invalidPrefixResourceIterator.hasNext()) {
                        val invalidPrefixResources =
                            invalidPrefixResourceIterator.asSequence().toList()
                        throw GradleException(
                            "All resource files need to be contained within " +
                                "$expectedResourceDirectory/ in the resources directory: " +
                                "$invalidPrefixResources"
                        )
                    }
                }
            }

        // Perform verification when classes are being compiled.
        project.tasks.named("classes").configure { task ->
            task.dependsOn(verifyPackageNamesTask, verifyResourcesTask)
        }
    }
}
