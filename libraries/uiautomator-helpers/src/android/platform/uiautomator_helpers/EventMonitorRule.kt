/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package android.platform.uiautomator_helpers

import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * TestRule to set up the EventMonitor for logging.
 *
 * @param eventMask mask of event types to listen for.
 */
class EventMonitorRule() : TestWatcher() {
    private var eventMonitorStarted = false

    /**
     * Annotation to monitor and log events using an AccessibilityEventListener. The MonitorEvents
     * annotation can be added to test classes or individual tests.
     *
     * @param eventMask mask of [AccessibilityEvent.EventType] events
     */
    @kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
    @Target(FUNCTION, CLASS)
    annotation class MonitorEvents(val eventMask: Int = DEFAULT_EVENT_MASK) {
        companion object {
            const val DEFAULT_EVENT_MASK =
                AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_VIEW_LONG_CLICKED or
                    AccessibilityEvent.TYPE_VIEW_SCROLLED or
                    AccessibilityEvent.TYPE_VIEW_CONTEXT_CLICKED or
                    AccessibilityEvent.TYPE_TOUCH_INTERACTION_START or
                    AccessibilityEvent.TYPE_TOUCH_INTERACTION_END or
                    AccessibilityEvent.TYPE_GESTURE_DETECTION_START or
                    AccessibilityEvent.TYPE_GESTURE_DETECTION_END
        }
    }

    /**
     * Start the event monitor depending on the instrumentation parameter or
     * annotation.
     *
     * @param description test description from [org.junit.runner.Description]
     */
    override fun starting(description: Description) {
        if (isMonitorEnabledByInstrumentationParameter(description)) {
            EventMonitor.start()
            eventMonitorStarted = true
        } else {
            val annotation = getMonitorAnnotation(description)
            if (annotation != null) {
                EventMonitor.start(annotation.eventMask)
                Log.d(TAG, "event monitoring enabled by annotation, mask=$annotation.eventMask")
            } else {
                Log.d(TAG, "event monitoring disabled")
            }
        }
    }

    /**
     * Stop the event monitor if it has been started.
     *
     * @param description test description from [org.junit.runner.Description]
     */
    override fun finished(decsription: Description) {
        if (eventMonitorStarted) {
            EventMonitor.stop()
        }
    }

    /**
     * Check the class and test for the [MonitorEvents] annotation
     *
     * @param description test description from [org.junit.runner.Description]
     */
    private fun getMonitorAnnotation(description: Description): MonitorEvents? {
        // Pull the annotation from the test or class depending on description.isTest
        return if (description.isTest)
            description.getAnnotation(MonitorEvents::class.java)
                ?: description.testClass.getAnnotation(MonitorEvents::class.java)
        else description.testClass.getAnnotation(MonitorEvents::class.java)
    }

    /**
     * Condition to check the instrumentation parameter for the test method or class
     *
     * @param description test description from [org.junit.runner.Description]
     */
    private fun isMonitorEnabledByInstrumentationParameter(description: Description) =
        classLevelOverrideEnabled() || (description.isTest && testLevelOverrideEnabled())

    private fun classLevelOverrideEnabled() =
        eventMonitorOverrideEnabled(MONITOR_EVENTS_CLASS_LEVEL_OVERRIDE_KEY)
    private fun testLevelOverrideEnabled() =
        eventMonitorOverrideEnabled(MONITOR_EVENTS_TEST_LEVEL_OVERRIDE_KEY)

    /**
     * This enables event monitoring when a parameter is passed to instrumentation, to avoid having
     * to recompile the test.
     */
    private fun eventMonitorOverrideEnabled(key: String): Boolean {
        val args = InstrumentationRegistry.getArguments()
        val override = args.getString(key, "false").toBoolean()
        if (override) {
            Log.d(TAG, "event monitoring enabled by instrumentation parameter $key")
        }
        return override
    }

    companion object {
        private val TAG = "EventMonitorRule"
        private val MONITOR_EVENTS_CLASS_LEVEL_OVERRIDE_KEY =
            "monitor-events-always-enabled-class-level"
        private val MONITOR_EVENTS_TEST_LEVEL_OVERRIDE_KEY =
            "monitor-events-always-enabled-test-level"
    }
}
