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

package android.platform.systemui_tapl.controller;

import static android.platform.uiautomator_helpers.DeviceHelpers.getUiDevice;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import android.system.helpers.QuickSettingsHelper;

import java.util.List;

/** Controller for manipulating quick settings contents. */
public class QuickSettingsController {
    private final QuickSettingsHelper mHelper;

    /** Returns an instance of QuickSettingsController. */
    public static QuickSettingsController get() {
        return new QuickSettingsController();
    }

    private QuickSettingsController() {
        mHelper = new QuickSettingsHelper(getUiDevice(), getInstrumentation());
    }

    /** Places a tile in quick settings as the first tile. Add the tile if necessary */
    public void addAsFirstTile(String tileName) {
        mHelper.setFirstQS(tileName);
    }

    /** Restore the default tile set. */
    public void restoreDefaultTiles() {
        mHelper.setQuickSettingsDefaultTiles();
    }

    /** Gets the list of default QS tiles. */
    public List<String> getDefaultTiles() {
        return mHelper.getQSDefaultTileList();
    }

    /** Gets the list of current QS tiles. */
    public List<String> getCurrentTiles() {
        return mHelper.getCurrentTilesList();
    }

    /**
     * Sets the list of tiles to a given list.
     *
     * @param tiles list of specs for the tiles.
     */
    public void setTiles(List<String> tiles) {
        mHelper.modifyQSTileList(tiles);
    }
}
