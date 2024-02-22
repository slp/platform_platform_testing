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

package android.tools.collectors;

import android.content.Context;
import android.device.collectors.PerfettoListener;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.helpers.PerfettoHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * A {@link PerfettoListener} that captures the perfetto trace for UI traces during each test method
 * and save the perfetto trace files under
 * <root>/<test_name>/PerfettoListener/<test_name>-<invocation_count>.perfetto-trace
 */
public class DefaultUITraceListener extends PerfettoListener {
    private static final String LOG_TAG = "UITraceListener";
    private static final String DEFAULT_TEXT_CONFIG_FILE = "trace_config.textproto";

    @SuppressWarnings("unused")
    public DefaultUITraceListener() {
        super();
    }

    /**
     * Constructor to simulate receiving the instrumentation arguments. Should not be used except
     * for testing.
     */
    @VisibleForTesting
    DefaultUITraceListener(Bundle args, PerfettoHelper helper, Map<String, Integer> invocationMap) {
        super(args, helper, invocationMap);
    }

    @Override
    protected void startPerfettoTracing() {
        Log.v(LOG_TAG, "startPerfettoTracing");
        // Text proto config to be passed to perfetto via output stream
        String protoConfig;
        try {
            protoConfig = readDefaultConfig();
        } catch (IOException e) {
            throw new RuntimeException("Unable to read config asset", e);
        }
        boolean success = getPerfettoHelper().startCollecting(protoConfig);
        if (!success) {
            Log.e(LOG_TAG, "Perfetto did not start successfully.");
        }

        setPerfettoStartSuccess(success);
    }

    @Override
    protected String getPerfettoFilePrefix() {
        return "uiTrace_";
    }

    @Override
    public void setupAdditionalArgs() {
        Bundle args = getArgsBundle();
        args.putBoolean(PERFETTO_CONFIG_TEXT_PROTO, true);
        super.setupAdditionalArgs();
    }

    private String readDefaultConfig() throws IOException {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();

        try (InputStream inputStream = context.getAssets().open(DEFAULT_TEXT_CONFIG_FILE)) {
            Log.v(LOG_TAG, "context.assets");
            return new String(inputStream.readAllBytes());
        }
    }
}
