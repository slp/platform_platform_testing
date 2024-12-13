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

import static com.android.tradefed.util.FileUtil.chmodRWXRecursively;
import static com.android.tradefed.util.FileUtil.createNamedTempDir;
import static com.android.tradefed.util.FileUtil.recursiveDelete;
import static com.android.tradefed.util.ZipUtil.extractZip;

import com.android.sts.common.GitHubUtils.GitHubRepo;
import com.android.sts.common.util.GhidraBusinessLogicHandler;
import com.android.tradefed.build.BuildRetrievalError;
import com.android.tradefed.build.FileDownloadCache;
import com.android.tradefed.build.FileDownloadCacheFactory;
import com.android.tradefed.build.IFileDownloader;
import com.android.tradefed.config.Option;
import com.android.tradefed.config.OptionClass;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.invoker.TestInformation;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.targetprep.BaseTargetPreparer;
import com.android.tradefed.targetprep.BuildError;
import com.android.tradefed.targetprep.TargetSetupError;
import com.android.tradefed.util.net.HttpHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

/** Preparer to download Ghidra, unzip it and set the path to the executable */
@OptionClass(alias = "ghidra-preparer")
public class GhidraPreparer extends BaseTargetPreparer {
    private static final Object LOCK_SETUP = new Object(); // Locks the setup
    private static final String GHIDRA_REPO_OWNER = "NationalSecurityAgency";
    private static final String GHIDRA_REPO_NAME = "ghidra";
    private static final String VERSION_THRESHOLD = "11.1.2";
    private static final String DEFAULT_ZIP_NAME = "ghidra.zip";
    private static final String BASE_INSTALLATION_DIR = "tradefed_ghidra";
    private static final String BASE_CACHE_DIR = "ghidra_cache";
    private static final String ANALYZE_HEADLESS = "analyzeHeadless";
    public static final String PROPERTY_KEY = "ghidra_analyze_headless";
    private File mGhidraZipDir = null; // Stores the ghidra zip directory
    private File mGhidraZipFile = null; // Stores the ghidra zip file
    private File mCacheDir = null; // Refers to the ghidra cache directory
    private URI mGhidraZipUri = null; // Stores the url to download ghidra zip
    private String mPreviousPropertyVal = null; // To restore analyzeHeadless path set in other runs
    private Optional<String> mGhidraZipNameFromBl = Optional.empty();

    @Option(
            name = "ghidra-tag",
            description = "Overrides 'gitReleaseTagName' of GhidraBusinessLogicHandler.")
    private String mTagName = null;

    @Option(
            name = "ghidra-asset",
            description = "Overrides 'gitReleaseAssetName' of GhidraBusinessLogicHandler.")
    private String mAssetName = null;

    @Option(name = "ghidra-url", description = "Custom url for Ghidra zip download.")
    private String mCustomGhidraUri = null;

    /** {@inheritDoc} */
    @Override
    public void setUp(TestInformation testInformation)
            throws DeviceNotAvailableException, BuildError, TargetSetupError {
        synchronized (LOCK_SETUP) {
            mGhidraZipNameFromBl = GhidraBusinessLogicHandler.getReleaseAssetName();
            String ghidraZipName =
                    mGhidraZipNameFromBl.isPresent()
                            ? mGhidraZipNameFromBl.get()
                            : DEFAULT_ZIP_NAME;
            ghidraZipName = (mAssetName == null) ? ghidraZipName : mAssetName;

            // Fetch value of property 'ghidra_analyze_headless' to restore later
            mPreviousPropertyVal = testInformation.properties().get(PROPERTY_KEY);

            // Set PROPERTY_KEY with the location of ANALYZE_HEADLESS
            // Preference order for sourcing Ghidra zip:
            // pre-existing or manually downloaded > Custom url > GitHub.
            // After downloading, the zip is extracted at
            // /tmp/tradefed_ghidra/<mGhidraZipDir>/<ghidra_zip_here>
            try {
                // Verify the existing Ghidra installation if found
                File existingZipDir =
                        Paths.get(String.format("/tmp/%s", BASE_INSTALLATION_DIR), ghidraZipName)
                                .toFile();

                // Update current installation directory
                mGhidraZipDir = existingZipDir;
                CLog.d(
                        "Setting current Ghidra installation directory to %s and verifying the"
                                + " installation",
                        mGhidraZipDir);

                if (!existingZipDir.exists()) {
                    throw new RuntimeException(
                            String.format(
                                    "Ghidra not found in %s directory hence downloading if"
                                            + " required",
                                    mGhidraZipDir));
                } else {
                    // Set PROPERTY_KEY if installation is proper
                    verifyInstallation(testInformation);
                }
            } catch (Exception e) {
                try {
                    CLog.d("Downloading Ghidra zip due to Exception: %s", e);

                    // Create required directories for Ghidra installation
                    // Fetch Ghidra download url from GitHub if a custom URL is not provided.
                    if (mCustomGhidraUri != null) {
                        mGhidraZipUri = new URI(mCustomGhidraUri);

                        // Create required directories and update current installation directory
                        createZipAndCacheDirs(ghidraZipName);
                    } else {
                        Map.Entry<String, URI> assetNameToUri = getZipNameAndUri();
                        ghidraZipName = assetNameToUri.getKey();
                        mGhidraZipUri = assetNameToUri.getValue();

                        // Create required directories and update current installation directory
                        createZipAndCacheDirs(ghidraZipName);
                    }

                    // Set PROPERTY_KEY if installation is proper
                    verifyInstallation(testInformation);
                } catch (Exception e2) {
                    // Remove the ghidra directory for the current version
                    if (mGhidraZipDir != null) {
                        recursiveDelete(mGhidraZipDir);
                        mGhidraZipDir = null;
                    }

                    // Remove the cache directory
                    if (mCacheDir != null) {
                        recursiveDelete(mCacheDir);
                        mCacheDir = null;
                    }
                    throw new TargetSetupError(
                            String.format(
                                    "Please manually download the latest version of Ghidra zip"
                                        + " from:"
                                        + " [https://github.com/NationalSecurityAgency/ghidra/releases]"
                                        + " to /tmp/%s/%2$s/%2$s. Make sure to rename the zip to"
                                        + " %2$s. Ensure /tmp/%s/%2$s/ directory does not contain"
                                        + " any other files. \n",
                                    BASE_INSTALLATION_DIR, ghidraZipName),
                            e2,
                            null /* deviceDescriptor */,
                            false /* deviceSide */);
                }
            }
        }
    }

