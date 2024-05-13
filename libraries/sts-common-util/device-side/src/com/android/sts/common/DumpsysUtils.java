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

import static com.android.compatibility.common.util.SystemUtil.runShellCommand;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Util to parse dumpsys */
public class DumpsysUtils {

    /**
     * Fetch dumpsys for the service.
     *
     * @param command the arguments {@link String} to filter output.
     * @return the raw output.
     */
    public static String getRawDumpsys(String command) {
        // Argument validation
        if (command == null) {
            throw new IllegalArgumentException("Command should not be null");
        }

        // Run dumpsys command
        return runShellCommand(String.format("dumpsys %s", command)).trim();
    }

    /**
     * Parses the dumpsys output for the specified service using the provided pattern.
     *
     * @param service the service name {@link String} to check status.
     * @param args the argument {@link Map} to filter output.
     * @param pattern the pattern {@link Pattern} to parse the dumpsys output.
     * @return the required value.
     */
    public static Matcher getParsedDumpsys(
            String service, Map<String, String> args, Pattern pattern) {
        // Argument validation
        if (service == null) {
            throw new IllegalArgumentException("Service should not be null");
        }

        // Construct arguments
        String arguments =
                args == null
                        ? ""
                        : args.entrySet().stream()
                                .map(arg -> String.format("%s %s", arg.getKey(), arg.getValue()))
                                .collect(Collectors.joining(" "));

        // Get raw dumpsys output
        String rawOutput = getRawDumpsys(String.format("%s %s", service, arguments));

        // Trim and concatenate lines
        rawOutput =
                String.join(
                        " ",
                        Arrays.stream(rawOutput.split("\n"))
                                .map(e -> e.trim())
                                .toArray(String[]::new));

        // Compile the pattern and match it against the output
        return pattern.matcher(rawOutput);
    }

    /**
     * Parse the dumpsys for the service using pattern.
     *
     * @param service the service name {@link String} to check status.
     * @param pattern the pattern {@link Pattern} to parse the dumpsys output.
     * @return the required value.
     */
    public static Matcher getParsedDumpsys(String service, Pattern pattern) {
        return getParsedDumpsys(service, null /* args */, pattern);
    }

    /**
     * Check if output contains 'mResumed=true' for the activity.
     *
     * @param activityName the activity name {@link String} to check status.
     * @return true, if 'mResumed=true' present, else false.
     */
    public static boolean isActivityResumed(String activityName) {
        // Calls 'getParsedDumpsys' method to retrieve the matcher object for the activity
        // Returns true if 'mResumed=true' is found in the dumpsys output, otherwise false
        return getParsedDumpsys(
                        "activity" /* service */,
                        Map.of("-a", activityName) /* args */,
                        Pattern.compile("mResumed=true" /* regex */, Pattern.CASE_INSENSITIVE))
                .find();
    }

    /**
     * Check if output contains 'reportedVisible=true' for the activity.
     *
     * @param activityName the activity name {@link String} to check status.
     * @return true, if 'reportedVisible=true' present, else false.
     */
    public static boolean isActivityVisible(String activityName) {
        // Calls 'getParsedDumpsys' method to retrieve the matcher object for the activity
        // Returns true if 'reportedVisible=true' is found in the dumpsys output, otherwise false
        return getParsedDumpsys(
                        "activity" /* service */,
                        Map.of("-a", activityName) /* args */,
                        Pattern.compile(
                                "reportedVisible=true" /* regex */, Pattern.CASE_INSENSITIVE))
                .find();
    }

    /**
     * Check if both 'isActivityResumed' and 'isActivityVisible' return true for the activity.
     *
     * @param activityName the activity name {@link String} to check status.
     * @return true if both 'isActivityResumed' and 'isActivityVisible' return true, else false.
     */
    public static boolean isActivityLaunched(String activityName) {
        // Calls 'isActivityResumed' and 'isActivityVisible' methods to check activity launch
        // Returns true if both 'isActivityResumed' and 'isActivityVisible' return true,
        // otherwise false
        return isActivityResumed(activityName) && isActivityVisible(activityName);
    }
}
