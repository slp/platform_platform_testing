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

package android.platform.test.stress.postprocessor;

import com.android.tradefed.log.LogUtil;
import com.android.tradefed.metrics.proto.MetricMeasurement;
import com.android.tradefed.postprocessor.BasePostProcessor;
import com.android.tradefed.result.LogFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class CarPropertyManagerStressTestLogPostProcessor extends BasePostProcessor {
    private static final String METRIC_NAME = "GET_PROPERTY_TIMING";
    private static final String FILE_NAME = "values_for_test";

    /** {@inheritDoc} */
    @Override
    public Map<String, MetricMeasurement.Metric.Builder> processRunMetricsAndLogs(
            HashMap<String, MetricMeasurement.Metric> rawMetrics, Map<String, LogFile> runLogs) {
        Map<String, MetricMeasurement.Metric.Builder> metrics = new HashMap<>();
        for (String key : runLogs.keySet()) {
            LogUtil.CLog.i("Reading file: %s", key);
            if (!key.contains(FILE_NAME)) {
                continue;
            }
            BufferedReader br;
            try {
                File file = new File(runLogs.get(key).getPath());
                if (isGZipped(file)) {
                    LogUtil.CLog.i("File %s is gzipped", file);
                    br =
                            new BufferedReader(
                                    new InputStreamReader(
                                            new GZIPInputStream(new FileInputStream(file))));
                } else {
                    LogUtil.CLog.i("File %s is not gzipped", file);
                    br = new BufferedReader(new FileReader(file));
                }
                String line = br.readLine();
                while (line != null) {
                    MetricMeasurement.Measurements.Builder measurement =
                            MetricMeasurement.Measurements.newBuilder()
                                    .setSingleInt(Long.parseLong(line));
                    MetricMeasurement.Metric.Builder metric =
                            MetricMeasurement.Metric.newBuilder().setMeasurements(measurement);
                    metrics.put(METRIC_NAME + "-" + line, metric);

                    line = br.readLine();
                }
            } catch (IOException e) {
                LogUtil.CLog.e("Unable to open buffered reader");
                throw new RuntimeException(e);
            }
        }
        LogUtil.CLog.i("returning metrics %s", metrics);
        return metrics;
    }

    /**
     * Checks if a file is gzipped.
     *
     * @param f File to check if its is gzipped
     * @return True if it is gzipped false otherwise
     */
    public static boolean isGZipped(File f) {
        int magic = 0;
        try {
            RandomAccessFile raf = new RandomAccessFile(f, "r");
            magic = (raf.read() & 0xff) | ((raf.read() << 8) & 0xff00);
            raf.close();
        } catch (Throwable e) {
            e.printStackTrace(System.err);
        }
        return magic == GZIPInputStream.GZIP_MAGIC;
    }
}
