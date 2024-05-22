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
    implementation("com.android.tools.build:gradle:8.2.2")

    // Use the Kotlin JUnit 5 integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // https://mvnrepository.com/artifact/com.google.code.gson/gson
    implementation("com.google.code.gson:gson:2.10")
}

java {
    toolchain {
        // The Android Gradle Plugin is currently only compatible with Java 17
        languageVersion = JavaLanguageVersion.of(17)
    }
}

// Define the overall plugin
group = "com.android.sts.sdk"

version = "1.0.0"

// Define the individual sub-plugins
gradlePlugin {
    plugins {
        create("stsSdkBasePlugin") {
            id = "com.android.sts.sdk.base"
            implementationClass = "com.android.sts.StsSdkBasePlugin"
        }
        create("stsSdkSubmissionPlugin") {
            id = "com.android.sts.sdk.submission"
            implementationClass = "com.android.sts.StsSdkSubmissionPlugin"
        }
        create("StsSdkJavaHostTestPlugin") {
            id = "com.android.sts.sdk.javahosttest"
            implementationClass = "com.android.sts.StsSdkJavaHostTestPlugin"
        }
        create("StsSdkAppTestPlugin") {
            id = "com.android.sts.sdk.apptest"
            implementationClass = "com.android.sts.StsSdkAppTestPlugin"
        }
        create("StsSdkNdkTestPlugin") {
            id = "com.android.sts.sdk.ndktest"
            implementationClass = "com.android.sts.StsSdkNdkTestPlugin"
        }
    }
}
