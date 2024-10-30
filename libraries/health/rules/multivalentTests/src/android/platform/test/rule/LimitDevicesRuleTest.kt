/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.platform.test.rule.DeviceProduct.CF_PHONE
import android.platform.test.rule.DeviceProduct.CF_TABLET
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.AssumptionViolatedException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.model.Statement

@RunWith(JUnit4::class)
class LimitDevicesRuleTest {
    @Test
    fun allowOnPhone_withPhone_succeeds() {
        val rule = LimitDevicesRule(thisDevice = CF_PHONE.product)

        val description = createDescription(AllowedOnPhonesOnly::class.java)

        assertEquals(rule.apply(statement, description), statement)
    }

    @Test(expected = AssumptionViolatedException::class)
    fun allowOnPhone_withTablet_fails() {
        val rule = LimitDevicesRule(thisDevice = CF_TABLET.product)

        val description = createDescription(AllowedOnPhonesOnly::class.java)

        rule.apply(statement, description).evaluate()
    }

    @Test
    fun denyOnPhone_withTablet_succeeds() {
        val rule = LimitDevicesRule(thisDevice = CF_TABLET.product)

        val description = createDescription(DeniedOnPhonesOnly::class.java)

        assertEquals(rule.apply(statement, description), statement)
    }

    @Test(expected = AssumptionViolatedException::class)
    fun denyOnPhone_withPhone_fails() {
        val rule = LimitDevicesRule(thisDevice = CF_PHONE.product)

        val description = createDescription(DeniedOnPhonesOnly::class.java)

        rule.apply(statement, description).evaluate()
    }

    @Test
    fun allowedOnBothPhonesAndTablets_withTabletAndPhone_succeeds() {
        val ruleOnTablet = LimitDevicesRule(thisDevice = CF_TABLET.product)
        val ruleOnPhone = LimitDevicesRule(thisDevice = CF_PHONE.product)

        val description = createDescription(AllowedOnPhonesAndTablets::class.java)

        assertEquals(ruleOnPhone.apply(statement, description), statement)
        assertEquals(ruleOnTablet.apply(statement, description), statement)
    }

    private fun <T> createDescription(clazz: Class<T>) =
        Description.createSuiteDescription(this.javaClass, *clazz.annotations)!!

    private val statement: Statement =
        object : Statement() {
            override fun evaluate() {}
        }

    @Test
    fun flakyDevices() {
        fun checkResults(vararg lines: String) =
            assertThat(StringingListener.run(FlakyOnPhone::class)).containsExactly(*lines)

        FlakyOnPhone.device = CF_PHONE
        FlakyOnPhone.isFlakyConfig = false
        checkResults(
            "runAndPass: SKIPPED: Skipping test as cf_x86_64_phone is flaky " +
                "and this config excludes flakes"
        )

        FlakyOnPhone.device = CF_PHONE
        FlakyOnPhone.isFlakyConfig = true
        checkResults("runAndPass: PASSED")

        FlakyOnPhone.device = CF_TABLET
        FlakyOnPhone.isFlakyConfig = false
        checkResults("runAndPass: PASSED")

        FlakyOnPhone.device = CF_TABLET
        FlakyOnPhone.isFlakyConfig = true
        checkResults("runAndPass: PASSED")
    }

    @FlakyOnTablet
    class ThisIsFlakyOnTablet {
        fun aTest() {}
    }

    @Test
    fun metaAnnotation() {
        val skipReason =
            LimitDevicesRule(thisDevice = CF_TABLET.product, runningFlakyTests = false)
                .skipReasonIfAny("aTest".description<ThisIsFlakyOnTablet>())
        val expected = "Skipping test as cf_x86_64_tablet is flaky and this config excludes flakes"
        assertThat(skipReason).isEqualTo(expected)
    }
}

@MetaAnnotation
@Retention(AnnotationRetention.RUNTIME)
@FlakyDevices(CF_TABLET)
annotation class FlakyOnTablet

@AllowedDevices(CF_PHONE) private class AllowedOnPhonesOnly

@DeniedDevices(CF_PHONE) private class DeniedOnPhonesOnly

@AllowedDevices(CF_PHONE, CF_TABLET) private class AllowedOnPhonesAndTablets

@FlakyDevices(CF_PHONE)
class FlakyOnPhone {
    @JvmField
    @Rule
    val limitDevicesRule =
        LimitDevicesRule(thisDevice = device.product, runningFlakyTests = isFlakyConfig)

    @Test fun runAndPass() {}

    companion object {
        var device = CF_PHONE
        var isFlakyConfig = false
    }
}
