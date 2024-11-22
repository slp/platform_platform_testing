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

import org.gradle.api.Project
import org.gradle.api.attributes.Attribute

class PluginBase {
    class AbiAttributes(name: String, bitness: Int) {
        val name = name
        val bitness = bitness
    }

    // https://developer.android.com/ndk/guides/abis#sa
    enum class Abi(val attr: AbiAttributes) {
        ABI_ARMEABI_V7A(AbiAttributes("armeabi-v7a", 32)),
        ABI_ARM64_V8A(AbiAttributes("arm64-v8a", 64)),
        ABI_X86(AbiAttributes("x86", 32)),
        ABI_X86_64(AbiAttributes("x86_64", 64)),
    }

    companion object {
        val AUTOREPRO_TESTCASES_RESOURCE_CONFIGURATION_NAME = "testResource"

        fun applyConfiguration(
            project: Project,
            sourceDirectoryArtifact: Any,
            manifestArtifact: Any,
            resourceArtifacts: List<Pair<Abi, Any>>,
        ) {
            // Export a configuration for users to add dependencies.
            // "testResource" represents Soong modules for the Tradefed testcases/ directory.
            // See "android_test_helper_app" and "cc_test" Soong modules.
            // Dependencies should map 1:1 to tradefed Android.bp files for easy reconstruction.
            project.configurations.create(AUTOREPRO_TESTCASES_RESOURCE_CONFIGURATION_NAME) {
                configuration ->
                configuration.isCanBeConsumed = true
                configuration.isCanBeResolved = false
            }

            val testResourceConfiguration =
                project.configurations.getByName(AUTOREPRO_TESTCASES_RESOURCE_CONFIGURATION_NAME)
            val abiAttribute = Attribute.of("com.android.security.autorepro.abi", Abi::class.java)
            testResourceConfiguration.outgoing { configurationPublications ->
                configurationPublications.artifact(sourceDirectoryArtifact) {
                    configurePublishArtifact ->
                    configurePublishArtifact.setType("source")
                }
                configurationPublications.artifact(manifestArtifact) { configurePublishArtifact ->
                    configurePublishArtifact.setType("manifest")
                }
                configurationPublications.variants { namedDomainObjectContainer ->
                    resourceArtifacts.forEach { resourceArtifact ->
                        val (abi, artifact) = resourceArtifact
                        val abiName = abi.attr.name
                        namedDomainObjectContainer.create("abi-$abiName") { configurationVariant ->
                            configurationVariant.attributes { attributeContainer ->
                                attributeContainer.attribute(abiAttribute, abi)
                            }
                            configurationVariant.artifact(artifact) { configurablePublishArtifact ->
                                configurablePublishArtifact.setType("resource")
                            }
                        }
                    }
                }
            }
        }
    }

    class ModuleManifest<T>(project: Project, type: String, resource: T) {
        val name: String = project.name
        val type: String = type
        val resource: T = resource
    }
}
