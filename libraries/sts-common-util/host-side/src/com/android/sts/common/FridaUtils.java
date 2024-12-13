/*
 * Copyright (C) 2022 The Android Open Source Project
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
import static com.android.sts.common.SystemUtil.poll;
import static com.android.tradefed.util.FileUtil.createNamedTempDir;

import static java.util.stream.Collectors.toList;

import com.android.compatibility.common.tradefed.build.CompatibilityBuildHelper;
import com.android.sts.common.GitHubUtils.GitHubRepo;
import com.android.sts.common.util.FridaUtilsBusinessLogicHandler;
import com.android.tradefed.build.BuildRetrievalError;
import com.android.tradefed.build.FileDownloadCache;
import com.android.tradefed.build.FileDownloadCacheFactory;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.build.IFileDownloader;
import com.android.tradefed.config.Option;
import com.android.tradefed.config.OptionClass;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.invoker.TestInformation;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.targetprep.BaseTargetPreparer;
import com.android.tradefed.targetprep.TargetSetupError;
import com.android.tradefed.util.FileUtil;
import com.android.tradefed.util.RunUtil;

import com.google.gson.JsonParseException;

import org.tukaani.xz.XZInputStream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** AutoCloseable that downloads and push frida and scripts to device and cleans up when done */
@OptionClass(alias = "frida-preparer")
public class FridaUtils extends BaseTargetPreparer implements AutoCloseable {
    private static final Object LOCK_SETUP = new Object(); // Locks setUpFrida
    private static final String PRODUCT_CPU_ABI_KEY = "ro.product.cpu.abi";
    private static final String PRODUCT_CPU_ABILIST_KEY = "ro.product.cpu.abilist";
    private static final String FRIDA_PACKAGE = "frida-inject";
    private static final String FRIDA_OS = "android";
    private static final String TMP_PATH = "/data/local/tmp/";
    private static final String FRIDA_OWNER_AND_REPO = "frida";
    private static final String VERSION_THRESHOLD = "16.4.8";

    // Default value as per fridaAssetTemplate.gcl
    private static final String FRIDA_FILE_NAME_TEMPLATE = "{0}-{1}-{2}-{3}.xz";

    private static final Map<String, FridaUtils> DEVICE_FRIDA_UTILS_MAP = new HashMap<>();

    private List<String> mFridaFiles = new ArrayList<>();
    private String mFridaAbi;
    private ITestDevice mDevice;
    private CompatibilityBuildHelper mBuildHelper;
    private String mRemoteFridaExeName;
    private String mFridaVersion =
            "version"; // Hardcoding the Frida version to avoid confusion with version naming during
    // manual downloads.
    private Map<Integer, Integer> mTargetPidsToFridaPids = new HashMap<>();

    @Option(name = "frida-url", description = "Custom url for frida-inject xz download.")
    private String mCustomFridaUrl = null;

    @Override
    public void setUp(TestInformation testInformation) throws TargetSetupError {
        ITestDevice device = testInformation.getDevice();
        String deviceSerialNum = device.getSerialNumber();
        try {
            if (!DEVICE_FRIDA_UTILS_MAP.containsKey(deviceSerialNum)) {
                setUpFrida(device, testInformation.getBuildInfo());
                DEVICE_FRIDA_UTILS_MAP.put(deviceSerialNum, this);
            }
        } catch (Exception e) {
            throw new TargetSetupError(
                    "Set up failed for device: " + deviceSerialNum,
                    e,
                    device.getDeviceDescriptor(),
                    false /* deviceSide */);
        }
    }

