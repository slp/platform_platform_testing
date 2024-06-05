/*
 * Copyright 2022 The Android Open Source Project
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

package platform.test.screenshot

import android.content.Context
import android.util.Log
import android.util.SparseIntArray
import android.widget.RemoteViews
import androidx.annotation.VisibleForTesting
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A rule to overload the target context system colors by [colors].
 *
 * This is especially useful to apply the colors before you start an activity using an
 * [ActivityScenarioRule] or any other rule, given that the colors must be [applied]
 * [MaterialYouColors.apply] *before* doing any resource resolution.
 */
class MaterialYouColorsRule(private val colors: MaterialYouColors = MaterialYouColors.GreenBlue) :
    TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                colors.apply(InstrumentationRegistry.getInstrumentation().targetContext)
                base.evaluate()
            }
        }
    }
}

/**
 * A util class to overload the Material You colors of a [Context] to some fixed values. This can be
 * used by screenshot tests so that device-specific colors don't impact the outcome of the test.
 *
 * @see apply
 */
class MaterialYouColors(
    @get:VisibleForTesting val colors: SparseIntArray,
) {
    /**
     * Apply these colors to [context].
     *
     * Important: No resource resolution must have be done on the context given to that method.
     */
    fun apply(context: Context) {
        RemoteViews.ColorResources.create(context, colors)?.apply(context)
    }

    companion object {
        private const val FIRST_RESOURCE_COLOR_ID = android.R.color.system_neutral1_0
        private const val LAST_RESOURCE_COLOR_ID = android.R.color.system_error_1000

        /**
         * An instance of [MaterialYouColors] with green/blue colors seed, that can be used directly
         * by tests.
         */
        val GreenBlue = fromColors(GREEN_BLUE)

        /**
         * Create a [MaterialYouColors] from [colors], where:
         * - `colors[i]` should be the value of `FIRST_RESOURCE_COLOR_ID + i`.
         * - [colors] are recommended to contain all values of all system colors, i.e. `colors.size`
         *   is best to be `LAST_RESOURCE_COLOR_ID - FIRST_RESOURCE_COLOR_ID + 1`, otherwise we are
         *   expected to observed unused values in the sparse array (more elements in the array) or
         *   unconfigured system colors (fewer elements in the array).
         */
        private fun fromColors(colors: IntArray): MaterialYouColors {
            val expectedSize = LAST_RESOURCE_COLOR_ID - FIRST_RESOURCE_COLOR_ID + 1
            if (colors.size != expectedSize) {
                Log.d(
                    "MaterialYouColors.fromColors",
                    "colors are best to have exactly $expectedSize elements for a complete " +
                    "configuration for system colors")
            }

            val sparseArray = SparseIntArray(/* initialCapacity= */ expectedSize)
            colors.forEachIndexed { i, color ->
                sparseArray.put(FIRST_RESOURCE_COLOR_ID + i, color)
            }

            return MaterialYouColors(sparseArray)
        }
    }
}

/**
 * Some green/blue colors, from system_neutral1_0 to system_accent3_1000, extracted using 0xB1EBFF
 * as seed color and "FRUIT_SALAD" as theme style.
 */
