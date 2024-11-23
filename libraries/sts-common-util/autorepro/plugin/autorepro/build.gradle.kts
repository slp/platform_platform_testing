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

plugins {
    // Apply the Java Gradle plugin development plugin to add support for developing Gradle plugins
    `java-gradle-plugin`

    // Apply the Kotlin JVM plugin to add support for Kotlin.
    alias(libs.plugins.jvm)

    `maven-publish`

    alias(libs.plugins.androidApp) apply false
    alias(libs.plugins.androidLib) apply false
}

dependencies {
    implementation("com.android.tools.build:gradle:8.5.2")

    // Use the Kotlin JUnit 5 integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // https://mvnrepository.com/artifact/com.google.code.gson/gson
    implementation("com.google.code.gson:gson:2.11.0")
}

java {
    toolchain {
        // The Android Gradle Plugin is currently only compatible with Java 17
        languageVersion = JavaLanguageVersion.of(17)
    }
}

// Define the overall plugin
group = "com.android.security.autorepro"

version = "1.0.0-alpha1"

// Define the individual sub-plugins
gradlePlugin {
    plugins {
        create("submissionPlugin") {
            id = "com.android.security.autorepro.submission"
            implementationClass = "com.android.security.autorepro.SubmissionPlugin"
        }
        create("javaHostTestPlugin") {
            id = "com.android.security.autorepro.javahosttest"
            implementationClass = "com.android.security.autorepro.JavaHostTestPlugin"
        }
        create("appTestPlugin") {
            id = "com.android.security.autorepro.apptest"
            implementationClass = "com.android.security.autorepro.AppTestPlugin"
        }
        create("ndkTestPlugin") {
            id = "com.android.security.autorepro.ndktest"
            implementationClass = "com.android.security.autorepro.NdkTestPlugin"
        }
    }
}

publishing.publications.withType<MavenPublication>().configureEach {
    pom {
        licenses {
            license {
                name = "The Apache Software License, Version 2.0"
                url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "repo"
            }
        }
        developers {
            developer {
                name = "The Android Open Source Project"
            }
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("pluginMaven") {
            pom {
                name = "AutoRepro"
                description = "Gradle plugin to develop Android VRP reports as Tradefed tests."
            }
        }
    }
    repositories {
        maven {
            url = uri(layout.buildDirectory.dir("maven-repo"))
        }
    }
}
