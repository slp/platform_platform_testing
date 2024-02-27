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

import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Rect
import android.platform.uiautomator_helpers.EventMonitorRule.MonitorEvents
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityRecord
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Accessibility event monitor, to log accessibility events using UIAutomationEventListener.
 *
 * NOTE: add the @MonitorEvents annotation and EventMonitorRule to record events in your test.
 *
 * This tracks where events actually went in your test by logging the AccessibilityNodeInfo of the
 * view which received the event. If the test is flaky, the EventMonitor output can be compared
 * between successful and failed tests.
 *
 * In your test, add the test rule:
 *
 * @Rule public final EventMonitorRule mEventMonitorRule = new EventMonitorRule();
 *   android.platform.uiautomator_helpers.EventMonitorRule
 *
 * Then the annotation: import android.platform.uiautomator_helpers.EventMonitorRule import
 * android.platform.uiautomator_helpers.EventMonitorRule.MonitorEvents
 *
 * and before the test class or test method, add the annotation:
 *
 * @MonitorEvents
 */
object EventMonitor {
    private const val TAG = "EventMonitor"
    private var originalInfo: AccessibilityServiceInfo? = null
    private var eventMask: Int = MonitorEvents.DEFAULT_EVENT_MASK
	private val DEFAULT_LOGGER: (AccessibilityEvent) -> Unit = { event ->
        Log.d(TAG, eventToString(event, event.getSource()))
    }

    /**
     * Log accessibility events using an accessibility event listener.
     *
     * @param eventMask combination from [AccessibilityEvent].TYPE_* EventMasks
     * @param logger logger lambda function, takes an AccessibilityEvent
     */
    fun start(
        eventMask: Int = MonitorEvents.DEFAULT_EVENT_MASK,
        logger: (event: AccessibilityEvent) -> Unit = DEFAULT_LOGGER
    ) {
        this.eventMask = eventMask
        Log.d(TAG, "adding accessibility event listener")
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation

        // NOTE: this replaces the AccessibilityEventListener. Internally, QueryController.java
        // uses it to track the last activity name and last traversed text, but those are used
        // for accessibility, in Tests
        // TODO (b/310679067): replace this with a method which gets the current listener and
        // adds this event listener.
        Log.w(
            TAG,
            "SetOnAccessibilityEventListener(). This may replace the existing listener " +
                "and will cause UiDevice.getLastTraversedText() and getCurrentActivityName()" +
                "to not work anymore."
        )
        setServiceInfo()
        uiAutomation.setOnAccessibilityEventListener { event ->
            if ((event.getEventType() and eventMask) != 0x0) {
                logger(event)
            }
        }
    }

    /**
     * Generate an event string. [AccessibilityEvent.toString()] displays fields not relevant for
     * event comparison. We generate the output on a single line to simplify output diffs.
     *
     * @param event The accessibility event from the listener
     * @param nodeInfo Extra node info to extract the view ID.
     * @return string with event information, space-delimited.
     */
    private fun eventToString(event: AccessibilityEvent, nodeInfo: AccessibilityNodeInfo?): String {
        val stringNodeInfo = stringNodeInfo(nodeInfo)
        val stringRecordInfo = stringifyRecords(event)
        return """EventType: ${AccessibilityEvent.eventTypeToString(event.getEventType())}
            EventTime: ${event.getEventTime()}
            PackageName: ${event.getPackageName()}
            Action: ${if (event.getAction() != 0) "${event.getAction()}" else "Unknown"}
            NodeInfo: $stringNodeInfo
            Records: $stringRecordInfo
            """
    }

    private fun stringNodeInfo(nodeInfo: AccessibilityNodeInfo?): String {
        if (nodeInfo != null) {
            val boundsInScreen = Rect()
            val boundsInWindow = Rect()
            nodeInfo.getBoundsInScreen(boundsInScreen)
            nodeInfo.getBoundsInWindow(boundsInWindow)
            return """
                ViewID: ${nodeInfo.getViewIdResourceName()}
                BoundsInScreen: $boundsInScreen
                BoundsInWindow: $boundsInWindow
                """
        } else {
            return "No AccessibilityNodeInfo available"
        }
    }

    private fun stringifyRecords(event: AccessibilityEvent) = buildString {
        (0 until event.recordCount).forEach { i -> append("${stringRecord(event.getRecord(i))}\n") }
    }

    private fun stringRecord(record: AccessibilityRecord): String {
        return """ClassName: ${record.getClassName()}
        Text: ${record.getText()}
        ContentDescription: ${record.getContentDescription()}
        BeforeText: ${record.getBeforeText()}
        Enabled: ${record.isEnabled()}
        Checked: ${record.isChecked()}
        """
    }

    /** Set the service to report view ids and flagged events. */
    private fun setServiceInfo() {
        // Set the service to report view ids and all events.
        val uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation()
        val info = uiAutomation.getServiceInfo()
        originalInfo = info
        info.flags =
            info.flags or
                (AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_SEND_MOTION_EVENTS or
                    AccessibilityServiceInfo.FLAG_SERVICE_HANDLES_DOUBLE_TAP or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS)

        // The internal AccessibilityEventListener used by QueryController uses these flags, so
        // we have to set them, or we'll break it.
        info.eventTypes =
            eventMask or
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
        uiAutomation.serviceInfo = info
    }

    /** Clear the accessibility listener, and restore the service info to its original state. */
    fun stop() {
        Log.d(TAG, "stop")
        val uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation()
        uiAutomation.setOnAccessibilityEventListener(null)
        originalInfo?.let { it -> uiAutomation.setServiceInfo(it) }
            ?: error("EventMonitor service was stopped but not started")
    }
}
