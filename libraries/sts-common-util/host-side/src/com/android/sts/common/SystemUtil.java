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

import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.util.RunUtil;

import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/** Various system-related helper functions */
public class SystemUtil {
    public static final long DEFAULT_MAX_POLL_TIME_MS = 30_000L;
    public static final long DEFAULT_POLL_TIME_MS = 100L;

    /** Different namespaces for settings */
    public enum Namespace {
        SYSTEM("system"),
        SECURE("secure"),
        GLOBAL("global");

        private final String namespace;

        private Namespace(String namespace) {
            this.namespace = namespace;
        }

        public final String getValue() {
            return this.namespace;
        }
    }

    private SystemUtil() {}

    /**
     * Set the value of a property and set it back to old value upon closing.
     *
     * @param device the device to use
     * @param name the name of the property to set
     * @param value the value that the property should be set to
     * @return AutoCloseable that resets the property back to old value upon closing
     */
    public static AutoCloseable withProperty(
            final ITestDevice device, final String name, final String value)
            throws DeviceNotAvailableException {
        // Set the property with the specified value
        final String oldValue = device.getProperty(name);
        if (!device.setProperty(name, value)) {
            throw new IllegalStateException(String.format("Could not reset property: %s", name));
        }

        // Return AutoCloseable to reset the property back to old value
        return () -> {
            if (!device.setProperty(name, oldValue == null ? "" : oldValue)) {
                throw new IllegalStateException(
                        String.format("Could not reset property: %s", name));
            }
        };
    }

    /**
     * Set the value of a device setting and set it back to old value upon closing.
     *
     * @param device the device to use
     * @param namespace the namespace from the enum Namespace [SYSTEM, SECURE, or GLOBAL]
     * @param key setting key to set
     * @param value setting value to set to
     * @param userId the ID of the user for whom the setting should be applied
     * @return AutoCloseable that resets the setting back to existing value upon closing
     */
    public static AutoCloseable withSetting(
            final ITestDevice device,
            final Namespace namespace,
            final String key,
            final String value,
            final int userId)
            throws DeviceNotAvailableException {
        // Check if the user associated with the userId exists
        if (!device.listUsers().contains(userId)) {
            throw new IllegalArgumentException("User " + userId + " does not exist");
        }

        // Return if the required value is already set
        final String namespaceStr = namespace.getValue();
        final String currentAssignedValue = device.getSetting(namespaceStr, key);
        if (currentAssignedValue.equals(value)) {
            return () -> {};
        }

        // Set the settings field
        final Optional<String> oldSetting = Optional.ofNullable(currentAssignedValue);
        device.setSetting(userId, namespaceStr, key, value);
        final Predicate<String> wasSettingSetSuccessfully =
                (currentValue) ->
                        poll(
                                () -> {
                                    try {
                                        return device.getSetting(userId, namespaceStr, key)
                                                .equals(currentValue);
                                    } catch (Exception e) {
                                        throw new IllegalStateException(e);
                                    }
                                });
        if (!wasSettingSetSuccessfully.test(value)) {
            throw new IllegalStateException(
                    String.format(
                            "Failed to set %s settings : %s to %s for user %d",
                            namespaceStr, key, value, userId));
        }

        // Return an AutoCloseable to restore the settings
        return () -> {
            final String oldValue = oldSetting.get();
            if (oldSetting.isEmpty()) {
                String cmd =
                        String.format("settings delete --user %d %s %s", userId, namespaceStr, key);
                runAndCheck(device, cmd);
            } else {
                device.setSetting(userId, namespaceStr, key, oldValue);
            }
            if (!wasSettingSetSuccessfully.test(oldValue))
                throw new IllegalStateException(
                        String.format(
                                "Failed to restore the %s setting from '%s' back to '%s' for user"
                                        + " %d",
                                namespaceStr, key, oldValue, userId));
        };
    }

    /**
     * Set the value of a device setting and set it back to old value upon closing.
     *
     * @param device the device to use
     * @param namespace the namespace from the enum Namespace [SYSTEM, SECURE, or GLOBAL]
     * @param key setting key to set
     * @param value setting value to set to
     * @return AutoCloseable that resets the setting back to existing value upon closing
     */
    public static AutoCloseable withSetting(
            final ITestDevice device,
            final Namespace namespace,
            final String key,
            final String value)
            throws DeviceNotAvailableException {
        return withSetting(device, namespace, key, value, device.getCurrentUser());
    }

