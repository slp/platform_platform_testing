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

package android.tools.traces.parsers.perfetto

import android.graphics.Insets
import android.graphics.Rect
import android.tools.PlatformConsts
import android.tools.Rotation
import android.tools.datatypes.Size
import android.tools.traces.wm.Activity
import android.tools.traces.wm.ColorMode
import android.tools.traces.wm.Configuration
import android.tools.traces.wm.ConfigurationContainer
import android.tools.traces.wm.ConfigurationContainerImpl
import android.tools.traces.wm.DisplayArea
import android.tools.traces.wm.DisplayContent
import android.tools.traces.wm.DisplayCutout
import android.tools.traces.wm.InsetsSource
import android.tools.traces.wm.InsetsSourceProvider
import android.tools.traces.wm.KeyguardControllerState
import android.tools.traces.wm.PixelFormat
import android.tools.traces.wm.RootWindowContainer
import android.tools.traces.wm.RotationAnimation
import android.tools.traces.wm.ScreenOrientation
import android.tools.traces.wm.Task
import android.tools.traces.wm.TaskFragment
import android.tools.traces.wm.UserRotationMode
import android.tools.traces.wm.WindowConfiguration
import android.tools.traces.wm.WindowContainer
import android.tools.traces.wm.WindowContainerImpl
import android.tools.traces.wm.WindowLayoutParams
import android.tools.traces.wm.WindowManagerPolicy
import android.tools.traces.wm.WindowManagerState
import android.tools.traces.wm.WindowState
import android.tools.traces.wm.WindowToken

/** Builder for [WindowManagerState] objects */
class WindowManagerStateBuilder(val entry: Args, val realToElapsedTimeOffsetNs: Long) {
    var computedZCounter = 0

    fun build(): WindowManagerState {
        computedZCounter = 0

        val service =
            entry.getChild("window_manager_service")
                ?: error("window_manager_service field should not be null")
        val elapsedTimestampNs =
            entry.getChild("elapsed_realtime_nanos")?.getLong()
                ?: error("elapsed_realtime_nanos field should not be null")
        val realTimestampNs = elapsedTimestampNs + realToElapsedTimeOffsetNs

        return WindowManagerState(
            elapsedTimestamp = elapsedTimestampNs,
            clockTimestamp = realTimestampNs,
            where = entry.getChild("where")?.getString() ?: "",
            policy = buildPolicy(service.getChild("policy")!!),
            focusedApp = service.getChild("focused_app")?.getString() ?: "",
            focusedDisplayId = service.getChild("focused_display_id")?.getInt() ?: 0,
            _focusedWindow =
                service.getChild("focused_window")?.getChild("title")?.getString() ?: "",
            inputMethodWindowAppToken =
                service.getChild("input_method_window")?.getChild("hash_code")?.getInt()?.let {
                    Integer.toHexString(it)
                } ?: "",
            isHomeRecentsComponent =
                service
                    .getChild("root_window_container")
                    ?.getChild("is_home_recents_component")
                    ?.getBoolean() ?: false,
            isDisplayFrozen = service.getChild("display_frozen")?.getBoolean() ?: false,
            _pendingActivities =
                service.getChild("root_window_container")?.getChildren("pending_activities")?.map {
                    it.getChild("title")?.getString() ?: ""
                } ?: emptyList(),
            root = buildRootWindowContainer(service.getChild("root_window_container")!!),
            keyguardControllerState =
                buildKeyguardControllerState(
                    service.getChild("root_window_container")?.getChild("keyguard_controller")
                )
        )
    }

