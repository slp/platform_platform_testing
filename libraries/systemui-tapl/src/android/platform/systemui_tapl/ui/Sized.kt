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

import android.graphics.Rect

/**
 * Represents dimensions and position of a visible view on the screen.
 *
 * Allows to compare it with other [sized] objects **without** disclosing the real position.
 */
abstract class Sized internal constructor(private val rect: Rect) {
    /** Returns whether this class intersects with [other]. */
    infix fun intersect(other: Sized): Boolean = rect.intersect(other.rect)

    override fun equals(other: Any?): Boolean =
        if (other is Sized) {
            rect == other.rect
        } else {
            super.equals(other)
        }

    override fun hashCode(): Int = rect.hashCode()

    override fun toString(): String = "${this::class.simpleName}(bounds=$rect)"
}

/**
 * System UI test automation object representing the notification shelf.
 *
 * This is visible at the end of the [NotificationStack] only when there are notification that don't
 * fit in the available space.
 */
class NotificationShelf internal constructor(rect: Rect) : Sized(rect)

/** System UI test automation object representing the lock icon visible on lockscreen. */
class LockscreenLockIcon internal constructor(rect: Rect) : Sized(rect)
