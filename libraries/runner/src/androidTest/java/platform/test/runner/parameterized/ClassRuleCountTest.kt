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

package platform.test.runner.parameterized

import com.google.common.truth.Truth.assertThat
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith

@RunWith(ParameterizedAndroidJunit4::class)
class ClassRuleCountTest(private val i: Int) {
    companion object {
        var howManyApplications = 0

        @JvmStatic
        @get:ClassRule
        val countApplicationsRule = TestRule { base, _ ->
            howManyApplications++
            base
        }

        @JvmStatic
        @Parameters(name = "N = {0}")
        fun data(): List<Array<Any>> {
            return listOf(arrayOf(0), arrayOf(1))
        }
    }

    @Test
    fun equalityWorks() {
        assertThat(i).isEqualTo(i)
        assertThat(howManyApplications).isEqualTo(1)
    }
}
