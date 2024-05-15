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

package android.platform.test.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated class uses flags from the provided classes.
 *
 * <p>In practice this tells {@code SetFlagsRule.ClassRule} to watch the all flags in the given
 * classes to ensure they are not read before being set.
 *
 * <p>This annotation is redundant if any flag in the provided class is already provided to an
 * {@link EnableFlags} or {@link DisableFlags} annotation on the same class or any of its tests.
 *
 * <p>This annotation is used by {@code SetFlagsRule.ClassRule} to determine which classes will be
 * watched during class initialization, because only watched classes will be able to be enabled or
 * disabled. The annotations {@link EnableFlags} or {@link DisableFlags} provide enough information
 * on their own, but Parameterized tests and tests which imperatively set flags do not provide
 * enough information via the junit Description to be watched, so this annotation is used to provide
 * that information.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface UsesFlags {
    /** The list of the `Flags` classes to watch. */
    Class<?>[] value();
}
