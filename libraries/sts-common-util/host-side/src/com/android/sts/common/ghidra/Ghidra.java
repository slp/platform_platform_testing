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

package com.android.sts.common;

import static com.android.sts.common.GhidraPreparer.PROPERTY_KEY;

import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.testtype.junit4.BaseHostJUnit4Test;

/** Represents a Ghidra instance. */
public class Ghidra {
    private String ghidraPath;
    private ITestDevice device;
    private String callingClassName;

    /**
     * Constructs a new Ghidra instance with information obtained from a BaseHostJUnit4Test object.
     *
     * @param test The BaseHostJUnit4Test object containing the required information.
     */
    public Ghidra(BaseHostJUnit4Test test) throws IllegalStateException {
        this.ghidraPath = test.getTestInformation().properties().get(PROPERTY_KEY);
        if (this.ghidraPath == null) {
            throw new IllegalStateException("Path to Ghidra installation not found");
        }
        this.device = test.getDevice();
        this.callingClassName = test.getClass().getSimpleName();
    }

    /**
     * Gets the Ghidra path.
     *
     * @return The Ghidra path.
     */
    public String getGhidraPath() {
        return ghidraPath;
    }

    /**
     * Gets the device.
     *
     * @return The device.
     */
    public ITestDevice getDevice() {
        return device;
    }

    /**
     * Gets the calling class name.
     *
     * @return The calling class name.
     */
    public String getCallingClassName() {
        return callingClassName;
    }
}