private val GREEN_BLUE =
    intArrayOf(
        -1,
        -393729,
        -1641480,
        -2562838,
        -4405043,
        -6181454,
        -7892073,
        -9668483,
        -11181979,
        -12760755,
        -14208458,
        -15590111,
        -16777216,
        -1,
        -393729,
        -2296322,
        -3217680,
        -4994349,
        -6770760,
        -8547171,
        -10257790,
        -11836822,
        -13350318,
        -14863301,
        -16376283,
        -16777216,
        -1,
        -720905,
        -4456478,
        -8128307,
        -10036302,
        -12075112,
        -14638210,
        -16742810,
        -16749487,
        -16756420,
        -16762839,
        -16768746,
        -16777216,
        -1,
        -720905,
        -4456478,
        -5901613,
        -7678281,
        -9454947,
        -11231613,
        -13139095,
        -15111342,
        -16756420,
        -16762839,
        -16768746,
        -16777216,
        -1,
        -393729,
        -2361857,
        -5051393,
        -7941655,
        -9783603,
        -11625551,
        -13729642,
        -16750723,
        -16757153,
        -16763326,
        -16769241,
        -16777216,
  /* "system_primary_container_light" */ -2432257,
  /* "system_on_primary_container_light" */ -16770999,
  /* "system_primary_light" */ -11969134,
  /* "system_on_primary_light" */ -1,
  /* "system_secondary_container_light" */ -2235655,
  /* "system_on_secondary_container_light" */ -15394004,
  /* "system_secondary_light" */ -10985871,
  /* "system_on_secondary_light" */ -1,
  /* "system_tertiary_container_light" */ -76039,
  /* "system_on_tertiary_container_light" */ -13954517,
  /* "system_tertiary_light" */ -9218959,
  /* "system_on_tertiary_light" */ -1,
  /* "system_background_light" */ -329473,
  /* "system_on_background_light" */ -15066335,
  /* "system_surface_light" */ -329473,
  /* "system_on_surface_light" */ -15066335,
  /* "system_surface_container_low_light" */ -723974,
  /* "system_surface_container_lowest_light" */ -1,
  /* "system_surface_container_light" */ -1118732,
  /* "system_surface_container_high_light" */ -1513489,
  /* "system_surface_container_highest_light" */ -1842455,
  /* "system_surface_bright_light" */ -329473,
  /* "system_surface_dim_light" */ -2434592,
  /* "system_surface_variant_light" */ -1973524,
  /* "system_on_surface_variant_light" */ -12237233,
  /* "system_outline_light" */ -9078912,
  /* "system_error_light" */ -4580838,
  /* "system_on_error_light" */ -1,
  /* "system_error_container_light" */ -9514,
  /* "system_on_error_container_light" */ -12517374,
  /* "system_control_activated_light" */ -2432257,
  /* "system_control_normal_light" */ -12237233,
  /* "system_control_highlight_light" */ 520093696,
  /* "system_text_primary_inverse_light" */ -1842455,
  /* "system_text_secondary_and_tertiary_inverse_light" */ -3815728,
  /* "system_text_primary_inverse_disable_only_light" */ -1842455,
  /* "system_text_secondary_and_tertiary_inverse_disabled_light" */ -1842455,
  /* "system_text_hint_inverse_light" */ -1842455,
  /* "system_palette_key_color_primary_light" */ -10324564,
  /* "system_palette_key_color_secondary_light" */ -9341301,
  /* "system_palette_key_color_tertiary_light" */ -7443061,
  /* "system_palette_key_color_neutral_light" */ -9013379,
  /* "system_palette_key_color_neutral_variant_light" */ -9013376,
  /* "system_primary_container_dark" */ -13548168,
  /* "system_on_primary_container_dark" */ -2432257,
  /* "system_primary_dark" */ -5061121,
  /* "system_on_primary_dark" */ -15192480,
  /* "system_secondary_container_dark" */ -12499367,
  /* "system_on_secondary_container_dark" */ -2235655,
  /* "system_secondary_dark" */ -4143395,
  /* "system_on_secondary_dark" */ -14012350,
  /* "system_tertiary_container_dark" */ -10863271,
  /* "system_on_tertiary_container_dark" */ -76039,
  /* "system_tertiary_dark" */ -1983524,
  /* "system_on_tertiary_dark" */ -12441791,
  /* "system_background_dark" */ -15592680,
  /* "system_on_background_dark" */ -1842455,
  /* "system_surface_dark" */ -15592680,
  /* "system_on_surface_dark" */ -1842455,
  /* "system_surface_container_low_dark" */ -15066335,
  /* "system_surface_container_lowest_dark" */ -15921645,
  /* "system_surface_container_dark" */ -14803163,
  /* "system_surface_container_high_dark" */ -14079441,
  /* "system_surface_container_highest_dark" */ -13421510,
  /* "system_surface_bright_dark" */ -13092545,
  /* "system_surface_dim_dark" */ -15592680,
  /* "system_surface_variant_dark" */ -12237233,
  /* "system_on_surface_variant_dark" */ -3815728,
  /* "system_outline_dark" */ -7368550,
  /* "system_error_dark" */ -19285,
  /* "system_on_error_dark" */ -9895931,
  /* "system_error_container_dark" */ -7143414,
  /* "system_on_error_container_dark" */ -9514,
  /* "system_control_activated_dark" */ -13548168,
  /* "system_control_normal_dark" */ -3815728,
  /* "system_control_highlight_dark" */ 872415231,
  /* "system_text_primary_inverse_dark" */ -15066335,
  /* "system_text_secondary_and_tertiary_inverse_dark" */ -12237233,
  /* "system_text_primary_inverse_disable_only_dark" */ -15066335,
  /* "system_text_secondary_and_tertiary_inverse_disabled_dark" */ -15066335,
  /* "system_text_hint_inverse_dark" */ -15066335,
  /* "system_palette_key_color_primary_dark" */ -10324564,
  /* "system_palette_key_color_secondary_dark" */ -9341301,
  /* "system_palette_key_color_tertiary_dark" */ -7443061,
  /* "system_palette_key_color_neutral_dark" */ -9013379,
  /* "system_palette_key_color_neutral_variant_dark" */ -9013376,
  /* "system_primary_fixed" */ -2432257,
  /* "system_primary_fixed_dim" */ -5061121,
  /* "system_on_primary_fixed" */ -16770999,
  /* "system_on_primary_fixed_variant" */ -13548168,
  /* "system_secondary_fixed" */ -2235655,
  /* "system_secondary_fixed_dim" */ -4143395,
  /* "system_on_secondary_fixed" */ -15394004,
  /* "system_on_secondary_fixed_variant" */ -12499367,
  /* "system_tertiary_fixed" */ -76039,
  /* "system_tertiary_fixed_dim" */ -1983524,
  /* "system_on_tertiary_fixed" */ -13954517,
  /* "system_on_tertiary_fixed_variant" */ -10863271,
  /* "system_outline_variant_light" */ -3815728,
  /* "system_outline_variant_dark" */ -12237233,
  /* "system_surface_disabled" */ 1123743999,
  /* "system_on_surface_disabled" */ 1109007137,
  /* "system_outline_disabled" */ 1114994560,
  /* "system_error_0" */ -1,
  /* "system_error_10" */ -1031,
  /* "system_error_50" */ -200978,
  /* "system_error_100" */ -401700,
  /* "system_error_200" */ -870219,
  /* "system_error_300" */ -1273202,
  /* "system_error_400" */ -1808030,
  /* "system_error_500" */ -2345426,
  /* "system_error_600" */ -5036514,
  /* "system_error_700" */ -7594728,
  /* "system_error_800" */ -10480624,
  /* "system_error_900" */ -12513781,
  /* "system_error_1000" */ -16777216,
    )
