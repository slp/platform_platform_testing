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
import com.android.tradefed.targetprep.BaseTargetPreparer;
import com.android.tradefed.targetprep.BuildError;
import com.android.tradefed.targetprep.TargetSetupError;
import com.android.tradefed.util.net.HttpHelper;

import java.io.File;
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
import java.util.stream.Stream;
import java.util.zip.ZipFile;

/** Preparer to download Ghidra, unzip it and set the path to the executable */
@OptionClass(alias = "ghidra-preparer")
public class GhidraPreparer extends BaseTargetPreparer {
    private static final Object LOCK_SETUP = new Object(); // Locks the setup
    private static final String GHIDRA_REPO_OWNER = "NationalSecurityAgency";
    private static final String GHIDRA_REPO_NAME = "ghidra";
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
                    mGhidraZipNameFromBl.isPresent() ? mGhidraZipNameFromBl.get() : "ghidra.zip";
            ghidraZipName = (mAssetName == null) ? ghidraZipName : mAssetName;
            try {
                // Fetch value of property 'ghidra_analyze_headless' to restore later
                mPreviousPropertyVal = testInformation.properties().get(PROPERTY_KEY);

                // Check if Ghidra zip was manually downloaded. If not then create required
                // directories, download ghidra and extract the zip at
                // /tmp/tradefed_ghidra/<mGhidraZipDir>/<ghidra_zip_here>
                if (!Paths.get("/tmp/tradefed_ghidra", ghidraZipName).toFile().exists()) {
                    if (mCustomGhidraUri == null) {
                        Map.Entry<String, URI> assetNameToUri = getZipNameAndUri();
                        ghidraZipName = assetNameToUri.getKey();
                        mGhidraZipUri = assetNameToUri.getValue();
                    } else {
                        mGhidraZipUri = new URI(mCustomGhidraUri);
                    }
                    mGhidraZipDir =
                            createNamedTempDir(
                                    Paths.get("tradefed_ghidra", ghidraZipName).toString());
                    mCacheDir = createNamedTempDir("ghidra_cache");
                } else {
                    mGhidraZipDir = Paths.get("/tmp/tradefed_ghidra", ghidraZipName).toFile();
                }

                // If 'analyzeHeadless' already exists add path to properties and return.
                String analyzeHeadlessPath = getAnalyzeHeadlessPath();
                if (analyzeHeadlessPath != null) {
                    testInformation.properties().put(PROPERTY_KEY, analyzeHeadlessPath);
                    return;
                }

                // Download and extract ghidra zip if needed
                lazyDownloadAndExtractGhidra();

                // Add path of 'analyzeHeadless' to properties
                analyzeHeadlessPath = getAnalyzeHeadlessPath();
                if (analyzeHeadlessPath != null) {
                    testInformation.properties().put(PROPERTY_KEY, analyzeHeadlessPath);
                } else {
                    throw new TargetSetupError("Failed to fetch 'analyzeHeadless' location.");
                }
            } catch (Exception e) {
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
                                "Please manually download ghidra from"
                                    + " 'https://github.com/NationalSecurityAgency/ghidra/releases'"
                                    + " to /tmp/tradefed_ghidra/%1$s/%1$s. Make sure to rename the"
                                    + " zip to %1$s if required.",
                                ghidraZipName),
                        e,
                        null /* deviceDescriptor */,
                        false /* deviceSide */);
            }
        }
    }

    private void lazyDownloadAndExtractGhidra() throws Exception {
        // Download Ghidra zip
        mGhidraZipFile = new File(mGhidraZipDir, mGhidraZipDir.getName());
        if (!mGhidraZipFile.exists()) {
            FileDownloadCache fileDownloadCache =
                    FileDownloadCacheFactory.getInstance().getCache(mCacheDir);
            fileDownloadCache.fetchRemoteFile(
                    new GhidraFileDownloader(), mGhidraZipUri.toString(), mGhidraZipFile);
        }

        // Unzip Ghidra zip and delete the zip
        extractZip(new ZipFile(mGhidraZipFile), mGhidraZipDir);
        recursiveDelete(mGhidraZipFile);

        // Chmod rwx 'mGhidraZipDir'
        chmodRWXRecursively(mGhidraZipDir);
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
                        "The asset name:" + assetName + " was not found in ghidra release.");
            }
            return new AbstractMap.SimpleEntry(assetName, assetUri);
        }

        // Throw if more than one entry was found in the map
        if (mapOfAssetNameToUris.size() != 1) {
            throw new IllegalStateException(
                    "More than one entries found in 'mapOfAssetNameToUris'. Entries: "
                            + mapOfAssetNameToUris.toString());
        }

        // Return the first entry from the map
        return mapOfAssetNameToUris.entrySet().iterator().next();
    }

    /** If found returns the path to 'analyzeHeadless' else returns null. */
    private String getAnalyzeHeadlessPath() {
        Optional<Path> pathToAnalyzeHeadless = Optional.empty();
        try (Stream<Path> walkStream = Files.walk(mGhidraZipDir.toPath())) {
            pathToAnalyzeHeadless =
                    walkStream
                            .filter(path -> path.toFile().isFile())
                            .filter(path -> path.toString().endsWith("/analyzeHeadless"))
                            .findFirst();
        } catch (Exception e) {
            // Ignore exceptions
        }
        return pathToAnalyzeHeadless.isPresent() ? pathToAnalyzeHeadless.get().toString() : null;
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
