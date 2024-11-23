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
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.file.Files
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.RelativePath
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip

open class SubmissionExtension {
    var deviceType: String? = null // phone, tv, wear, etc. enum?
    var isRootRequired: Boolean = false
    var isExternalHardwareRequired: Boolean = false // string?
    var isPersistentExploit: Boolean = false
}

class SubmissionPlugin : Plugin<Project> {
    val taskGroup = "AutoRepro"
    val submissionSourcesDir = "autorepro-submission"
    val stsTradefedDir = "android-sts"

    // get plugin jar path from current class; plugin jar has resources
    val pluginJarUrl = this.javaClass.getProtectionDomain().getCodeSource().getLocation()

    val abiAttribute =
        Attribute.of("com.android.security.autorepro.abi", PluginBase.Abi::class.java)
    var testResourceConfiguration: Configuration? = null

    fun dependencyProjectConfiguration(
        project: Project,
        configuration: Configuration,
        action: Action<Pair<Project, Configuration>>,
    ) {
        // By default, Gradle considers "evaluation" of the "configure" lifecycle to be done before
        // the subprojects/dependencies are "configured/evaluated".
        // This means that we can't process the subproject's configurations or artifacts unless this
        // is set.
        // Note: This likely only works for subprojects instead of all dependencies. I don't see
        // another way because dependencies aren't available until afterEvaluate.
        project.evaluationDependsOnChildren()

        // Need to perform this work in the "afterEvaluate" callback because the extensions,
        // dependencies, and dependency artifacts are not initialized yet.
        project.afterEvaluate {
            configuration.dependencies.forEach { dependency ->
                if (dependency is ProjectDependency) {
                    val dependencyProject = dependency.dependencyProject
                    val dependencyConfiguration =
                        dependencyProject.configurations.getByName(configuration.name)
                    action.execute(Pair(dependencyProject, dependencyConfiguration))
                }
            }
        }
    }

    fun registerAssembleAutoReproTradefedTask(
        project: Project,
        target: String,
        abis: Set<PluginBase.Abi>,
    ) {
        val copyAutoReproTradefedToolsTask =
            project.tasks.register<Copy>("copyAutoReproTradefedTools-$target", Copy::class.java) {
                task ->
                // use ziptree to copy the zip contents instead of the jar itself
                task.from(project.zipTree(pluginJarUrl)) { copySpec ->
                    // only include the "sts-tradefed-tools" path instead of all jar contents
                    copySpec.include("sts-tradefed-tools/**")
                    copySpec.eachFile { fileCopyDetails ->
                        // drop the "sts-tradefed-tools" path prefix
                        fileCopyDetails.relativePath =
                            RelativePath(
                                true /* endsWithFile */,
                                *fileCopyDetails.relativePath.segments.drop(1).toTypedArray(),
                            )
                    }
                    // don't include the "sts-tradefed-tools" dir itself
                    copySpec.includeEmptyDirs = false
                }
                task.into(project.layout.buildDirectory.dir("$target/$stsTradefedDir/tools"))
            }

        val copyTradefedJdkTask =
            project.tasks.register<Copy>("copyTradefedJdk-$target", Copy::class.java) { task ->
                task.from(project.zipTree(pluginJarUrl)) { copySpec -> copySpec.include("jdk/**") }
                task.into(project.layout.buildDirectory.dir("$target/$stsTradefedDir"))
            }

        val assembleAutoReproTradefedTask =
            project.tasks.register("assembleAutoReproTradefed-$target") { task ->
                task.description =
                    "Assemble the AutoRepro Tradefed executable test suite for $target."
                task.group = taskGroup
                task.dependsOn(copyAutoReproTradefedToolsTask, copyTradefedJdkTask)
            }

        abis.forEach { abi ->
            dependencyProjectConfiguration(project, testResourceConfiguration!!) {
                dependencyProjectConfigurationPair ->
                val (dependencyProject, dependencyConfiguration) =
                    dependencyProjectConfigurationPair
                val projectName = dependencyProject.name
                dependencyConfiguration.outgoing { configurationPublications ->
                    configurationPublications.variants { namedDomainObjectContainer ->
                        namedDomainObjectContainer
                            .filter { configurationVariant ->
                                configurationVariant.attributes.getAttribute(abiAttribute) == abi
                            }
                            .forEach { configurationVariant ->
                                val abiName = abi.attr.name
                                configurationVariant.artifacts
                                    .filter { publishArtifact ->
                                        publishArtifact.type == "resource"
                                    }
                                    .forEach { publishArtifact ->
                                        val copyTestcaseResourceTaskName =
                                            "copyTestcaseResource-$target-$projectName-$abiName"
                                        val copyTestcaseResourceTask =
                                            project.tasks.register<Copy>(
                                                copyTestcaseResourceTaskName,
                                                Copy::class.java,
                                            ) { task ->
                                                // Don't copy until entire task done
                                                task.dependsOn(publishArtifact)
                                                // TODO:
                                                // https://github.com/gradle/gradle/issues/25587 -
                                                // use publishArtifact directly
                                                task.from(publishArtifact.file)
                                                task.into(
                                                    project.layout.buildDirectory.dir(
                                                        "$target/$stsTradefedDir/testcases"
                                                    )
                                                )
                                            }
                                        assembleAutoReproTradefedTask.configure { task ->
                                            task.dependsOn(copyTestcaseResourceTask)
                                        }
                                    }
                            }
                    }
                }
            }
        }

        // Build AutoRepro Tradefed tasks on assemble
        project.tasks.named("assemble").configure { task ->
            task.dependsOn(assembleAutoReproTradefedTask)
        }
    }

