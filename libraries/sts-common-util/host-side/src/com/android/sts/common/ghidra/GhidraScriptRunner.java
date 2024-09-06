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

import static com.android.sts.common.CommandUtil.runAndCheck;
import static com.android.tradefed.util.FileUtil.chmodRWXRecursively;
import static com.android.tradefed.util.FileUtil.createNamedTempDir;
import static com.android.tradefed.util.FileUtil.recursiveDelete;

import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.util.RunUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/** class for running Ghidra scripts. */
public class GhidraScriptRunner {
    public static final long POST_SCRIPT_TIMEOUT = 90 * 1000L; // 90 seconds
    private ByteArrayOutputStream mOutputOfGhidra;
    private ITestDevice mDevice;
    private String mBinaryName;
    private String mBinaryFile;
    private String mAnalyzeHeadlessPath;
    private Optional<String> mPropertiesFileName = Optional.empty();
    private Optional<String> mPreScriptFileName = Optional.empty();
    private Optional<String> mPostScriptFileName = Optional.empty();
    private Optional<String> mPreScriptContent = Optional.empty();
    private Optional<String> mPostScriptContent = Optional.empty();
    private Map<String, String> mPropertiesFileContentsMap = Collections.emptyMap();
    private String mCallingClass;
    private boolean mEnableAnalysis = false;
    private File mPulledLibSaveFolder;

    /**
     * Constructor for GhidraScriptRunner. When using this constructor from the same class
     * concurrently, make sure to append a unique suffix to the {@code callingClass} parameter to
     * avoid conflicts.
     *
     * @param device The ITestDevice to pull the binary from.
     * @param binaryFile The binary file in device.
     * @param callingClass The name of the calling class.
     * @param propertiesFileName The file name of properties file associated with post script. eg.
     *     function_offset_post_script.properties
     * @param preScriptFileName The file name of pre script file.
     * @param postScriptFileName The file name of post script file. eg.
     *     function_offset_post_script.java
     */
    public GhidraScriptRunner(
            ITestDevice device,
            File binaryFile,
            String callingClass,
            String propertiesFileName,
            String preScriptFileName,
            String postScriptFileName,
            String analyzeHeadlessPath) {
        mDevice = device;
        mBinaryName = binaryFile.getName();
        mBinaryFile = binaryFile.toString();
        mCallingClass = callingClass;
        mAnalyzeHeadlessPath = analyzeHeadlessPath;
        mPropertiesFileName = Optional.ofNullable(propertiesFileName);
        mPreScriptFileName = Optional.ofNullable(preScriptFileName);
        mPostScriptFileName = Optional.ofNullable(postScriptFileName);
    }

    /**
     * Set analysis flag during Ghidra script execution.
     *
     * @return This GhidraScriptRunner instance with analysis enabled.
     */
    public GhidraScriptRunner enableAnalysis() {
        mEnableAnalysis = true;
        return this;
    }

    /**
     * Return ByteArrayOutputStream.
     *
     * @return mOutputOfGhidra.
     */
    public ByteArrayOutputStream getOutputStream() {
        return mOutputOfGhidra;
    }

    /**
     * Specify a post-script its properties to be executed after Ghidra analysis.
     *
     * @param contents The contents of the post-script.
     * @param propertiesContent The map of key value pairs to write in properties file
     * @return This GhidraScriptRunner instance with post-script enabled and configured.
     */
    public GhidraScriptRunner postScript(String contents, Map<String, String> propertiesContent) {
        mPostScriptContent = Optional.ofNullable(contents);

        if (!propertiesContent.isEmpty()) {
            mPropertiesFileContentsMap = propertiesContent;
        }
        return this;
    }

    /**
     * Specify a pre-script to be executed before Ghidra analysis.
     *
     * @param contents The contents of the pre-script.
     * @return This GhidraScriptRunner instance with pre-script enabled and configured.
     */
    public GhidraScriptRunner preScript(String contents) {
        mPreScriptContent = Optional.ofNullable(contents);
        return this;
    }

    /**
     * Run Ghidra with the specified options and scripts.
     *
     * @return an AutoCloseable for cleaning up temporary files after script execution.
     */
    public AutoCloseable run() throws Exception {
        return runWithTimeout(POST_SCRIPT_TIMEOUT);
    }