    private void setUpFrida(ITestDevice device, IBuildInfo buildInfo)
            throws DeviceNotAvailableException, UnsupportedOperationException, IOException,
                    URISyntaxException, BuildRetrievalError, TargetSetupError {
        synchronized (LOCK_SETUP) {
            this.mDevice = device;
            this.mBuildHelper = new CompatibilityBuildHelper(buildInfo);
            String fridaUrl = null;

            // Figure out which version we should be using
            Optional<String> releaseTagName = FridaUtilsBusinessLogicHandler.getFridaVersion();

            // Figure out which Frida arch we should be using for our device
            mFridaAbi = getFridaAbiFor(device);
            String fridaExeName =
                    String.format("%s-%s-%s-%s", FRIDA_PACKAGE, mFridaVersion, FRIDA_OS, mFridaAbi);

            // Preference order for sourcing frida binary:
            // pre-existing or manually downloaded > custom url > GitHub.
            File localFridaExe = null;
            try {
                // Check for an existing Frida binary.
                localFridaExe = mBuildHelper.getTestFile(fridaExeName);
                CLog.d("%s found at %s", fridaExeName, localFridaExe.getAbsolutePath());

                // Throw if the version threshold is not met
                pushAndCheckVersion(localFridaExe);
            } catch (Exception e) {
                try {
                    CLog.d("Downloading Frida due to Exception: %s", e);

                    // Fetch Frida download url from GitHub if a custom URL is not provided.
                    fridaUrl =
                            mCustomFridaUrl != null
                                    ? mCustomFridaUrl
                                    : getFridaDownloadUrl(releaseTagName);
                    CLog.d(
                            "Downloading Frida from %s url = %s",
                            mCustomFridaUrl != null ? "custom" : "latest", fridaUrl);
                    localFridaExe = new File(mBuildHelper.getTestsDir(), fridaExeName);
                    FileDownloadCache fileDownloadCache =
                            FileDownloadCacheFactory.getInstance()
                                    .getCache(createNamedTempDir("frida_cache"));
                    fileDownloadCache.fetchRemoteFile(
                            new FridaFileDownloader(), fridaUrl, localFridaExe);

                    // Throw if the version threshold is not met
                    pushAndCheckVersion(localFridaExe);
                } catch (Exception e2) {
                    // In case download fails, provide instructions for manual setup.
                    showManualSetupInstructions(e2, fridaUrl);
                }
            }
        }
    }

    private void pushAndCheckVersion(File localFridaExe)
            throws DeviceNotAvailableException, IOException, TargetSetupError {
        // Upload Frida binary to device
        mRemoteFridaExeName = new File(TMP_PATH, localFridaExe.getName()).getAbsolutePath();
        mDevice.pushFile(localFridaExe, mRemoteFridaExeName);
        runAndCheck(mDevice, String.format("chmod a+x '%s'", mRemoteFridaExeName));
        mFridaFiles.add(mRemoteFridaExeName);
        if (!mDevice.doesFileExist(mRemoteFridaExeName.toString())) {
            throw new TargetSetupError("Failed to push Frida into the device");
        }

        // Throw if the version threshold is not met
        meetsVersionThreshold();
        CLog.d(
                "Frida is installed at %s in the device:%s",
                mRemoteFridaExeName, mDevice.getSerialNumber());
    }

    /**
     * Find out which Frida binary we need and download it if needed.
     *
     * @param device device to use Frida on
     * @param buildInfo test device build info (from test.getBuild())
     * @return FridaUtils object that can be used to run Frida scripts with
     */
    public static FridaUtils withFrida(ITestDevice device, IBuildInfo buildInfo) {
        String deviceSerialNum = device.getSerialNumber();
        if (DEVICE_FRIDA_UTILS_MAP.containsKey(deviceSerialNum)) {
            return DEVICE_FRIDA_UTILS_MAP.get(deviceSerialNum);
        }
        throw new IllegalStateException(
                "Unable to find FridaUtils object for device: " + deviceSerialNum);
    }

    /**
     * Upload and run frida script on given process.
     *
     * @param fridaJsScriptContent Content of the Frida JS script. Note: this is not a file name
     * @param targetProcessPid PID of the process to attach Frida to
     * @return ByteArrayOutputStream containing stdout and stderr of frida command
     */
    public ByteArrayOutputStream withFridaScript(
            final String fridaJsScriptContent, int targetProcessPid)
            throws DeviceNotAvailableException, FileNotFoundException, IOException,
                    TimeoutException, InterruptedException {
        return withFridaScript(fridaJsScriptContent, targetProcessPid, false);
    }