    /**
     * Set the value of a device setting and set it back to old value upon closing.
     *
     * @param device the device to use
     * @param namespace "system", "secure", or "global"
     * @param key setting key to set
     * @param value setting value to set to
     * @return AutoCloseable that resets the setting back to existing value upon closing.
     * @deprecated Use {@link #withSetting(ITestDevice, Namespace, String, String)} instead.
     */
    @Deprecated
    public static AutoCloseable withSetting(
            final ITestDevice device, final String namespace, final String key, final String value)
            throws DeviceNotAvailableException {
        // Check if the value of namespace corresponds to one of 'system', 'secure', or 'global'
        Namespace targetNamespace = null;
        switch (namespace) {
            case "system":
                targetNamespace = Namespace.SYSTEM;
                break;
            case "secure":
                targetNamespace = Namespace.SECURE;
                break;
            case "global":
                targetNamespace = Namespace.GLOBAL;
                break;
            default:
                throw new IllegalArgumentException(
                        String.format(
                                "'%s' was provided. Namespace must be one of the following:"
                                    + " 'system', 'secure', 'global'",
                                namespace));
        }
        return withSetting(device, targetNamespace, key, value);
    }

    /**
     * Ensures Bluetooth is enabled on the specified test device.
     *
     * @param device The test device on which Bluetooth is to be enabled.
     * @return An AutoCloseable resource that restores the state of bluetooth.
     */
    public static AutoCloseable withBluetoothEnabled(ITestDevice device)
            throws DeviceNotAvailableException, IllegalStateException {
        if (!device.hasFeature("android.hardware.bluetooth")) {
            throw new IllegalStateException("Device does not support bluetooth");
        }

        // Check if bluetooth is already enabled
        if (device.getSetting("global", "bluetooth_on").trim().equals("1")) {
            return () -> {};
        }

        // Enable bluetooth and disable it later
        runAndCheck(device, "svc bluetooth enable");
        return () -> runAndCheck(device, "svc bluetooth disable");
    }

    /**
     * Poll on a condition supplied by the user.
     *
     * @param waitCondition returns true when the polling condition is met, false otherwise.
     * @return boolean value of {@code waitCondition}.
     * @throws IllegalArgumentException when {@code pollingTime} is not a positive integer and is
     *     not less than {@code maxPollingTime}.
     */
    public static boolean poll(BooleanSupplier waitCondition) throws IllegalArgumentException {
        return poll(waitCondition, DEFAULT_POLL_TIME_MS, DEFAULT_MAX_POLL_TIME_MS);
    }

    /**
     * Poll on a condition supplied by the user.
     *
     * @param waitCondition returns true when the polling condition is met, false otherwise.
     * @param pollingTime wait between successive calls to fetch value of {@code waitCondition} in
     *     milliseconds
     * @param maxPollingTime maximum waiting time before return.
     * @return boolean value of {@code waitCondition}.
     * @throws IllegalArgumentException when {@code pollingTime} is not a positive ineteger and is
     *     not less than {@code maxPollingTime}.
     */
    public static boolean poll(BooleanSupplier waitCondition, long pollingTime, long maxPollingTime)
            throws IllegalArgumentException {
        // The value of pollingTime should be a positive integer
        if (pollingTime <= 0) {
            throw new IllegalArgumentException("pollingTime should be a positive integer");
        }

        // The value of pollingTime should be less than maxPollingTime
        // If not, use the minimum of the two.
        pollingTime = Math.min(pollingTime, maxPollingTime);
        if (pollingTime == maxPollingTime) {
            CLog.i(
                    String.format(
                            "Provided polling time %d is greater than the maximum limit."
                                    + " Using %d as the polling time",
                            pollingTime, maxPollingTime));
        }

        final long startTime = System.currentTimeMillis();
        do {
            if (waitCondition.getAsBoolean()) {
                return true;
            }
            RunUtil.getDefault().sleep(pollingTime);
        } while (System.currentTimeMillis() - startTime <= maxPollingTime);

        return waitCondition.getAsBoolean();
    }
}
