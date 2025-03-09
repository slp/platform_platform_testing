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
package android.platform.systemui_tapl.ui

import android.graphics.PointF
import android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector
import android.platform.systemui_tapl.utils.SETTINGS_PACKAGE
import android.platform.systemui_tapl.utils.SYSUI_PACKAGE
import android.platform.test.scenario.tapl_common.TaplUiDevice
import android.platform.test.scenario.tapl_common.TaplUiObject
import android.platform.uiautomatorhelpers.BetterSwipe
import android.platform.uiautomatorhelpers.DeviceHelpers.assertInvisible
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.uiDevice
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForFirstObj
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import android.platform.uiautomatorhelpers.FLING_GESTURE_INTERPOLATOR
import android.platform.uiautomatorhelpers.scrollUntilFound
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiSelector
import java.time.Duration
import java.util.regex.Pattern

/** System UI test automation object representing quick settings in the notification shade. */
class QuickSettings internal constructor() {
    // TODO(279061302): Remove TaplUiObject after BetterSwipe has a scroll wrapper.
    private val pager: TaplUiObject

    private val clazzNamePattern = Pattern.compile("android\\.widget\\.((Switch)|(Button))")

    init {
        UI_QUICK_SETTINGS_CONTAINER_ID.assertVisible { "Quick settings didn't open" }
        FOOTER_SELECTOR.assertVisible()
        pager = TaplUiDevice.waitForObject(PAGER_UI_OBJECT_SELECTOR, "QS pager")
    }

    /** Presses Power button to open the power panel. */
    fun openPowerPanel(): PowerPanel {
        waitForObj(sysuiResSelector(POWER_BTN_RES_ID)).click()
        return PowerPanel()
    }

    /** Presses Settings button to open Settings. */
    fun openSettings() {
        waitForObj(sysuiResSelector(SETTINGS_BUTTON_RES_ID)).click()
        By.pkg(SETTINGS_PACKAGE).assertVisible()
    }

    /** Opens the user selection panel by clicking User Switch button. */
    fun openUserSelection(): UserSelectionPanel {
        // Click on the multi user switch icon. http://go/hsv/5465641194618880?node=84
        waitForObj(sysuiResSelector(MULTI_USER_SWITCH_RES_ID)).click()
        return UserSelectionPanel()
    }

    /** Finds a tile by the prefix of its description */
    fun findTile(tileDesc: String): QuickSettingsTile {
        // Select by title_label https://hsv.googleplex.com/5476758214148096?node=57
        val titleLabelSelector = sysuiResSelector("tile_label").textStartsWith(tileDesc)
        val tileSelector = By.clazz(clazzNamePattern).hasDescendant(titleLabelSelector, 3)
        waitForObj(tileSelector)
        return QuickSettingsTile(tileSelector)
    }

    fun findComposeTile(tileDesc: String): ComposeQuickSettingsTile {
        val smallTileSelector = ComposeQuickSettingsTile.smallTileSelector(tileDesc)
        val largeTileSelector = ComposeQuickSettingsTile.largeTileSelector(tileDesc)
        val (_, selector) = waitForFirstObj(smallTileSelector, largeTileSelector)
        return ComposeQuickSettingsTile.createFrom(selector)
    }

    /** Returns the brightness slider. */
    val brightnessSlider: BrightnessSlider
        get() = BrightnessSlider()

    /** Returns the QS header. */
    val header: QSHeader
        get() = QSHeader()

    /**
     * Returns the visible UMO, or fails if it's not visible.
     *
     * **See:** [HSV](https://hsv.googleplex.com/6702722561605632?node=85)
     */
    val universalMediaObject: UniversalMediaObject
        get() {
            val footerTop = waitForObj(QUICK_SETTING_FOOTER_SELECTOR).visibleBounds.top
            waitForObj(QUICK_SETTINGS_SCROLLVIEW_SELECTOR).scrollUntilFound(
                UniversalMediaObject.MEDIA_CAROUSEL_SCROLLER,
                direction = Direction.DOWN,
            ) {
                val umoBottom = it.visibleBounds.bottom
                umoBottom - footerTop <= 0
            }
            return UniversalMediaObject()
        }

    /** Swipes up back to QQS or closes shade in case of split shade. */
    fun close() {
        swipeUp()
        UI_QUICK_SETTINGS_CONTAINER_ID.assertInvisible()
    }

    fun swipeLeft() {
        pager.fling(Direction.LEFT, 0.5f)
    }

    private fun swipeUp() {
        val displayWidth = uiDevice.displayWidth
        val displayHeight = uiDevice.displayHeight
        BetterSwipe.from(PointF((displayWidth / 2).toFloat(), displayHeight.toFloat() - 1f))
            .to(
                PointF((displayWidth / 2).toFloat(), 0f),
                Duration.ofMillis(500),
                FLING_GESTURE_INTERPOLATOR,
            )
            .release()
    }

    private companion object {
        val UI_QUICK_SETTINGS_CONTAINER_ID = sysuiResSelector("quick_settings_panel")
        val FOOTER_SELECTOR = sysuiResSelector("qs_footer_actions")

        // https://hsv.googleplex.com/5291196806070272?node=109
        const val POWER_BTN_RES_ID = "pm_lite"

        // https://hsv.googleplex.com/5291196806070272?node=108
        const val SETTINGS_BUTTON_RES_ID = "settings_button_container"

        // http://go/hsv/5465641194618880?node=84
        const val MULTI_USER_SWITCH_RES_ID = "multi_user_switch"
        const val PAGER_CLASS_NAME = "androidx.viewpager.widget.ViewPager"
        const val PAGER_RES_ID = "$SYSUI_PACKAGE:id/qs_pager"
        val PAGER_SELECTOR = UiSelector().className(PAGER_CLASS_NAME).resourceId(PAGER_RES_ID)
        const val PAGER_UI_OBJECT_RES_ID = "qs_pager"
        val PAGER_UI_OBJECT_SELECTOR = sysuiResSelector(PAGER_UI_OBJECT_RES_ID)
        private val QUICK_SETTINGS_SCROLLVIEW_SELECTOR = sysuiResSelector("expanded_qs_scroll_view")
        private val QUICK_SETTING_FOOTER_SELECTOR = sysuiResSelector("qs_footer_actions")
    }
}
