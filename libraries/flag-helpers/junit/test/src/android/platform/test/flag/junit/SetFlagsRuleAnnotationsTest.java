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

package android.platform.test.flag.junit;

import static android.platform.test.flag.junit.SetFlagsRule.DefaultInitValueType.DEVICE_DEFAULT;
import static android.platform.test.flag.junit.SetFlagsRule.DefaultInitValueType.NULL_DEFAULT;

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@code SetFlagsRule} being used with annotations. */
@RunWith(JUnit4.class)
public final class SetFlagsRuleAnnotationsTest extends SetFlagsRuleTestCommon {

    @Override
    protected Helper makeNullDefaultHelper() {
        return new Helper(new SetFlagsRule(NULL_DEFAULT));
    }

    @Override
    protected Helper makeParameterizedHelper(FlagsParameterization params) {
        return new Helper(new SetFlagsRule(DEVICE_DEFAULT, params));
    }
}
