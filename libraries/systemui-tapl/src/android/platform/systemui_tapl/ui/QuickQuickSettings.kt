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

import android.platform.systemui_tapl.ui.ComposeQuickSettingsTile.Companion.assertIsTile
import android.platform.systemui_tapl.utils.DeviceUtils.LONG_WAIT
import android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisibility
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForFirstObj
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import androidx.test.uiautomator.UiObject2
import com.android.systemui.Flags

/**
 * System UI test automation object representing quick quick settings in the notification shade.
 *
 * This is the area that contains a few tiles and doesn't have pages, appearing on top of the
 * Notification space.
 *
 * https://hsv.googleplex.com/4814389392703488?node=15#
 */
class QuickQuickSettings internal constructor() {

    private val qqsTilesContainer: UiObject2

    init {
        UI_QUICK_QUICK_SETTINGS_CONTAINER_SELECTOR.assertVisible(timeout = LONG_WAIT) {
            "Quick quick settings not visible"
        }
        qqsTilesContainer =
            waitForObj(UI_QQS_TILE_LAYOUT_SELECTOR) {
                "Quick quick settings does not have a tile layout"
            }
    }

    /**
     * Get a list of [QuickSettingsTile] objects, each representing one of the tiles visible in the
     * QuickQuickSettings container. Will fail if there's an element that's not a tile (i.e.,
     * doesn't have the label view as https://hsv.googleplex.com/4814389392703488?node=22#), only
     * when the [qsUiRefactorComposeFragment] flag is false.
     */
    fun getVisibleTiles(): List<QuickQuickSettingsTile> {
        val uiTiles = qqsTilesContainer.children
        if (!Flags.qsUiRefactorComposeFragment()) {
            uiTiles.forEach { tile ->
                tile.assertVisibility(UI_TILE_LABEL_SELECTOR, visible = true)
            }
        }
        return uiTiles.map { QuickQuickSettingsTile(it) }
    }

    fun getVisibleComposeTiles(): List<ComposeQuickSettingsTile> {
        val uiChildren = qqsTilesContainer.children
        val largeTileSelector = sysuiResSelector(ComposeQuickSettingsTile.LARGE_TILE_TAG)
        val smallTileSelector = sysuiResSelector(ComposeQuickSettingsTile.SMALL_TILE_TAG)
        val uiTiles =
            uiChildren.map { child ->
                val (tile, _) =
                    child.waitForFirstObj(smallTileSelector, largeTileSelector) {
                        "No tile object found in child $child"
                    }
                tile.assertIsTile()
                tile
            }
        return uiTiles.map { ComposeQuickSettingsTile.createFrom(it) }
    }

    companion object {
        @JvmField
        val UI_QUICK_QUICK_SETTINGS_CONTAINER_SELECTOR = sysuiResSelector("quick_qs_panel")
        // https://hsv.googleplex.com/4814389392703488?node=16#
        private val UI_QQS_TILE_LAYOUT_SELECTOR = sysuiResSelector("qqs_tile_layout")
        @JvmField
        // https://hsv.googleplex.com/4814389392703488?node=22#
        val UI_TILE_LABEL_SELECTOR = sysuiResSelector("tile_label")
    }
}