    private boolean lazyDownloadAndExtractGhidra() throws Exception {
        boolean result = false;
        // Download Ghidra zip
        mGhidraZipFile = new File(mGhidraZipDir, mGhidraZipDir.getName());
        if (!mGhidraZipFile.exists() && (mGhidraZipUri != null)) {
            CLog.d("Downloading Ghidra zip from %s to %s", mGhidraZipUri, mGhidraZipFile);
            FileDownloadCache fileDownloadCache =
                    FileDownloadCacheFactory.getInstance().getCache(mCacheDir);
            fileDownloadCache.fetchRemoteFile(
                    new GhidraFileDownloader(), mGhidraZipUri.toString(), mGhidraZipFile);
            result = true;
        }

        if (mGhidraZipFile.exists()) {
            // Unzip Ghidra zip and delete the zip
            CLog.d("Unzipping %s", mGhidraZipFile);
            extractZip(new ZipFile(mGhidraZipFile), mGhidraZipDir);
            recursiveDelete(mGhidraZipFile);

            // Chmod rwx 'mGhidraZipDir'
            chmodRWXRecursively(mGhidraZipDir);
            result = true;
        }
        return result;
    }

    /** Downloads and extracts if required. Checks for correct version. Sets PROPERTY_KEY */
    private void verifyInstallation(TestInformation testInformation) throws Exception {
        // Return if installation already exists
        if (findAnalyzeHeadlessAndSetProperty(testInformation)) {
            return;
        }

        // Download and extract if required
        if (lazyDownloadAndExtractGhidra()) {
            // Set PROPERTY_KEY with ANALYZE_HEADLESS path if found else throws
            if (!findAnalyzeHeadlessAndSetProperty(testInformation)) {
                throw new TargetSetupError(
                        String.format("Ghidra installation at %s is not proper.", mGhidraZipDir));
            }
        } else {
            throw new TargetSetupError(
                    String.format("No Ghidra zip found inside %s", mGhidraZipDir));
        }
    }

    private boolean findAnalyzeHeadlessAndSetProperty(TestInformation testInformation)
            throws IOException, TargetSetupError {
        String analyzeHeadlessPath = getFilePath(String.format("/%s", ANALYZE_HEADLESS));
        if (analyzeHeadlessPath == null) {
            return false;
        } else {
            CLog.d("analyzeHeadless found at %s", analyzeHeadlessPath);
            if (meetsVersionThreshold() < 0) {
                CLog.d(
                        "Ghidra installation at %s does not meet the minimum version"
                                + " requirement of %s and higher",
                        analyzeHeadlessPath, VERSION_THRESHOLD);
                return false;
            }
        }
        CLog.d("Setting property to %s", analyzeHeadlessPath);
        testInformation.properties().put(PROPERTY_KEY, analyzeHeadlessPath);
        return true;
    }

