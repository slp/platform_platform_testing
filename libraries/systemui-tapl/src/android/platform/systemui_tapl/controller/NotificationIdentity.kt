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

package android.platform.systemui_tapl.controller

/**
 * A way to uniquely identify a notification. It's produced by posting a notification and can be
 * passed to methods for finding a notification.
 */
data class NotificationIdentity
@JvmOverloads
constructor(
    val type: Type,
    val title: String? = null,
    val text: String? = null,
    val summary: String? = null,
    val textWhenExpanded: String? = null,
    val contentIsVisibleInCollapsedState: Boolean = false,
    val pkg: String? = null,
    val hasAction: Boolean = false,
) {
    enum class Type {
        GROUP,
        GROUP_MINIMIZED,
        GROUP_AUTO_GENERATED,
        BIG_PICTURE,
        BIG_TEXT,
        MESSAGING_STYLE,
        CONVERSATION,
        BY_TITLE,
        BY_TEXT,
        CALL,
        INBOX,
        MEDIA,
        CUSTOM,
    }
}
