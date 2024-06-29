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

import android.app.Instrumentation
import android.hardware.devicestate.DeviceState.PROPERTY_FOLDABLE_DISPLAY_CONFIGURATION_OUTER_PRIMARY
import android.hardware.devicestate.DeviceStateManager
import android.hardware.devicestate.feature.flags.Flags as DeviceStateManagerFlags
import android.os.Build
import androidx.test.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.android.internal.R
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.ANNOTATION_CLASS
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import org.junit.AssumptionViolatedException
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

/**
 * Rule that allow some tests to be executed only on [FoldableOnly], [LargeScreenOnly], [TabletOnly]
 * or [SmallScreenOnly] devices.
 */
class DeviceTypeRule : TestRule {

    private val isFoldable = isFoldable()
    private val isLargeScreen = isLargeScreen()
    private val isTablet = isTablet()

    // We don't have a clear rule for whether these annotations should be inherited or not, and
    // it's less confusing if we require them to be explicit
    private inline fun <reified T : Annotation> Description.getAnnotationClearly(): T? {
        getAnnotation(T::class.java)?.let {
            return it
        }
        var superClass = testClass.superclass
        while (superClass != null) {
            if (superClass.getAnnotation(T::class.java) != null) {
                val msg =
                    "DeviceTypeRule requires subclass to have the same device-check " +
                        "annotations as superclasses: ${T::class.simpleName}"
                throw RuntimeException(msg)
            }
            superClass = superClass.superclass
        }
        return null
    }

    override fun apply(base: Statement, description: Description): Statement {
        val smallScreenAnnotation = description.getAnnotationClearly<SmallScreenOnly>()
        if (smallScreenAnnotation != null && isLargeScreen) {
            return wrongDeviceTypeStatement(
                description,
                "Skipping test on ${Build.PRODUCT} as it doesn't have a small screen. " +
                    "Reason why this should only run on small screens: " +
                    "$smallScreenAnnotation.reason."
            )
        }

        if (description.getAnnotationClearly<LargeScreenOnly>() != null && !isLargeScreen) {
            return wrongDeviceTypeStatement(
                description,
                "Skipping test on ${Build.PRODUCT} as it doesn't have a large screen."
            )
        }

        if (description.getAnnotationClearly<FoldableOnly>() != null && !isFoldable) {
            return wrongDeviceTypeStatement(
                description,
                "Skipping test on ${Build.PRODUCT} as it is not a foldable."
            )
        }

        if (description.getAnnotationClearly<FoldableOnly>() != null && isFoldable
            && isCuttlefish) {
            return wrongDeviceTypeStatement(
                description,
                "Skipping test on ${Build.PRODUCT} as E2E foldable tests are not " +
                        "supported on Cuttlefish targets. " +
                        "See go/e2e-cf-foldable-maybe-not for more details"
            )
        }

        if (description.getAnnotationClearly<TabletOnly>() != null && !isTablet) {
            return wrongDeviceTypeStatement(
                description,
                "Skipping test on ${Build.PRODUCT} as it is not a tablet."
            )
        }

        return base
    }
}

internal fun isFoldable(): Boolean {
    if (DeviceStateManagerFlags.deviceStatePropertyMigration()) {
        val dm: DeviceStateManager =
            getInstrumentation().targetContext.getSystemService(DeviceStateManager::class.java)
        return dm.supportedDeviceStates.any { state ->
            state.hasProperty(PROPERTY_FOLDABLE_DISPLAY_CONFIGURATION_OUTER_PRIMARY)
        }
    } else {
        return getInstrumentation()
            .targetContext
            .resources
            .getIntArray(R.array.config_foldedDeviceStates)
            .isNotEmpty()
    }
}

private val isCuttlefish get() = Build.BOARD == "cutf"

/** Returns whether the device default display is currently considered large screen. */
fun isLargeScreen(): Boolean {
    val sizeDp = getUiDevice().displaySizeDp
    return sizeDp.x >= LARGE_SCREEN_DP_THRESHOLD && sizeDp.y >= LARGE_SCREEN_DP_THRESHOLD
}

internal fun isTablet(): Boolean {
    return (isLargeScreen() && !isFoldable())
}

private fun wrongDeviceTypeStatement(description: Description, message: String): Statement {
    val annotation = description.getAnnotation(RunWith::class.java)
    if (
        annotation != null &&
            annotation.value.annotations.none {
                it.annotationClass == HandlesClassLevelExceptions::class
            }
    ) {
        return object : Statement() {
            override fun evaluate() {
                throw Exception(
                    "Test $description has runner ${annotation.value.simpleName} " +
                        "that is incompatible with DeviceTypeRule checks"
                )
            }
        }
    }

    return object : Statement() {
        override fun evaluate() {
            throw AssumptionViolatedException(message)
        }
    }
}

private fun getInstrumentation(): Instrumentation = InstrumentationRegistry.getInstrumentation()

private fun getUiDevice(): UiDevice = UiDevice.getInstance(getInstrumentation())

private const val LARGE_SCREEN_DP_THRESHOLD = 600

/**
 * The test will be skipped on large screens. Don't use this annotation instead of fixing a test on
 * a large-screen device. See [isLargeScreen].
 */
@Retention(RUNTIME)
@Target(ANNOTATION_CLASS, CLASS, FUNCTION)
annotation class SmallScreenOnly(val reason: String)

/** The test will run only on large screens. See [isLargeScreen]. */
@Retention(RUNTIME) @Target(ANNOTATION_CLASS, CLASS, FUNCTION) annotation class LargeScreenOnly

/** The test will run only on foldables. */
@Retention(RUNTIME) @Target(ANNOTATION_CLASS, CLASS, FUNCTION) annotation class FoldableOnly

/** The test will run only on tablets. */
@Retention(RUNTIME) @Target(ANNOTATION_CLASS, CLASS, FUNCTION) annotation class TabletOnly