    private fun buildPolicy(windowManagerPolicyProto: Args): WindowManagerPolicy {
        return WindowManagerPolicy.from(
            focusedAppToken =
                windowManagerPolicyProto.getChild("focused_app_token")?.getString() ?: "",
            forceStatusBar =
                windowManagerPolicyProto.getChild("force_status_bar")?.getBoolean() ?: false,
            forceStatusBarFromKeyguard =
                windowManagerPolicyProto.getChild("force_status_bar_from_keyguard")?.getBoolean()
                    ?: false,
            keyguardDrawComplete =
                windowManagerPolicyProto.getChild("keyguard_draw_complete")?.getBoolean() ?: false,
            keyguardOccluded =
                windowManagerPolicyProto.getChild("keyguard_occluded")?.getBoolean() ?: false,
            keyguardOccludedChanged =
                windowManagerPolicyProto.getChild("keyguard_occluded_changed")?.getBoolean()
                    ?: false,
            keyguardOccludedPending =
                windowManagerPolicyProto.getChild("keyguard_occluded_pending")?.getBoolean()
                    ?: false,
            lastSystemUiFlags =
                windowManagerPolicyProto.getChild("last_system_ui_flags")?.getInt() ?: 0,
            orientation =
                ScreenOrientation.fromName(
                        windowManagerPolicyProto.getChild("orientation")?.getString()
                    )
                    ?.value ?: 0,
            rotation =
                Rotation.getByName(windowManagerPolicyProto.getChild("rotation")?.getString())
                    ?: Rotation.ROTATION_0,
            rotationMode =
                UserRotationMode.fromName(
                        windowManagerPolicyProto.getChild("rotation_mode")?.getString()
                    )
                    ?.value ?: 0,
            screenOnFully =
                windowManagerPolicyProto.getChild("screen_on_fully")?.getBoolean() ?: false,
            windowManagerDrawComplete =
                windowManagerPolicyProto.getChild("window_manager_draw_complete")?.getBoolean()
                    ?: false
        )
    }

    private fun buildRootWindowContainer(rootWindowContainerProto: Args): RootWindowContainer {
        val windowContainer =
            buildWindowContainer(
                windowContainerProto = rootWindowContainerProto.getChild("window_container"),
                windowContainerChildProtos =
                    rootWindowContainerProto.getChild("window_container")?.getChildren("children"),
                isActivityInTree = false
            ) ?: error("Window container should not be null")

        return RootWindowContainer(windowContainer)
    }

    private fun buildWindowContainer(
        windowContainerProto: Args?,
        windowContainerChildProtos: List<Args>?,
        isActivityInTree: Boolean,
        nameOverride: String? = null,
        visibleOverride: Boolean? = null
    ): WindowContainer? {
        if (windowContainerProto == null) {
            return null
        }

        val children =
            windowContainerChildProtos?.mapNotNull {
                buildWindowContainerChild(it, isActivityInTree)
            } ?: listOf<WindowContainer>()

        return WindowContainerImpl(
            title =
                nameOverride
                    ?: windowContainerProto.getChild("identifier")?.getChild("title")?.getString()
                    ?: "",
            token =
                windowContainerProto.getChild("identifier")?.getChild("hash_code")?.getInt()?.let {
                    Integer.toHexString(it)
                } ?: "",
            orientation = windowContainerProto.getChild("orientation")?.getInt() ?: 0,
            _isVisible =
                visibleOverride ?: windowContainerProto.getChild("visible")?.getBoolean() ?: false,
            configurationContainer =
                buildConfigurationContainer(
                    windowContainerProto.getChild("configuration_container")
                ),
            layerId =
                windowContainerProto.getChild("surfaceControl")?.getChild("layerId")?.getInt() ?: 0,
            _children = children,
            computedZ = computedZCounter++
        )
    }

    private fun buildConfigurationContainer(
        configurationContainerProto: Args?
    ): ConfigurationContainer {
        return ConfigurationContainerImpl.from(
            overrideConfiguration =
                buildConfiguration(configurationContainerProto?.getChild("override_configuration")),
            fullConfiguration =
                buildConfiguration(configurationContainerProto?.getChild("full_configuration")),
            mergedOverrideConfiguration =
                buildConfiguration(
                    configurationContainerProto?.getChild("merged_override_configuration")
                )
        )
    }

    private fun buildConfiguration(configurationProto: Args?): Configuration? {
        if (configurationProto == null) {
            return null
        }

        return Configuration.from(
            windowConfiguration =
                buildWindowConfiguration(configurationProto.getChild("window_configuration")),
            densityDpi = configurationProto.getChild("density_dpi")?.getInt() ?: 0,
            orientation = configurationProto.getChild("orientation")?.getInt() ?: 0,
            screenHeightDp = configurationProto.getChild("screen_height_dp")?.getInt() ?: 0,
            screenWidthDp = configurationProto.getChild("screen_width_dp")?.getInt() ?: 0,
            smallestScreenWidthDp =
                configurationProto.getChild("smallest_screen_width_dp")?.getInt() ?: 0,
            screenLayout = configurationProto.getChild("screen_layout")?.getInt() ?: 0,
            uiMode = configurationProto.getChild("ui_mode")?.getInt() ?: 0
        )
    }

