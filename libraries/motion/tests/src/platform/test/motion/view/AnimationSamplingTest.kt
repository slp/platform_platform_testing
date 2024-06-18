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

package platform.test.motion.view

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Correspondence.tolerance
import com.google.common.truth.IterableSubject
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import platform.test.motion.view.AnimationSampling.Companion.evenlySampled

@RunWith(AndroidJUnit4::class)
class AnimationSamplingTest {

    @Test
    fun evenlySampled_1_inMiddle() {
        val subject = evenlySampled(1, sampleAtStart = false, sampleAtEnd = false)
        assertThat(subject.sampleAt).allowTolerance().containsExactly(1 / 2f).inOrder()
    }

    @Test
    fun evenlySampled_1_atStart() {
        val subject = evenlySampled(1, sampleAtStart = true, sampleAtEnd = false)
        assertThat(subject.sampleAt).allowTolerance().containsExactly(0f).inOrder()
    }

    @Test
    fun evenlySampled_1_atEnd() {
        val subject = evenlySampled(1, sampleAtStart = false, sampleAtEnd = true)
        assertThat(subject.sampleAt).allowTolerance().containsExactly(1f).inOrder()
    }

    @Test
    fun evenlySampled_1_atStartAndEnd_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            evenlySampled(1, sampleAtStart = true, sampleAtEnd = true)
        }
    }

    @Test
    fun evenlySampled_2_inMiddle() {
        val subject = evenlySampled(2, sampleAtStart = false, sampleAtEnd = false)
        assertThat(subject.sampleAt).allowTolerance().containsExactly(1 / 3f, 2 / 3f).inOrder()
    }

    @Test
    fun evenlySampled_2_atStart() {
        val subject = evenlySampled(2, sampleAtStart = true, sampleAtEnd = false)
        assertThat(subject.sampleAt).allowTolerance().containsExactly(0f, 1 / 2f).inOrder()
    }

    @Test
    fun evenlySampled_2_atEnd() {
        val subject = evenlySampled(2, sampleAtStart = false, sampleAtEnd = true)
        assertThat(subject.sampleAt).allowTolerance().containsExactly(1 / 2f, 1f).inOrder()
    }

    @Test
    fun evenlySampled_2_atStartAndEnd() {
        val subject = evenlySampled(2, sampleAtStart = true, sampleAtEnd = true)
        assertThat(subject.sampleAt).allowTolerance().containsExactly(0f, 1f).inOrder()
    }
}

private fun IterableSubject.allowTolerance() = comparingElementsUsing(tolerance(1.0e-10))