    /**
     * Upload and run frida script on given process.
     *
     * @param fridaJsScriptContent Content of the Frida JS script. Note: this is not a file name
     * @param targetProcessPid PID of the process to attach Frida to
     * @param allowMultipleFridaProcess allows multiple Frida processes when true
     * @return ByteArrayOutputStream containing stdout and stderr of frida command
     */
    public ByteArrayOutputStream withFridaScript(
            final String fridaJsScriptContent,
            int targetProcessPid,
            boolean allowMultipleFridaProcess)
            throws DeviceNotAvailableException, FileNotFoundException, IOException,
                    TimeoutException, InterruptedException {
        if (!(mTargetPidsToFridaPids.isEmpty() || allowMultipleFridaProcess)) {
            throw new RuntimeException(
                    "Multiple Frida processes are disabled, set allowMultipleFridaProcess to"
                            + " enable multiple frida processes");
        }
        if (mTargetPidsToFridaPids.containsKey(targetProcessPid)) {
            throw new RuntimeException(
                    "Frida is already attached to process with PID:" + targetProcessPid);
        }

        // Upload Frida script to device
        mDevice.enableAdbRoot();
        String uuid = UUID.randomUUID().toString();
        String remoteFridaJsScriptName =
                new File(TMP_PATH, "frida_" + uuid + ".js").getAbsolutePath();
        mDevice.pushString(fridaJsScriptContent, remoteFridaJsScriptName);
        mFridaFiles.add(remoteFridaJsScriptName);

        // Execute Frida, binding to given PID, in the background
        ByteArrayOutputStream output =
                runFrida(
                        List.of(
                                "-p",
                                String.valueOf(targetProcessPid),
                                "-s",
                                remoteFridaJsScriptName,
                                "--runtime=v8"));

        // Frida can fail to attach after a short pause so wait for that
        TimeUnit.SECONDS.sleep(5);
        try {
            Map<Integer, String> fridaProcessPids =
                    ProcessUtil.waitProcessRunning(
                            mDevice, "^" + mRemoteFridaExeName + ".-p." + targetProcessPid);
            if (fridaProcessPids.isEmpty()) {
                throw new RuntimeException(
                        "Frida process not found for target PID:" + targetProcessPid);
            }

            // Store target process pid and Frida process pid in mTargetPidsToFridaPids
            mTargetPidsToFridaPids.put(
                    targetProcessPid, fridaProcessPids.keySet().iterator().next());
        } catch (Exception e) {
            CLog.e(e);
            CLog.e("Frida attach output: %s", output.toString(StandardCharsets.UTF_8));
            throw e;
        }
        return output;
    }

    @Override
    /* Kill all running Frida processes. */
    public void close() throws DeviceNotAvailableException, TimeoutException {
        mDevice.enableAdbRoot();
        for (Integer pid : mTargetPidsToFridaPids.values()) {
            CLog.e("Killing frida pid: " + pid);
            try {
                ProcessUtil.killPid(mDevice, pid.intValue(), 10_000L);
            } catch (ProcessUtil.KillException e) {
                if (e.getReason() != ProcessUtil.KillException.Reason.NO_SUCH_PROCESS) {
                    CLog.e(e);
                }
            }
        }
        mTargetPidsToFridaPids.clear(); // Remove killed pids from mTargetPidsToFridaPids
        mDevice.disableAdbRoot();
    }

    @Override
    /* Delete Frida files from device and remove FridaUtils object from DEVICE_FRIDA_UTILS_MAP */
    public void tearDown(TestInformation testInformation, Throwable e) {
        try {
            for (String file : mFridaFiles) {
                testInformation.getDevice().deleteFile(file);
            }
            mFridaFiles.clear();
            DEVICE_FRIDA_UTILS_MAP.remove(testInformation.getDevice().getSerialNumber());
        } catch (Exception ignore) {
            // Ignore exceptions while cleanup
        }
    }

    /**
     * Return the best ABI of Frida that we should download for given device.
     *
     * <p>Throw UnsupportedOperationException if Frida does not support device's ABI.
     */
    private String getFridaAbiFor(ITestDevice device)
            throws DeviceNotAvailableException, UnsupportedOperationException {
        for (String abi : getSupportedAbis(device)) {
            if (abi.startsWith("arm64")) {
                return "arm64";
            } else if (abi.startsWith("armeabi")) {
                return "arm";
            } else if (abi.startsWith("x86_64")) {
                return "x86_64";
            } else if (abi.startsWith("x86")) {
                return "x86";
            }
        }
        throw new UnsupportedOperationException(
                String.format("Device %s is not supported by Frida", device.getSerialNumber()));
    }

    /* Return a list of supported ABIs by the device in order of preference. */
    private List<String> getSupportedAbis(ITestDevice device) throws DeviceNotAvailableException {
        String primaryAbi = device.getProperty(PRODUCT_CPU_ABI_KEY);
        String[] supportedAbis = device.getProperty(PRODUCT_CPU_ABILIST_KEY).split(",");
        return Stream.concat(Stream.of(primaryAbi), Arrays.stream(supportedAbis))
                .distinct()
                .collect(toList());
    }

    private ByteArrayOutputStream runFrida(List<String> args)
            throws DeviceNotAvailableException, IOException {
        mDevice.enableAdbRoot();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        List<String> cmd =
                new ArrayList<>(
                        List.of(
                                "adb",
                                "-s",
                                mDevice.getSerialNumber(),
                                "shell",
                                mRemoteFridaExeName));
        cmd.addAll(args);
        RunUtil.getDefault().runCmdInBackground(cmd, output);
        return output;
    }

