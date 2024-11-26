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

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.lang.annotation.Inherited
import kotlin.reflect.full.memberFunctions
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.JUnitCore
import org.junit.runner.Request
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Inherited @Retention(AnnotationRetention.RUNTIME) annotation class Flavor(val value: String = "")

@Retention(AnnotationRetention.RUNTIME) annotation class NotInherited(val value: String = "")

@Inherited
@Retention(AnnotationRetention.RUNTIME)
@MetaAnnotation
@Flavor("umami")
annotation class Umami()

inline fun <reified T> String.description() =
    Description.createTestDescription(
        T::class.java,
        this,
        *(T::class.memberFunctions.single { it.name == this }.annotations.toTypedArray()),
    )

class TestAnnotationScannerTest {
    class NoAnnotation {
        fun aTest() {}
    }

    @Test
    fun noAnnotation() {
        val scanner = TestAnnotationScanner()
        val method = "aTest".description<NoAnnotation>()
        assertThat(scanner.find<Flavor>(method)).isNull()
    }

    class AnnotationOnMethod {
        @Flavor("spicy") fun aTest() {}
    }

    @Test
    fun annotationOnMethod() {
        val scanner = TestAnnotationScanner()
        val method = "aTest".description<AnnotationOnMethod>()
        assertThat(scanner.find<Flavor>(method)?.value).isEqualTo("spicy")
    }

    @Flavor("sour")
    class AnnotationOnClass {
        fun aTest() {}
    }

    @Test
    fun annotationOnClass() {
        val scanner = TestAnnotationScanner()
        val method = "aTest".description<AnnotationOnClass>()
        assertThat(scanner.find<Flavor>(method)?.value).isEqualTo("sour")
    }

    @Flavor("bitter")
    class AnnotationOnBoth {
        @Flavor("sweet") fun aTest() {}
    }

    @Test
    fun methodWinsOnBoth() {
        val scanner = TestAnnotationScanner()
        val method = "aTest".description<AnnotationOnBoth>()
        assertThat(scanner.find<Flavor>(method)?.value).isEqualTo("sweet")
    }

    @NotInherited("only for this class")
    @Flavor("tangy")
    @AllowedDevices(DeviceProduct.CF_PHONE)
    open class Superclass

    class Subclass : Superclass() {
        fun aTest() {}
    }

    @Test
    fun findSuperclassIfInherited() {
        val scanner = TestAnnotationScanner()
        val method = "aTest".description<Subclass>()
        assertThat(scanner.find<Flavor>(method)?.value).isEqualTo("tangy")
    }

    @Test
    fun findAllowedDevicesFromSuperclass() {
        val scanner = TestAnnotationScanner()
        val method = "aTest".description<Subclass>()
        assertThat(scanner.find<AllowedDevices>(method)?.allowed)
            .isEqualTo(arrayOf(DeviceProduct.CF_PHONE))
    }

    @Test
    fun dontFindSuperclassIfNotInherited() {
        val scanner = TestAnnotationScanner()
        val method = "aTest".description<Subclass>()
        assertThat(scanner.find<NotInherited>(method)).isNull()
    }

    @Umami
    open class UmamiTest {
        fun aTest() {}
    }

    @Test
    fun metaAnnotation() {
        val scanner = TestAnnotationScanner()
        val method = "aTest".description<UmamiTest>()
        assertThat(scanner.find<Flavor>(method)!!.value).isEqualTo("umami")
    }

    @RunWith(Parameterized::class)
    class ParameterizedUmamiSubclassTest : UmamiTest() {
        companion object {
            @Parameterized.Parameters(name = "{0}")
            @JvmStatic
            fun getParams() = listOf("1", "2", "3")
        }

        @Parameterized.Parameter lateinit var param: String

        @Test fun bTest() {}
    }

    @Test
    fun parameterizedSubclass() {
        val scanner = TestAnnotationScanner()
        val request = Request.classes(ParameterizedUmamiSubclassTest::class.java)
        assertThat(JUnitCore().run(request).failures).isEmpty()
        val rootDescription = request.runner.description
        val leaves = rootDescription.leafDescriptions()
        assertThat(leaves.map { it.methodName }).contains("bTest[1]")
        val bTestDescription = leaves.first { it.methodName == "bTest[1]" }
        assertWithMessage(rootDescription.familyTree())
            .that(scanner.find<Flavor>(bTestDescription)?.value)
            .isEqualTo("umami")
    }
}

private fun Description.familyTree(): String {
    return toString() +
        if (isTest) {
            ""
        } else {
            children.map { it.familyTree() }.toString()
        }
}

private fun Description.leafDescriptions(): List<Description> {
    return if (isTest) {
        listOf(this)
    } else {
        children.flatMap { it.leafDescriptions() }
    }
}