    private fun buildWindowConfiguration(windowConfigurationProto: Args?): WindowConfiguration? {
        if (windowConfigurationProto == null) {
            return null
        }

        return WindowConfiguration.from(
            appBounds = buildRect(windowConfigurationProto.getChild("app_bounds")),
            bounds = buildRect(windowConfigurationProto.getChild("bounds")),
            maxBounds = buildRect(windowConfigurationProto.getChild("max_bounds")),
            windowingMode = windowConfigurationProto.getChild("windowing_mode")?.getInt() ?: 0,
            activityType = windowConfigurationProto.getChild("activity_type")?.getInt() ?: 0
        )
    }

    private fun buildKeyguardControllerState(
        keyguardControllerProto: Args?
    ): KeyguardControllerState {
        return KeyguardControllerState.from(
            isAodShowing = keyguardControllerProto?.getChild("aod_showing")?.getBoolean() ?: false,
            isKeyguardShowing =
                keyguardControllerProto?.getChild("keyguard_showing")?.getBoolean() ?: false,
            keyguardOccludedStates =
                keyguardControllerProto?.getChildren("keyguard_occluded_states")?.associate {
                    val displayId = it.getChild("display_id")?.getInt() ?: 0
                    val keyguardOccuded = it.getChild("keyguard_occluded")?.getBoolean() ?: false
                    displayId to keyguardOccuded
                } ?: emptyMap()
        )
    }

    private fun buildWindowContainerChild(
        windowContainerChildProto: Args,
        isActivityInTree: Boolean
    ): WindowContainer? {
        return buildDisplayContent(
            windowContainerChildProto.getChild("display_content"),
            isActivityInTree
        )
            ?: buildDisplayArea(
                windowContainerChildProto.getChild("display_area"),
                isActivityInTree
            )
            ?: buildTask(windowContainerChildProto.getChild("task"), isActivityInTree)
            ?: buildTaskFragment(
                windowContainerChildProto.getChild("task_fragment"),
                isActivityInTree
            )
            ?: buildActivity(windowContainerChildProto.getChild("activity"))
            ?: buildWindowToken(
                windowContainerChildProto.getChild("window_token"),
                isActivityInTree
            )
            ?: buildWindowState(windowContainerChildProto.getChild("window"), isActivityInTree)
            ?: buildWindowContainer(
                windowContainerChildProto.getChild("window_container"),
                windowContainerChildProtos = null,
                isActivityInTree = isActivityInTree
            )
    }

    private fun buildDisplayContent(
        displayContentProto: Args?,
        isActivityInTree: Boolean
    ): DisplayContent? {
        if (displayContentProto == null) {
            return null
        }

        return DisplayContent(
            displayId = displayContentProto.getChild("id")?.getInt() ?: 0,
            focusedRootTaskId = displayContentProto.getChild("focused_root_task_id")?.getInt() ?: 0,
            resumedActivity =
                displayContentProto.getChild("resumed_activity")?.getChild("title")?.getString()
                    ?: "",
            singleTaskInstance =
                displayContentProto.getChild("single_task_instance")?.getBoolean() ?: false,
            defaultPinnedStackBounds =
                buildRect(
                    displayContentProto
                        .getChild("pinned_task_controller")
                        ?.getChild("default_bounds")
                ),
            pinnedStackMovementBounds =
                buildRect(
                    displayContentProto
                        .getChild("pinned_task_controller")
                        ?.getChild("movement_bounds")
                ),
            displayRect =
                Rect(
                    0,
                    0,
                    displayContentProto
                        .getChild("display_info")
                        ?.getChild("logical_width")
                        ?.getInt() ?: 0,
                    displayContentProto
                        .getChild("display_info")
                        ?.getChild("logical_height")
                        ?.getInt() ?: 0
                ),
            appRect =
                Rect(
                    0,
                    0,
                    displayContentProto.getChild("display_info")?.getChild("app_width")?.getInt()
                        ?: 0,
                    displayContentProto.getChild("display_info")?.getChild("app_height")?.getInt()
                        ?: 0
                ),
            dpi = displayContentProto.getChild("dpi")?.getInt() ?: 0,
            flags = displayContentProto.getChild("display_info")?.getChild("flags")?.getInt() ?: 0,
            stableBounds =
                buildRect(
                    displayContentProto.getChild("display_frames")?.getChild("stable_bounds")
                ),
            surfaceSize = displayContentProto.getChild("surface_size")?.getInt() ?: 0,
            focusedApp = displayContentProto.getChild("focused_app")?.getString() ?: "",
            lastTransition =
                displayContentProto
                    .getChild("app_transition")
                    ?.getChild("last_used_app_transition")
                    ?.getString() ?: DEFAULT_TRANSITION_TYPE,
            appTransitionState =
                displayContentProto
                    .getChild("app_transition")
                    ?.getChild("app_transition_state")
                    ?.getString() ?: DEFAULT_APP_STATE,
            rotation =
                Rotation.getByValue(
                    displayContentProto.getChild("display_rotation")?.getChild("rotation")?.getInt()
                        ?: 0
                ),
            lastOrientation =
                displayContentProto
                    .getChild("display_rotation")
                    ?.getChild("last_orientation")
                    ?.getInt() ?: 0,
            cutout =
                buildDisplayCutout(
                    displayContentProto.getChild("display_info")?.getChild("cutout")
                ),
            insetsSourceProviders =
                buildInsetsSourceProviders(
                    displayContentProto.getChildren("insets_source_providers"),
                ),
            windowContainer =
                buildWindowContainer(
                    windowContainerProto =
                        displayContentProto
                            .getChild("root_display_area")
                            ?.getChild("window_container"),
                    windowContainerChildProtos =
                        displayContentProto
                            .getChild("root_display_area")
                            ?.getChild("window_container")
                            ?.getChildren("children"),
                    isActivityInTree = isActivityInTree,
                    nameOverride =
                        displayContentProto.getChild("display_info")?.getChild("name")?.getString()
                            ?: ""
                ) ?: error("Window container should not be null")
        )
    }

