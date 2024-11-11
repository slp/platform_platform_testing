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
    private static final String GET_METRIC_NAME = "GET_PROPERTY_TIMING";
    private static final String SET_METRIC_NAME = "SET_PROPERTY_TIMING";
    private static final String FILE_NAME = "values_for_test";

    /** {@inheritDoc} */
    /**
     * Returns {@link MetricMeasurement.DataType.RAW} for metrics reported by the post processor.
     * RAW is required in order for {@link
     * com.android.tradefed.postprocessor.MetricFilePostProcessor} to aggregate the values
     */
    @Override
    protected MetricMeasurement.DataType getMetricType() {
        // Return raw metrics in order for MetricFilePostProcessor to aggregate
        return MetricMeasurement.DataType.RAW;
    }

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
            String metricName = key.contains("get") ? GET_METRIC_NAME : SET_METRIC_NAME;
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
                StringBuilder stringBuilder = new StringBuilder();
                String line = br.readLine();
                while (line != null) {
                    stringBuilder.append(line + ",");
                    line = br.readLine();
                }
                MetricMeasurement.Measurements.Builder measurement =
                        MetricMeasurement.Measurements.newBuilder()
                                .setSingleString(stringBuilder.toString());
                MetricMeasurement.Metric.Builder metric =
                        MetricMeasurement.Metric.newBuilder().setMeasurements(measurement);
                metrics.put(metricName, metric);
            } catch (IOException e) {
                LogUtil.CLog.e("Unable to open buffered reader");
                throw new RuntimeException(e);
            }
        }
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
