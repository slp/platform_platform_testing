/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.sts.common.util;

import java.lang.Integer;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Tools for parsing kernel version strings */
public final class KernelVersion implements Comparable<KernelVersion> {
    public final int version;
    public final int patchLevel;
    public final int subLevel;
    public Optional<Integer> osRelease;

    public KernelVersion(int version, int patchLevel, int subLevel) {
        this.version = version;
        this.patchLevel = patchLevel;
        this.subLevel = subLevel;
        this.osRelease = Optional.empty();
    }

    public KernelVersion(int version, int patchLevel, int subLevel, Optional<Integer> osRelease) {
        this.version = version;
        this.patchLevel = patchLevel;
        this.subLevel = subLevel;
        this.osRelease = osRelease;
    }

    /**
     * Parse a kernel version string in the format "version.patchlevel.sublevel-androidosRelease" -
     * "5.4.123-android12". Trailing values are ignored so `uname -r` can be parsed properly.
     *
     * @param versionString The version string to parse
     */
    public static KernelVersion parse(String versionString) {
        Pattern kernelReleasePattern =
                Pattern.compile(
                        "^(?<version>\\d+)[.](?<patchLevel>\\d+)[.](?<subLevel>\\d+)(-android(?<osRelease>\\d+))?(.*)$");
        Matcher matcher = kernelReleasePattern.matcher(versionString);
        if (matcher.find()) {
            return new KernelVersion(
                    Integer.parseInt(matcher.group("version")),
                    Integer.parseInt(matcher.group("patchLevel")),
                    Integer.parseInt(matcher.group("subLevel")),
                    Optional.ofNullable(matcher.group("osRelease")).map(Integer::parseInt));
        }
        throw new IllegalArgumentException(
                String.format("Could not parse kernel version string (%s)", versionString));
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return toStringWithRelease().hashCode();
    }

    /** Compare by version, patchlevel, and sublevel in that order. */
    public int compareTo(KernelVersion o) {
        if (version != o.version) {
            return Integer.compare(version, o.version);
        }
        if (patchLevel != o.patchLevel) {
            return Integer.compare(patchLevel, o.patchLevel);
        }
        if (subLevel != o.subLevel) {
            return Integer.compare(subLevel, o.subLevel);
        }
        return Integer.compare(
                osRelease.orElse(Integer.valueOf(0)), o.osRelease.orElse(Integer.valueOf(0)));
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (o instanceof KernelVersion) {
            return this.compareTo((KernelVersion) o) == 0;
        }
        return false;
    }

    /** Format as "version.patchlevel.sublevel" */
    @Override
    public String toString() {
        return String.format("%d.%d.%d", version, patchLevel, subLevel);
    }

    /** Format as "version.patchlevel" */
    public String toStringShort() {
        return String.format("%d.%d", version, patchLevel);
    }

    public String toStringWithRelease() {
        if (!osRelease.isPresent()) {
            return toString();
        }
        return String.format(
                "%d.%d.%d-android%d", version, patchLevel, subLevel, osRelease.get().intValue());
    }
}
