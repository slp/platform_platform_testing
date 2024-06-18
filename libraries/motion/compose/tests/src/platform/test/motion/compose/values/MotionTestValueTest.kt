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

package platform.test.motion.compose.values

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MotionTestValueTest {

    @get:Rule val composeRule = createComposeRule()

    @Test
    fun modifier_isIgnored() {
        composeRule.setContent { Text("foo", Modifier.motionTestValues { fail() }) }

        val semanticsNode = composeRule.onNodeWithText("foo").fetchSemanticsNode()
        assertThat(semanticsNode.config.contains(foo.semanticsPropertyKey)).isFalse()
    }

    @Test
    fun modifier_withEnableMotionTesting_isAttached() {
        composeRule.setContent {
            EnableMotionTestValueCollection {
                Text("foo", Modifier.motionTestValues { 1f exportAs foo })
            }
        }

        val semanticsNode = composeRule.onNodeWithText("foo").fetchSemanticsNode()
        assertThat(semanticsNode.config.contains(foo.semanticsPropertyKey)).isTrue()
        assertThat(semanticsNode.config[foo.semanticsPropertyKey]).isEqualTo(1f)
    }

    companion object {
        val foo = MotionTestValueKey<Float>("foo")
    }
}
