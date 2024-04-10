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

import static com.android.sts.common.GhidraScriptRunner.POST_SCRIPT_TIMEOUT;

import static java.util.stream.Collectors.joining;

import com.android.tradefed.device.ITestDevice;

import com.google.common.collect.ImmutableMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A utility class containing constants and functions for a Ghidra post-script (located at
 * res/ghidra-scripts/FunctionOffsetPostScript.java) used to get function offsets
 */
public class GhidraFunctionOffsets {
    public static final String POST_SCRIPT_CLASS_NAME = "FunctionOffsetPostScript";
    public static final String POST_SCRIPT_CONTENT_RESOURCE_PATH =
            "ghidra-scripts/" + POST_SCRIPT_CLASS_NAME + ".java";

    /**
     * Converts the output of {@code getFunctionOffsets} to a string with space separated offsets.
     *
     * @param ghidra An instance of Ghidra.
     * @param binaryFile The binary file in device.
     * @param functions The list of function names.
     * @return A string containing space separated function offsets.
     */
    public static String getFunctionOffsetsAsCmdLineArgs(
            Ghidra ghidra, File binaryFile, List<String> functions) throws Exception {
        return String.join(
                " ",
                getFunctionOffsets(ghidra, binaryFile, functions).stream()
                        .map(BigInteger::toString)
                        .toArray(String[]::new));
    }

    /**
     * Retrieves the function offsets from the given binary.
     *
     * @param ghidra An instance of Ghidra.
     * @param binaryFile The binary file in device.
     * @param functions The list of function names.
     * @return A list of BigIntegers containing function offsets in the same order as @param
     *     functions.
     */
    public static List<BigInteger> getFunctionOffsets(
            Ghidra ghidra, File binaryFile, List<String> functions) throws Exception {
        final String ghidraPath = ghidra.getGhidraPath();
        final String callingClass = ghidra.getCallingClassName();
        final ITestDevice device = ghidra.getDevice();

        // Set up a server socket to listen for output from post script
        final Map<String, List<BigInteger>> mapOfResults = new HashMap<String, List<BigInteger>>();
        final Semaphore outputReceived = new Semaphore(0);
        final ServerSocket serverSocket = new ServerSocket(0);
        serverSocket.setReuseAddress(true /* on */);
        serverSocket.setSoTimeout((int) POST_SCRIPT_TIMEOUT);
        int port = serverSocket.getLocalPort();
        Thread clientThread =
                new Thread(
                        () -> {
                            try (Socket clientSocket =
                                            serverSocket.accept(); // blocks till connected
                                    ObjectInputStream inputStream =
                                            new ObjectInputStream(
                                                    clientSocket.getInputStream()); ) {
                                mapOfResults.put(
                                        callingClass, (List<BigInteger>) inputStream.readObject());
                            } catch (Exception e) {
                                mapOfResults.put(callingClass, null);
                            } finally {
                                outputReceived.release();
                            }
                        });

        GhidraScriptRunner ghidraScriptRunner =
                new GhidraScriptRunner(
                                device,
                                binaryFile,
                                callingClass,
                                POST_SCRIPT_CLASS_NAME + ".properties",
                                null,
                                POST_SCRIPT_CLASS_NAME + ".java",
                                ghidraPath)
                        .postScript(
                                readResource(POST_SCRIPT_CONTENT_RESOURCE_PATH),
                                new HashMap<String, String>(
                                        ImmutableMap.of(
                                                "functionNames",
                                                String.join(" ", functions),
                                                "port",
                                                String.valueOf(port))));
        try (AutoCloseable ghidraScriptRunnerAc = ghidraScriptRunner.run()) {
            clientThread.start();
            if (!outputReceived.tryAcquire(POST_SCRIPT_TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new TimeoutException(
                        "FunctionOffsetPostScript timed out. Output of Ghidra: "
                                + ghidraScriptRunner.getOutputStream().toString("UTF-8"));
            }
            return mapOfResults.get(callingClass);
        }
    }

    private static String readResource(String fullResourceName) throws Exception {
        try (InputStream in =
                GhidraFunctionOffsets.class
                        .getClassLoader()
                        .getResourceAsStream(fullResourceName)) {
            return new BufferedReader(new InputStreamReader(in))
                    .lines()
                    .collect(joining(System.lineSeparator()));
        }
    }
}