    private void meetsVersionThreshold()
            throws IOException, IllegalArgumentException, DeviceNotAvailableException,
                    TargetSetupError {
        // Get version from Frida binary
        ByteArrayOutputStream output = runFrida(List.of("--version"));
        poll(() -> output.size() > 0);
        String version = output.toString(StandardCharsets.UTF_8).trim();
        CLog.d("Current version is: %s", version);

        // Get version threshold
        Optional<String> minVersionFromBl = FridaUtilsBusinessLogicHandler.getFridaVersion();
        String versionThreshold =
                minVersionFromBl.isPresent() ? minVersionFromBl.get() : VERSION_THRESHOLD;

        // Throw if threshold is not met
        if (compareVersion(version, versionThreshold) < 0) {
            throw new TargetSetupError(
                    String.format(
                            "Minimum version required is %s. Current version is %s.",
                            versionThreshold.toString(), version.toString()));
        }
    }

    /** Helper method to parse version parts and default missing parts to 0 */
    private int[] parseVersion(String version) {
        String[] parts = version.split("\\.");
        int major = (parts.length > 0) ? Integer.parseInt(parts[0]) : 0;
        int minor = (parts.length > 1) ? Integer.parseInt(parts[1]) : 0;
        int patch = (parts.length > 2) ? Integer.parseInt(parts[2]) : 0;
        return new int[] {major, minor, patch};
    }

    /** Helper method to compare versions */
    private int compareVersion(String version, String otherVersion) {
        // Parse the version, defaulting missing parts to 0
        int[] versionParts = parseVersion(version);
        int major = versionParts[0];
        int minor = versionParts[1];
        int patch = versionParts[2];

        // Parse the other version, defaulting missing parts to 0
        int[] otherVersionParts = parseVersion(otherVersion);
        int otherMajor = otherVersionParts[0];
        int otherMinor = otherVersionParts[1];
        int otherPatch = otherVersionParts[2];

        // Compare versions
        int res = Integer.compare(major, otherMajor);
        if (res == 0) {
            res = Integer.compare(minor, otherMinor);
            if (res == 0) {
                res = Integer.compare(patch, otherPatch);
            }
        }
        return res;
    }

    private void showManualSetupInstructions(Exception e, String fridaUrl)
            throws FileNotFoundException, TargetSetupError {
        throw new TargetSetupError(
                String.format(
                        "Could not download Frida. Please manually download %s and"
                                + " extract to '%s' renaming the extracted file to '%s' exactly."
                                + " Incorrect naming will cause a setup failure.",
                        fridaUrl != null
                                ? fridaUrl
                                : String.format(
                                        "the latest '%s-x.x.x-%s-%s.xz' from"
                                                + " https://github.com/%4$s/%4$s/releases",
                                        FRIDA_PACKAGE, FRIDA_OS, mFridaAbi, FRIDA_OWNER_AND_REPO),
                        mBuildHelper.getTestsDir(),
                        String.format(
                                "%s-%s-%s-%s", FRIDA_PACKAGE, mFridaVersion, FRIDA_OS, mFridaAbi)),
                e,
                mDevice.getDeviceDescriptor(),
                false /* deviceSide */);
    }

    private String getFridaDownloadUrl(Optional<String> releaseTagName)
            throws IOException, JsonParseException, MalformedURLException, URISyntaxException {
        String fridaFileNameTemplate = FridaUtilsBusinessLogicHandler.getFridaFilenameTemplate();
        String name =
                MessageFormat.format(
                        fridaFileNameTemplate != null
                                ? fridaFileNameTemplate
                                : FRIDA_FILE_NAME_TEMPLATE,
                        FRIDA_PACKAGE,
                        releaseTagName.isPresent() ? releaseTagName.get() : "(.*.)",
                        FRIDA_OS,
                        mFridaAbi);

        // Get the map of frida asset names to the download uris
        Map<String, URI> fridaAssetNameToUri =
                new GitHubRepo(FRIDA_OWNER_AND_REPO, FRIDA_OWNER_AND_REPO)
                        .getReleaseAssetUris(releaseTagName);

        // Get the download url from 'fridaAssetNameToUri' map
        for (Map.Entry<String, URI> entry : fridaAssetNameToUri.entrySet()) {
            Matcher matcher = Pattern.compile(name).matcher(entry.getKey());
            if (matcher.matches()) {
                return entry.getValue().toString();
            }
        }
        throw new RuntimeException("Could not fetch frida download url");
    }

    private static class FridaFileDownloader implements IFileDownloader {

        /** {@inheritDoc} */
        @Override
        public File downloadFile(String remoteFilePath) throws BuildRetrievalError {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc} */
        @Override
        public void downloadFile(String relativeRemotePath, File destFile)
                throws BuildRetrievalError {
            try {
                // Download Frida binary
                URLConnection conn = new URL(relativeRemotePath).openConnection();
                XZInputStream in = new XZInputStream(conn.getInputStream());
                FileUtil.writeToFile(in, destFile);
            } catch (Exception e) {
                throw new BuildRetrievalError("Downloading frida failed.", e);
            }
        }
    }
}
