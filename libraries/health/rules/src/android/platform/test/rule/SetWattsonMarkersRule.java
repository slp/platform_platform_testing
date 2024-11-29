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
package android.platform.test.rule;

import androidx.test.platform.app.InstrumentationRegistry;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.runner.Description;
import org.junit.runners.model.InitializationError;

/**
 * Rule to insert Wattson start and stop markers around a test
 */
public class SetWattsonMarkersRule extends TestWatcher {
    public SetWattsonMarkersRule() {}

    @Override
    protected void starting(Description description) {
        writeTraceMarker("I|0|wattson_start");
    }

    @Override
    protected void finished(Description description) {
        writeTraceMarker("I|0|wattson_stop");
    }

    private void writeTraceMarker(String marker) {
        // Create a temporary file which contains the markers command.
        // Do this because we cannot write to sysfs directly, as executeShellCommand parses the '>'
        // character as a literal.
        File tempDir = InstrumentationRegistry.getInstrumentation().getContext().getCacheDir();
        File tempFile = null;
        try {
            tempFile = File.createTempFile("echo_marker_script", ".sh", tempDir);
            tempFile.setWritable(true);
            tempFile.setExecutable(true, /*ownersOnly*/false);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile.toString()))) {
                writer.write(String.format("echo '%s' > /sys/kernel/tracing/trace_marker", marker));
            }
            executeShellCommand(tempFile.toString());
        } catch (IOException e) {
            throw new AssertionError (e);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
}