    private fun buildDisplayArea(displayAreaProto: Args?, isActivityInTree: Boolean): DisplayArea? {
        if (displayAreaProto == null) {
            return null
        }

        return DisplayArea(
            isTaskDisplayArea =
                displayAreaProto.getChild("is_task_display_area")?.getBoolean() ?: false,
            windowContainer =
                buildWindowContainer(
                    windowContainerProto = displayAreaProto.getChild("window_container"),
                    windowContainerChildProtos =
                        displayAreaProto.getChild("window_container")?.getChildren("children"),
                    isActivityInTree = isActivityInTree,
                ) ?: error("Window container should not be null")
        )
    }

    private fun buildTask(taskProto: Args?, isActivityInTree: Boolean): Task? {
        if (taskProto == null) {
            return null
        }

        return Task(
            activityType =
                taskProto.getChild("task_fragment")?.getChild("activity_type")?.getInt()
                    ?: taskProto.getChild("activity_type")?.getInt()
                    ?: 0,
            isFullscreen = taskProto.getChild("fills_parent")?.getBoolean() ?: false,
            bounds = buildRect(taskProto.getChild("bounds")),
            taskId = taskProto.getChild("id")?.getInt() ?: 0,
            rootTaskId = taskProto.getChild("root_task_id")?.getInt() ?: 0,
            displayId =
                taskProto.getChild("task_fragment")?.getChild("display_id")?.getInt()
                    ?: taskProto.getChild("display_id")?.getInt()
                    ?: 0,
            lastNonFullscreenBounds = buildRect(taskProto.getChild("last_non_fullscreen_bounds")),
            realActivity = taskProto.getChild("real_activity")?.getString() ?: "",
            origActivity = taskProto.getChild("orig_activity")?.getString() ?: "",
            resizeMode = taskProto.getChild("resize_mode")?.getInt() ?: 0,
            _resumedActivity =
                taskProto.getChild("resumed_activity")?.getChild("title")?.getString() ?: "",
            animatingBounds = taskProto.getChild("animating_bounds")?.getBoolean() ?: false,
            surfaceWidth = taskProto.getChild("surface_width")?.getInt() ?: 0,
            surfaceHeight = taskProto.getChild("surface_height")?.getInt() ?: 0,
            createdByOrganizer = taskProto.getChild("created_by_organizer")?.getBoolean() ?: false,
            minWidth =
                taskProto.getChild("task_fragment")?.getChild("min_width")?.getInt()
                    ?: taskProto.getChild("min_width")?.getInt()
                    ?: 0,
            minHeight =
                taskProto.getChild("task_fragment")?.getChild("min_height")?.getInt()
                    ?: taskProto.getChild("min_height")?.getInt()
                    ?: 0,
            windowContainer =
                buildWindowContainer(
                    windowContainerProto =
                        taskProto.getChild("task_fragment")?.getChild("window_container")
                            ?: taskProto.getChild("window_container"),
                    windowContainerChildProtos =
                        if (taskProto.getChild("task_fragment") != null) {
                            taskProto
                                .getChild("task_fragment")
                                ?.getChild("window_container")
                                ?.getChildren("children")
                        } else {
                            taskProto.getChild("window_container")?.getChildren("children")
                        },
                    isActivityInTree = isActivityInTree
                ) ?: error("Window container should not be null")
        )
    }

