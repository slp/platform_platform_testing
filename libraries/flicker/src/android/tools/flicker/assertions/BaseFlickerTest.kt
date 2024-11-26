/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.tools.flicker.assertions

import android.tools.flicker.subject.events.EventLogSubject
import android.tools.flicker.subject.layers.LayerTraceEntrySubject
import android.tools.flicker.subject.layers.LayersTraceSubject
import android.tools.flicker.subject.region.RegionTraceSubject
import android.tools.flicker.subject.wm.WindowManagerStateSubject
import android.tools.flicker.subject.wm.WindowManagerTraceSubject
import android.tools.traces.component.IComponentMatcher
import android.tools.withTracing

abstract class BaseFlickerTest(
    private val assertionFactory: AssertionFactory = AssertionFactory()
) : FlickerTest {
    protected abstract fun doProcess(assertion: AssertionData)

    override fun assertWmStart(assertion: WindowManagerStateSubject.() -> Unit) {
        withTracing("assertWmStart") {
            val assertionData = assertionFactory.createWmStartAssertion(assertion)
            doProcess(assertionData)
        }
    }

    override fun assertWmEnd(assertion: WindowManagerStateSubject.() -> Unit) {
        withTracing("assertWmEnd") {
            val assertionData = assertionFactory.createWmEndAssertion(assertion)
            doProcess(assertionData)
        }
    }

    override fun assertWm(assertion: WindowManagerTraceSubject.() -> Unit) {
        withTracing("assertWm") {
            val assertionData = assertionFactory.createWmAssertion(assertion)
            doProcess(assertionData)
        }
    }

    override fun assertWmTag(tag: String, assertion: WindowManagerStateSubject.() -> Unit) {
        withTracing("assertWmTag") {
            val assertionData = assertionFactory.createWmTagAssertion(tag, assertion)
            doProcess(assertionData)
        }
    }

    override fun assertWmVisibleRegion(
        componentMatcher: IComponentMatcher,
        assertion: RegionTraceSubject.() -> Unit,
    ) {
        withTracing("assertWmVisibleRegion") {
            val assertionData =
                assertionFactory.createWmVisibleRegionAssertion(componentMatcher, assertion)
            doProcess(assertionData)
        }
    }

    override fun assertLayersStart(assertion: LayerTraceEntrySubject.() -> Unit) {
        withTracing("assertLayersStart") {
            val assertionData = assertionFactory.createLayersStartAssertion(assertion)
            doProcess(assertionData)
        }
    }

    override fun assertLayersEnd(assertion: LayerTraceEntrySubject.() -> Unit) {
        withTracing("assertLayersEnd") {
            val assertionData = assertionFactory.createLayersEndAssertion(assertion)
            doProcess(assertionData)
        }
    }

    override fun assertLayers(assertion: LayersTraceSubject.() -> Unit) {
        withTracing("assertLayers") {
            val assertionData = assertionFactory.createLayersAssertion(assertion)
            doProcess(assertionData)
        }
    }

    override fun assertLayersTag(tag: String, assertion: LayerTraceEntrySubject.() -> Unit) {
        withTracing("assertLayersTag") {
            val assertionData = assertionFactory.createLayersTagAssertion(tag, assertion)
            doProcess(assertionData)
        }
    }

    override fun assertLayersVisibleRegion(
        componentMatcher: IComponentMatcher,
        useCompositionEngineRegionOnly: Boolean,
        assertion: RegionTraceSubject.() -> Unit,
    ) {
        withTracing("assertLayersVisibleRegion") {
            val assertionData =
                assertionFactory.createLayersVisibleRegionAssertion(
                    componentMatcher,
                    useCompositionEngineRegionOnly,
                    assertion,
                )
            doProcess(assertionData)
        }
    }

    override fun assertEventLog(assertion: EventLogSubject.() -> Unit) {
        withTracing("assertEventLog") {
            val assertionData = assertionFactory.createEventLogAssertion(assertion)
            doProcess(assertionData)
        }
    }
}