    override fun apply(project: Project) {
        val submissionExtension =
            project.extensions.create("submission", SubmissionExtension::class.java)

        testResourceConfiguration =
            project.configurations.create("testResource") { configuration ->
                configuration.isCanBeConsumed = false
                configuration.isCanBeResolved = true
            }

        // The standard lifecycle tasks including `assemble` are defined in `base`
        // By applying the plugin, we essentially export it
        // Without it, users will hit an error that "assemble" isn't found
        project.plugins.apply("base")

        val mergeManifestsTask =
            project.tasks.register("mergeManifests") { task ->
                task.outputs.file(
                    project.layout.buildDirectory.dir("$submissionSourcesDir/manifest.json")
                )
                task.doLast {
                    val submissionJson = JsonObject()
                    val resources = JsonArray()
                    task.inputs.files.forEach {
                        val resourcesJsonString = Files.readString(it.toPath())
                        resources.add(JsonParser.parseString(resourcesJsonString))
                    }
                    submissionJson.add("resources", resources)
                    submissionJson.add("submission", Gson().toJsonTree(submissionExtension))
                    val gson = GsonBuilder().setPrettyPrinting().create()
                    val jsonOutput = gson.toJson(submissionJson)
                    task.outputs.files.singleFile.writeText(jsonOutput)
                }
            }

        val copyRootProjectTask =
            project.tasks.register<Copy>("copyRootProject", Copy::class.java) { task ->
                val rootProjectDir = project.rootProject.layout.projectDirectory
                task.from(rootProjectDir) { copySpec ->
                    // Ignore system-specific files
                    copySpec.exclude("local.properties")

                    // Ignore gradle build
                    project.rootProject.subprojects.forEach { subproject ->
                        // exclude must be a String pattern relative to the copySpec "from"
                        val subprojectBuildDirFile = subproject.layout.buildDirectory.asFile.get()
                        copySpec.exclude(
                            subprojectBuildDirFile.relativeTo(rootProjectDir.asFile).toString()
                        )
                    }
                    // Ignore Gradle work dir and executable files
                    copySpec.exclude(".gradle")
                    copySpec.exclude("gradle")
                    copySpec.exclude("gradlew")
                    copySpec.exclude("gradlew.bat")

                    // Ignore git
                    copySpec.exclude(".git*")

                    // Ignore Idea workspace files
                    copySpec.exclude(".idea")

                    // Ignore native build directories
                    copySpec.exclude("**/.cxx")
                }
                task.into(project.layout.buildDirectory.dir("$submissionSourcesDir/project"))
            }

        val assembleSubmissionSourcesTask =
            project.tasks.register("assembleSubmissionSources") { task ->
                task.description = "Assemble the source files for the submission zip."
                task.group = taskGroup
                task.dependsOn(mergeManifestsTask)
                task.dependsOn(copyRootProjectTask)
                task.outputs.file(project.layout.buildDirectory.dir(submissionSourcesDir))
            }

        val assembleSubmissionZipTask =
            project.tasks.register<Zip>("assembleSubmissionZip", Zip::class.java) { task ->
                task.description = "Assemble the submission zip for upload."
                task.group = taskGroup
                task.from(assembleSubmissionSourcesTask)
                task.archiveFileName.set("autorepro-submission.zip")
                task.destinationDirectory.set(project.layout.buildDirectory)
            }

        // matching Android Build configs
        val targetToMultiAbiSets =
            mapOf(
                "test_suites_arm64" to
                    setOf(PluginBase.Abi.ABI_ARMEABI_V7A, PluginBase.Abi.ABI_ARM64_V8A),
                "test_suites_x86_64" to setOf(PluginBase.Abi.ABI_X86, PluginBase.Abi.ABI_X86_64),
            )
        targetToMultiAbiSets.entries.forEach { mapEntry ->
            val (target, abis) = mapEntry
            registerAssembleAutoReproTradefedTask(project, target, abis)
        }

        // By default, Gradle considers "evaluation" of the "configure" lifecycle to be done before
        // the subprojects/dependencies are "configured/evaluated".
        // This means that we can't process the subproject's configurations or artifacts unless this
        // is set.
        // Note: This likely only works for subprojects instead of all dependencies. I don't see
        // another way because dependencies aren't available until afterEvaluate.
        project.evaluationDependsOnChildren()

        // Need to perform this work in the "afterEvaluate" callback because the extensions,
        // dependencies, and dependency artifacts are not initialized yet.
        project.afterEvaluate {
            testResourceConfiguration!!.dependencies.forEach { dependency ->
                if (dependency is ProjectDependency) {
                    val dependencyProject = dependency.dependencyProject
                    val configuration =
                        dependencyProject.configurations.getByName(testResourceConfiguration!!.name)
                    val projectName = dependencyProject.name

                    configuration.artifacts.forEach { publishArtifact ->
                        when (publishArtifact.type) {
                            "manifest" -> {
                                mergeManifestsTask.configure { task ->
                                    task.dependsOn(dependencyProject.tasks.getByName("assemble"))
                                    task.inputs.file(publishArtifact.file)
                                }
                            }
                            "source" -> {
                                val copyAutoReproTestcaseResourceSourceTask =
                                    project.tasks.register<Copy>(
                                        "copyAutoReproTestcaseResourceSource-$projectName",
                                        Copy::class.java,
                                    ) { task ->
                                        // TODO: https://github.com/gradle/gradle/issues/25587 - use
                                        // publishArtifact directly
                                        task.from(publishArtifact.file)
                                        task.into(
                                            project.layout.buildDirectory.dir(
                                                "$submissionSourcesDir/source/$projectName/"
                                            )
                                        )
                                    }
                                assembleSubmissionSourcesTask.configure { task ->
                                    task.dependsOn(copyAutoReproTestcaseResourceSourceTask)
                                }
                            }
                            else -> {
                                throw IllegalStateException(
                                    "A resource added to the ${configuration.name} configuration " +
                                        "has an unknown type. name: ${publishArtifact.name}, " +
                                        "type: ${publishArtifact.type}"
                                )
                            }
                        }
                    }
                }
            }
        }

        val copyInvocationResultsResultsToSubmissionTask =
            project.tasks.register("copyInvocationResultsToSubmission") { task ->
                task.description =
                    "Copy the results from previous Tradefed invocations into the AutoRepro " +
                        "submission sources directory to assist with the review process. Note " +
                        "that this contains logs from both the host and device; please review " +
                        "the contents before or after running this."
                task.group = taskGroup
            }
        targetToMultiAbiSets.entries.forEach { mapEntry ->
            val (target, _) = mapEntry
            val targetTask =
                project.tasks.register<Copy>(
                    "copyInvocationResultsToSubmission-$target",
                    Copy::class.java,
                ) { task ->
                    task.dependsOn(assembleSubmissionZipTask)
                    task.from(project.layout.buildDirectory.dir("$target/$stsTradefedDir")) {
                        copySpec ->
                        copySpec.include("logs/**")
                        copySpec.include("results/**")
                    }
                    task.into(project.layout.buildDirectory.dir(submissionSourcesDir))
                }
            copyInvocationResultsResultsToSubmissionTask.configure { task ->
                task.dependsOn(targetTask)
            }
        }
    }
}