    private fun buildTaskFragment(
        taskFragmentProto: Args?,
        isActivityInTree: Boolean
    ): TaskFragment? {
        if (taskFragmentProto == null) {
            return null
        }

        return TaskFragment(
            activityType = taskFragmentProto.getChild("activity_type")?.getInt() ?: 0,
            displayId = taskFragmentProto.getChild("display_id")?.getInt() ?: 0,
            minWidth = taskFragmentProto.getChild("min_width")?.getInt() ?: 0,
            minHeight = taskFragmentProto.getChild("min_height")?.getInt() ?: 0,
            windowContainer =
                buildWindowContainer(
                    windowContainerProto = taskFragmentProto.getChild("window_container"),
                    windowContainerChildProtos =
                        taskFragmentProto.getChild("window_container")?.getChildren("children"),
                    isActivityInTree = isActivityInTree
                ) ?: error("Window container should not be null")
        )
    }

    private fun buildActivity(activityRecordProto: Args?): Activity? {
        if (activityRecordProto == null) {
            return null
        }

        return Activity(
            state = activityRecordProto.getChild("state")?.getString() ?: "",
            frontOfTask = activityRecordProto.getChild("front_of_task")?.getBoolean() ?: false,
            procId = activityRecordProto.getChild("proc_id")?.getInt() ?: 0,
            isTranslucent = activityRecordProto.getChild("translucent")?.getBoolean() ?: false,
            windowContainer =
                buildWindowContainer(
                    windowContainerProto =
                        activityRecordProto.getChild("window_token")?.getChild("window_container"),
                    windowContainerChildProtos =
                        activityRecordProto
                            .getChild("window_token")
                            ?.getChild("window_container")
                            ?.getChildren("children"),
                    isActivityInTree = true,
                    nameOverride = activityRecordProto.getChild("name")?.getString() ?: ""
                ) ?: error("Window container should not be null")
        )
    }

    private fun buildWindowToken(windowTokenProto: Args?, isActivityInTree: Boolean): WindowToken? {
        if (windowTokenProto == null) {
            return null
        }

        return WindowToken(
            buildWindowContainer(
                windowContainerProto = windowTokenProto.getChild("window_container"),
                windowContainerChildProtos =
                    windowTokenProto.getChild("window_container")?.getChildren("children"),
                isActivityInTree = isActivityInTree
            ) ?: error("Window container should not be null")
        )
    }