    @Override
    public void tearDown(TestInformation testInformation, Throwable e) {
        // Restore the previous property value
        if (mPreviousPropertyVal != null) {
            testInformation.properties().put(PROPERTY_KEY, mPreviousPropertyVal);
        } else {
            testInformation.properties().remove(PROPERTY_KEY);
        }
    }

    /** Fetch the first entry from the map of assetName to URI */
    private Map.Entry<String, URI> getZipNameAndUri() throws IOException, URISyntaxException {
        Optional<String> ghidraReleaseTagName = GhidraBusinessLogicHandler.getGitReleaseTagName();
        Optional<String> ghidraReleaseAssetName = mGhidraZipNameFromBl;
        if (mTagName != null) {
            ghidraReleaseTagName = Optional.of(mTagName);
        }
        if (mAssetName != null) {
            ghidraReleaseAssetName = Optional.of(mAssetName);
        }

        // Fetch the assetName to uri map
        Map<String, URI> mapOfAssetNameToUris =
                new GitHubRepo(GHIDRA_REPO_OWNER, GHIDRA_REPO_NAME)
                        .getReleaseAssetUris(ghidraReleaseTagName);

        // Get map entry corresponding to 'ghidraReleaseAssetName'
        if (ghidraReleaseAssetName.isPresent()) {
            String assetName = ghidraReleaseAssetName.get();
            URI assetUri = mapOfAssetNameToUris.get(assetName);
            if (assetUri == null) {
                throw new IllegalStateException(
                        String.format(
                                "The asset name: '%s' was not found in ghidra releases.",
                                assetName));
            }
            return new AbstractMap.SimpleEntry(assetName, assetUri);
        }

        // Throw if more than one entry was found in the map
        if (mapOfAssetNameToUris.size() != 1) {
            throw new IllegalStateException(
                    String.format(
                            "More than one entries found in 'mapOfAssetNameToUris'. Entries: %s",
                            mapOfAssetNameToUris.toString()));
        }

        // Return the first entry from the map
        return mapOfAssetNameToUris.entrySet().iterator().next();
    }

    private void createZipAndCacheDirs(String ghidraZipName) throws IOException {
        mGhidraZipDir =
                createNamedTempDir(Paths.get(BASE_INSTALLATION_DIR, ghidraZipName).toString());
        CLog.d(
                "Setting current Ghidra installation directory to %s and verifying the"
                        + " installation",
                mGhidraZipDir);

        mCacheDir = createNamedTempDir(BASE_CACHE_DIR);
    }

    /** If found returns the path to 'fileName' else returns null. */
    private String getFilePath(String fileName) {
        Optional<Path> pathToFile = Optional.empty();
        try (Stream<Path> walkStream = Files.walk(mGhidraZipDir.toPath())) {
            pathToFile =
                    walkStream
                            .filter(path -> path.toFile().isFile())
                            .filter(path -> path.toString().endsWith(fileName))
                            .findFirst();
        } catch (Exception e) {
            // Ignore exceptions
        }
        return pathToFile.isPresent() ? pathToFile.get().toString() : null;
    }

    /** Returns 1 if threshold is met, returns -1 if not met, returns 0 if versions match */
    private int meetsVersionThreshold()
            throws IOException, FileNotFoundException, TargetSetupError {
        // Get version threshold
        Optional<String> minGhidraVersionFromBl = GhidraBusinessLogicHandler.getMinGhidraVersion();
        String versionThreshold =
                minGhidraVersionFromBl.isPresent()
                        ? minGhidraVersionFromBl.get()
                        : VERSION_THRESHOLD;

        String ghidraPropertiesPath = getFilePath("application.properties");
        if (ghidraPropertiesPath == null) {
            throw new TargetSetupError(
                    String.format("application.properties not found inside %s", mGhidraZipDir));
        }
        Properties ghidraProperties = new Properties();
        ghidraProperties.load(new FileInputStream(getFilePath("application.properties")));

        // Get current installation version from Ghidra application properties file
        String currentVersion = ghidraProperties.getProperty("application.version");

        // Check if currentVersion is available and properly formatted
        if (currentVersion == null || !currentVersion.matches("^\\d+(\\.\\d+)*$")) {
            throw new TargetSetupError(
                    String.format(
                            "Unable to read version from %s. Current version is %s",
                            ghidraPropertiesPath, currentVersion));
        }
        CLog.d(
                "Minimum Ghidra version required: %s. The current installation's version: %s",
                versionThreshold, currentVersion);
        return compareVersion(currentVersion, versionThreshold);
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

    private static class GhidraFileDownloader implements IFileDownloader {

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
                // Download ghidra zip
                new HttpHelper().doGet(relativeRemotePath, new FileOutputStream(destFile));
            } catch (Exception e) {
                throw new BuildRetrievalError("Downloading ghidra zip failed.", e);
            }
        }
    }
}
