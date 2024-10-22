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

package android.tools.traces.wm

import android.graphics.Rect
import android.tools.withCache

class InsetsSource private constructor(val type: Int, val frame: Rect, val visible: Boolean) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InsetsSource) return false

        if (type != other.type) return false
        if (frame != other.frame) return false
        if (visible != other.visible) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + frame.hashCode()
        result = 31 * result + visible.hashCode()
        return result
    }

    override fun toString(): String {
        return "InsetsSource(type=$type, frame=$frame, visible=$visible)"
    }

    companion object {
        fun from(type: Int, frame: Rect, visible: Boolean): InsetsSource = withCache {
            InsetsSource(type, frame, visible)
        }
    }
}
