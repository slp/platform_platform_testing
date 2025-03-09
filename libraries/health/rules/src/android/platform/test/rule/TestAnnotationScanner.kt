/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.platform.test.rule

import kotlin.reflect.KClass
import kotlin.reflect.cast
import kotlin.reflect.full.hasAnnotation
import org.junit.runner.Description

/**
 * Marks an annotation class as a "meta-annotation", which for our purposes means that it bundles
 * together one or more test-behavior-affecting annotation values for reuse in multiple places in
 * the test suite, if the runner or rule in question uses [TestAnnotationScanner]. For example:
 * ```
 * @Retention(AnnotationRetention.RUNTIME)
 * annotation class Flavor(val value: String = "")
 *
 * @Retention(AnnotationRetention.RUNTIME)
 * @MetaAnnotation
 * @Flavor("umami")
 * annotation class Umami()
 *
 * // This test should be treated as if annotation with `@Flavor("umami")`
 * @Umami
 * class SushiTest {
 * }
 * ```
 */
@Retention(AnnotationRetention.RUNTIME) annotation class MetaAnnotation

/**
 * Scans for annotations on test methods and classes that may affect test runners or rules.
 *
 * (Encapsulated as an object to allow potentially caching results in the future, based on
 * experience on annotation reflection performance in JUnit 4.)
 *
 * @see [find] for primary usage
 */
class TestAnnotationScanner {
    /** inline reified version of [find] for more concise usage in Kotlin */
    inline fun <reified T : Annotation> find(description: Description) = find(T::class, description)

    /**
     * Find the most-relevant instance of [annotationClass] for the leaf-level test method described
     * by [description], or null if none exists. The rules include:
     * - if there is no such annotation on the test method, looks for an annotation on the class
     * - if [annotationClass] is marked with [java.lang.annotation.Inherited], then the superclass
     *   hierarchy will be searched for a relevant annotation
     * - if there are any annotations on the method or class, (or superclasses if [Inherited]) that
     *   are marked with [MetaAnnotation], then the annotations on the meta-annotation will be
     *   recursively searched (see [MetaAnnotation])
     */
    fun <T : Annotation> find(annotationClass: KClass<T>, description: Description): T? {
        findAnnotation(annotationClass, description.annotations)?.let {
            return it
        }
        val testClass =
            description.testClass
                ?: throw IllegalArgumentException(
                    "Could not find class for test: ${description.displayName}"
                )
        findAnnotation(annotationClass, testClass.annotations.toList())?.let {
            return it
        }
        return null
    }

    private fun <T : Annotation> findAnnotation(
        annotationClass: KClass<T>,
        annotations: Collection<Annotation>,
    ): T? {
        annotations.forEach { annotation ->
            if (annotationClass.isInstance(annotation)) {
                return annotationClass.cast(annotation)
            }
            val maybeMeta = annotation.annotationClass
            if (maybeMeta.hasAnnotation<MetaAnnotation>()) {
                findAnnotation(annotationClass, maybeMeta.annotations)?.let {
                    return it
                }
            }
        }
        return null
    }
}