    private fun buildWindowState(windowStateProto: Args?, isActivityInTree: Boolean): WindowState? {
        if (windowStateProto == null) {
            return null
        }

        val identifierName =
            windowStateProto
                .getChild("window_container")
                ?.getChild("identifier")
                ?.getChild("title")
                ?.getString() ?: ""
        return WindowState(
            attributes = buildWindowLayoutParams(windowStateProto.getChild("attributes")),
            displayId = windowStateProto.getChild("display_id")?.getInt() ?: 0,
            stackId = windowStateProto.getChild("stack_id")?.getInt() ?: 0,
            layer =
                windowStateProto
                    .getChild("animator")
                    ?.getChild("surface")
                    ?.getChild("layer")
                    ?.getInt() ?: 0,
            isSurfaceShown =
                windowStateProto
                    .getChild("animator")
                    ?.getChild("surface")
                    ?.getChild("shown")
                    ?.getBoolean() ?: false,
            windowType =
                when {
                    identifierName.startsWith(PlatformConsts.STARTING_WINDOW_PREFIX) ->
                        PlatformConsts.WINDOW_TYPE_STARTING
                    windowStateProto.getChild("animating_exit")?.getBoolean() ?: false ->
                        PlatformConsts.WINDOW_TYPE_EXITING
                    identifierName.startsWith(PlatformConsts.DEBUGGER_WINDOW_PREFIX) ->
                        PlatformConsts.WINDOW_TYPE_STARTING
                    else -> 0
                },
            requestedSize =
                Size.from(
                    windowStateProto.getChild("requested_width")?.getInt() ?: 0,
                    windowStateProto.getChild("requested_height")?.getInt() ?: 0
                ),
            surfacePosition = buildRect(windowStateProto.getChild("surface_position")),
            frame = buildRect(windowStateProto.getChild("window_frames")?.getChild("frame")),
            containingFrame =
                buildRect(windowStateProto.getChild("window_frames")?.getChild("containing_frame")),
            parentFrame =
                buildRect(windowStateProto.getChild("window_frames")?.getChild("parent_frame")),
            contentFrame =
                buildRect(windowStateProto.getChild("window_frames")?.getChild("content_frame")),
            contentInsets =
                buildRect(windowStateProto.getChild("window_frames")?.getChild("content_insets")),
            surfaceInsets = buildRect(windowStateProto.getChild("surface_insets")),
            givenContentInsets = buildRect(windowStateProto.getChild("given_content_insets")),
            crop = buildRect(windowStateProto.getChild("animator")?.getChild("last_clip_rect")),
            windowContainer =
                buildWindowContainer(
                    windowContainerProto = windowStateProto.getChild("window_container"),
                    windowContainerChildProtos =
                        windowStateProto.getChild("window_container")?.getChildren("children"),
                    isActivityInTree = isActivityInTree,
                    nameOverride =
                        getWindowTitle(
                            when {
                                // Existing code depends on the prefix being removed
                                identifierName.startsWith(PlatformConsts.STARTING_WINDOW_PREFIX) ->
                                    identifierName.substring(
                                        PlatformConsts.STARTING_WINDOW_PREFIX.length
                                    )
                                identifierName.startsWith(PlatformConsts.DEBUGGER_WINDOW_PREFIX) ->
                                    identifierName.substring(
                                        PlatformConsts.DEBUGGER_WINDOW_PREFIX.length
                                    )
                                else -> identifierName
                            }
                        )
                ) ?: error("Window container should not be null"),
            isAppWindow = isActivityInTree
        )
    }

    private fun buildDisplayCutout(displayCutoutProto: Args?): DisplayCutout? {
        if (displayCutoutProto == null) {
            return null
        }

        return DisplayCutout.from(
            buildInsets(displayCutoutProto.getChild("insets")),
            buildRect(displayCutoutProto.getChild("bound_left")),
            buildRect(displayCutoutProto.getChild("bound_top")),
            buildRect(displayCutoutProto.getChild("bound_right")),
            buildRect(displayCutoutProto.getChild("bound_bottom")),
            buildInsets(displayCutoutProto.getChild("waterfall_insets"))
        )
    }

    private fun buildInsetsSource(insetsSourceProto: Args?): InsetsSource? {
        if (insetsSourceProto == null) {
            return null
        }

        return InsetsSource.from(
            type = insetsSourceProto.getChild("type_number")?.getInt() ?: -1,
            frame = buildRect(insetsSourceProto.getChild("frame")),
            visible = insetsSourceProto.getChild("visible")?.getBoolean() ?: false,
        )
    }

    private fun buildInsetsSourceProviders(
        insetsProvidersProto: List<Args>?
    ): Array<InsetsSourceProvider> {
        return insetsProvidersProto
            ?.map {
                InsetsSourceProvider(
                    buildRect(it.getChild("frame")),
                    buildInsetsSource(it.getChild("source"))
                )
            }
            ?.toTypedArray() ?: emptyArray<InsetsSourceProvider>()
    }