    /**
     * Run Ghidra with the specified options and scripts, with a timeout.
     *
     * @param timeout The timeout value in milliseconds.
     * @return an AutoCloseable for cleaning up temporary files after script execution.
     */
    public AutoCloseable runWithTimeout(long timeout) throws Exception {
        try {
            // Get the language using readelf
            String deviceSerial = mDevice.getSerialNumber().replace(":", "");
            String pulledLibSaveFolderString = mCallingClass + "_" + deviceSerial + "_files";
            String language = getLanguage(mDevice, mBinaryFile);
            mPulledLibSaveFolder =
                    createNamedTempDir(
                            Paths.get(mAnalyzeHeadlessPath).getParent().toFile(),
                            pulledLibSaveFolderString);

            // Pull binary from the device to the folder
            if (!mDevice.pullFile(
                    mBinaryFile, new File(mPulledLibSaveFolder + "/" + mBinaryName))) {
                throw new Exception("Pulling " + mBinaryFile + " was not successful");
            }

            // Create script related files and chmod rwx them
            if (!mPropertiesFileContentsMap.isEmpty() && mPropertiesFileName.isPresent()) {
                createPropertiesFile(
                        mPulledLibSaveFolder,
                        mPropertiesFileName.get(),
                        mPropertiesFileContentsMap);
            }
            if (mPreScriptContent.isPresent() && mPreScriptFileName.isPresent()) {
                createScriptFile(
                        mPulledLibSaveFolder, mPreScriptFileName.get(), mPreScriptContent.get());
            }
            if (mPostScriptContent.isPresent() && mPostScriptFileName.isPresent()) {
                createScriptFile(
                        mPulledLibSaveFolder, mPostScriptFileName.get(), mPostScriptContent.get());
            }
            if (!chmodRWXRecursively(mPulledLibSaveFolder)) {
                throw new Exception("chmodRWX failed for " + mPulledLibSaveFolder.toString());
            }

            // Analyze the pulled binary using Ghidra headless analyzer
            List<String> cmd =
                    createCommandList(
                            mAnalyzeHeadlessPath,
                            mCallingClass,
                            deviceSerial,
                            mPulledLibSaveFolder.getPath(),
                            mBinaryName,
                            mPreScriptFileName.isPresent() ? mPreScriptFileName.get() : "",
                            mPostScriptFileName.isPresent() ? mPostScriptFileName.get() : "",
                            language,
                            mEnableAnalysis);
            mOutputOfGhidra = new ByteArrayOutputStream();
            Process ghidraProcess = RunUtil.getDefault().runCmdInBackground(cmd, mOutputOfGhidra);
            if (!ghidraProcess.isAlive()) {
                throw new Exception("Ghidra process died. Output:" + mOutputOfGhidra);
            }

            if (mOutputOfGhidra.toString("UTF-8").contains("Enter path to JDK home directory")) {
                throw new Exception(
                        "JDK 17+ (64-bit) not found in the system PATH. Please add it to your"
                                + " PATH environment variable.");
            }
            return () -> recursiveDelete(mPulledLibSaveFolder);
        } catch (Exception e) {
            recursiveDelete(mPulledLibSaveFolder);
            throw e;
        }
    }

    /**
     * Creates a ghidra script file with the specified content in the given folder.
     *
     * @param folder The folder where the script file will be created.
     * @param fileName The name of the script file.
     * @param content The content to be written into the script file.
     */
    private static void createScriptFile(File folder, String fileName, String content)
            throws Exception {
        try (FileWriter fileWriter = new FileWriter(new File(folder, fileName))) {
            fileWriter.write(content);
        }
    }

    /**
     * Creates a ghidra script properties file with the specified content in the given folder.
     *
     * @param folder The folder where the script file will be created.
     * @param fileName The name of the script file.
     * @param map The map of key value pairs to be written into the properties file.
     */
    private static void createPropertiesFile(File folder, String fileName, Map<String, String> map)
            throws Exception {
        File propertiesFile = new File(folder, fileName);
        try (FileWriter fileWriter = new FileWriter(propertiesFile);
                FileInputStream fileInputStream = new FileInputStream(propertiesFile)) {
            propertiesFile.createNewFile();

            Properties properties = new Properties();
            if (propertiesFile.exists()) {
                properties.load(fileInputStream);
            } else {
                throw new Exception("Unable to create ghidra script properties file");
            }

            // Populate properties file
            map.forEach(properties::setProperty);
            properties.store(fileWriter, "");
        }
    }