    private fun buildWindowLayoutParams(windowLayoutParamsProto: Args?): WindowLayoutParams {
        return WindowLayoutParams.from(
            type = windowLayoutParamsProto?.getChild("type")?.getInt() ?: 0,
            x = windowLayoutParamsProto?.getChild("x")?.getInt() ?: 0,
            y = windowLayoutParamsProto?.getChild("y")?.getInt() ?: 0,
            width = windowLayoutParamsProto?.getChild("width")?.getInt() ?: 0,
            height = windowLayoutParamsProto?.getChild("height")?.getInt() ?: 0,
            horizontalMargin =
                windowLayoutParamsProto?.getChild("horizontal_margin")?.getFloat() ?: 0f,
            verticalMargin = windowLayoutParamsProto?.getChild("vertical_margin")?.getFloat() ?: 0f,
            gravity = windowLayoutParamsProto?.getChild("gravity")?.getInt() ?: 0,
            softInputMode = windowLayoutParamsProto?.getChild("soft_input_mode")?.getInt() ?: 0,
            format =
                PixelFormat.fromName(windowLayoutParamsProto?.getChild("format")?.getString())
                    ?.value ?: 0,
            windowAnimations =
                windowLayoutParamsProto?.getChild("window_animations")?.getInt() ?: 0,
            alpha = windowLayoutParamsProto?.getChild("alpha")?.getFloat() ?: 0f,
            screenBrightness =
                windowLayoutParamsProto?.getChild("screen_brightness")?.getFloat() ?: 0f,
            buttonBrightness =
                windowLayoutParamsProto?.getChild("button_brightness")?.getFloat() ?: 0f,
            rotationAnimation =
                RotationAnimation.fromName(
                        windowLayoutParamsProto?.getChild("rotation_animation")?.getString()
                    )
                    ?.value ?: 0,
            preferredRefreshRate =
                windowLayoutParamsProto?.getChild("preferred_refresh_rate")?.getFloat() ?: 0f,
            preferredDisplayModeId =
                windowLayoutParamsProto?.getChild("preferred_display_mode_id")?.getInt() ?: 0,
            hasSystemUiListeners =
                windowLayoutParamsProto?.getChild("has_system_ui_listeners")?.getBoolean() ?: false,
            inputFeatureFlags =
                windowLayoutParamsProto?.getChild("input_feature_flags")?.getInt() ?: 0,
            userActivityTimeout =
                windowLayoutParamsProto?.getChild("user_activity_timeout")?.getLong() ?: 0,
            colorMode =
                ColorMode.fromName(windowLayoutParamsProto?.getChild("color_mode")?.getString())
                    ?.value ?: 0,
            flags = windowLayoutParamsProto?.getChild("flags")?.getInt() ?: 0,
            privateFlags = windowLayoutParamsProto?.getChild("private_flags")?.getInt() ?: 0,
            systemUiVisibilityFlags =
                windowLayoutParamsProto?.getChild("system_ui_visibility_flags")?.getInt() ?: 0,
            subtreeSystemUiVisibilityFlags =
                windowLayoutParamsProto?.getChild("subtree_system_ui_visibility_flags")?.getInt()
                    ?: 0,
            appearance = windowLayoutParamsProto?.getChild("appearance")?.getInt() ?: 0,
            behavior = windowLayoutParamsProto?.getChild("behavior")?.getInt() ?: 0,
            fitInsetsTypes = windowLayoutParamsProto?.getChild("fit_insets_types")?.getInt() ?: 0,
            fitInsetsSides = windowLayoutParamsProto?.getChild("fit_insets_sides")?.getInt() ?: 0,
            fitIgnoreVisibility =
                windowLayoutParamsProto?.getChild("fit_ignore_visibility")?.getBoolean() ?: false
        )
    }

    private fun buildInsets(rectProto: Args?): Insets {
        if (rectProto == null) {
            return Insets.NONE
        }

        return Insets.of(
            rectProto?.getChild("left")?.getInt() ?: 0,
            rectProto?.getChild("top")?.getInt() ?: 0,
            rectProto?.getChild("right")?.getInt() ?: 0,
            rectProto?.getChild("bottom")?.getInt() ?: 0
        )
    }

    private fun buildRect(rectProto: Args?): Rect =
        Rect(
            rectProto?.getChild("left")?.getInt() ?: 0,
            rectProto?.getChild("top")?.getInt() ?: 0,
            rectProto?.getChild("right")?.getInt() ?: 0,
            rectProto?.getChild("bottom")?.getInt() ?: 0
        )

    private fun getWindowTitle(title: String): String {
        return when {
            // Existing code depends on the prefix being removed
            title.startsWith(PlatformConsts.STARTING_WINDOW_PREFIX) ->
                title.substring(PlatformConsts.STARTING_WINDOW_PREFIX.length)
            title.startsWith(PlatformConsts.DEBUGGER_WINDOW_PREFIX) ->
                title.substring(PlatformConsts.DEBUGGER_WINDOW_PREFIX.length)
            else -> title
        }
    }

    companion object {
        private const val DEFAULT_TRANSITION_TYPE = "TRANSIT_NONE"
        private const val DEFAULT_APP_STATE = "APP_STATE_IDLE"
    }
}