    /**
     * Uses the value under 'Machine' from the output of 'readelf -h' command on the specified
     * binary file to determine the '-processor' parameter (language id) to be passed when invoking
     * ghidra.
     *
     * <p>For e.g. readelf -h <binary_name>
     *
     * <p>ELF Header: Magic: 7f 45 4c 46 02 01 01 00 00 00 00 00 00 00 00 00
     *
     * <p>[...]
     *
     * <p>Machine: arm64 <--- ABI or processor type
     *
     * <p>Version: 0x1
     *
     * <p>[...]
     *
     * <p>The language id can be found in processor-specific .ldefs files located at:
     * https://github.com/NationalSecurityAgency/ghidra/blob/master/Ghidra/Processors/
     * <proc_name>/data/languages/<proc_name>.ldefs
     *
     * <p>where 'proc_name' can be AARCH64, ARM, x86 which are the only ABIs that Android currently
     * supports as per https://developer.android.com/ndk/guides/abis
     *
     * <p>For e.g. Following code snippet from AARCH64.ldefs shows the language 'id' of 'AARCH64'
     * machine type ie 'arm64'.
     *
     * <p><language processor="AARCH64" endian="little" size="64" variant="v8A" version="1.6"
     * slafile="AARCH64.sla" processorspec="AARCH64.pspec" manualindexfile="../manuals/AARCH64.idx"
     * id="AARCH64:LE:64:v8A"> <--- 'id' denotes the language id.
     *
     * <p>TODO: Utilize ghidra pre script for setting language automatically.
     *
     * @param device The ITestDevice representing the testing device.
     * @param binaryFile The path to the binary file.
     * @return The language of the binary in the format "ARCH:ENDIAN:BITS:VARIANT"
     */
    private static String getLanguage(ITestDevice device, String binaryFile) throws Exception {
        String language =
                runAndCheck(device, "readelf -h " + binaryFile + " | grep Machine")
                        .getStdout()
                        .trim()
                        .split(":\\s*")[1]
                        .trim();
        switch (language) {
            case "arm":
                return "ARM:LE:32:v8";
            case "arm64":
                return "AARCH64:LE:64:v8A";
            case "386":
                return "x86:LE:32:default";
            case "x86-64":
                return "x86:LE:64:default";
            case "riscv":
                return "RISCV:LE:64:RV64GC";
            default:
                throw new Exception("Unsupported Machine: " + language);
        }
    }

    /**
     * Creates a list of command-line arguments for invoking Ghidra's analyzeHeadless tool.
     *
     * @param ghidraBinaryLocation The analyzerHeadless location.
     * @param callingClass The name of the calling class.
     * @param deviceSerialNumber The serial number of the target device.
     * @param tempFileName The temporary folder name in which target binary is pulled.
     * @param binaryName The name of the binary file to run analyzeHeadless on.
     * @param preScriptFileName The file name of the pre script.
     * @param postScriptFileName The file name of the post script.
     * @param lang The processor language for analysis. eg ARM:LE:32:v8
     * @param analysis Flag indicating whether analysis should be performed.
     * @return A list of command-line arguments for Ghidra analyzeHeadless tool.
     */
    private static List<String> createCommandList(
            String ghidraBinaryLocation,
            String callingClass,
            String deviceSerialNumber,
            String tempFileName,
            String binaryName,
            String preScriptFileName,
            String postScriptFileName,
            String lang,
            boolean analysis) {
        boolean preScript = !preScriptFileName.isEmpty();
        boolean postScript = !postScriptFileName.isEmpty();
        return List.of(
                ghidraBinaryLocation,
                tempFileName,
                callingClass + "_ghidra_project_" + deviceSerialNumber,
                "-import",
                tempFileName + "/" + binaryName,
                "-scriptPath",
                tempFileName,
                postScript ? "-postScript" : "",
                postScript ? postScriptFileName : "",
                preScript ? "-preScript" : "",
                preScript ? preScriptFileName : "",
                "-processor",
                lang,
                analysis ? "" : "-noanalysis",
                "-deleteProject");
    }
}
