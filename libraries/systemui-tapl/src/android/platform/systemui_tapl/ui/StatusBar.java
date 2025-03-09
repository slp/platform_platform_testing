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

package android.platform.systemui_tapl.ui;

import static android.platform.systemui_tapl.utils.DeviceUtils.LONG_WAIT;
import static android.platform.systemui_tapl.utils.DeviceUtils.SHORT_WAIT;
import static android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector;
import static android.platform.test.util.HealthTestingUtils.waitForValueCatchingStaleObjectExceptions;
import static android.platform.uiautomatorhelpers.DeviceHelpers.getUiDevice;
import static android.platform.uiautomatorhelpers.WaitUtils.ensureThat;

import static androidx.test.uiautomator.Until.findObject;

import static com.android.settingslib.flags.Flags.newStatusBarIcons;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.junit.Assume.assumeFalse;

import android.app.Flags;
import android.graphics.Point;
import android.graphics.Rect;
import android.platform.helpers.foldable.UnfoldAnimationTestingUtils;
import android.platform.uiautomatorhelpers.DeviceHelpers;

import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.SearchCondition;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** System UI test automation object representing status bar. */
public class StatusBar {

    private static final BySelector PERCENTAGE_SELECTOR = By.text(Pattern.compile("\\d+%"));
    private static final String BATTERY_ID = "battery";
    static final String BATTERY_LEVEL_TEXT_ID = "battery_percentage_view";
    static final String CLOCK_ID = "clock";
    private static final String NOTIFICATION_ICON_CONTAINER_ID = "notificationIcons";
    private static final BySelector NOTIFICATION_LIGHTS_OUT_DOT_SELECTOR =
            sysuiResSelector("notification_lights_out");
    private static final String UI_SYSTEM_ICONS_ID = "system_icons";
    private static final String DATE_ID = "date";
    static final String STATUS_ICON_CONTAINER_ID = "statusIcons";
    private static final String ALARM_ICON_DESC_PREFIX_STRING = "Alarm";
    private static final String AIRPLANE_MODE_ICON_DESC = "Airplane mode.";
    private static final String DATA_SAVER_ICON_DESC = "Data Saver is on";
    // https://hsv.googleplex.com/6227911158792192?node=18
    static final String DOCK_DEFEND_ICON_SUFFIX_STRING = "charging paused for battery protection";
    static final String DND_ICON_DESC = Flags.modesUi() ? "Do Not Disturb is on" : "Do Not Disturb";
    private static final String WIFI_ICON_ID = "wifi_combo";
    private static final String ONGOING_ACTIVITY_CHIP_ICON_ID = "ongoing_activity_chip_primary";
    static final String SCREEN_RECORD_DESC_STRING = "Recording screen";
    static final String SILENT_ICON_DESC_PREFIX_STRING = "Ringer silent";
    static final String VIBRATE_ICON_DESC_PREFIX_STRING = "Ringer vibrate";
    private final List<String> mStatusBarViewIds =
            List.of(DATE_ID, CLOCK_ID, WIFI_ICON_ID, BATTERY_ID, BATTERY_LEVEL_TEXT_ID);

    StatusBar() {
        verifyClockIsVisible();
    }

    private static List<UiObject2> getNotificationIconsObjects() {
        // As the container for notifications can change between the moment we get it and we get its
        // children, we retry several times in case of failure. This aims at reducing flakiness.
        return waitForValueCatchingStaleObjectExceptions(
                () -> "Failed to get status bar icons.",
                () -> {
                    UiObject2 container =
                            getUiDevice()
                                    .wait(
                                            Until.findObject(
                                                    sysuiResSelector(
                                                            NOTIFICATION_ICON_CONTAINER_ID)),
                                            10000);
                    return container != null ? container.getChildren() : Collections.emptyList();
                });
    }

    /** Returns the number of notification icons visible on the status bar. */
    public int getNotificationIconCount() {
        List<UiObject2> icons = getNotificationIconsObjects();

        // 2 icons are never ellipsized, let's return them.
        if (icons.size() <= 2) {
            return icons.size();
        }

        // Notification icons don't change visibility when collapsed.
        // The same icon can represent either the notification and the ellipsis.
        //
        // For this reason, UiAutomator thinks the icons are always visible, when we're
        // actually stacking them up on top of each other and drawing transparent.
        //
        // Let's check for overlap instead of view visibility.
        int iconPadding =
                icons.get(1).getVisibleBounds().left - icons.get(0).getVisibleBounds().left;
        int lastIcon = icons.size() - 1;
        for (int i = 1; i < icons.size(); i++) {
            lastIcon = i;
            int padding =
                    icons.get(i).getVisibleBounds().left - icons.get(i - 1).getVisibleBounds().left;
            if (padding != iconPadding) {
                break;
            }
        }
        return lastIcon;
    }

    /** Verifies visibility of the battery percentage indicator. */
    public void verifyBatteryPercentageVisibility(boolean expectedVisible) {
        assumeFalse(newStatusBarIcons());
        UiObject2 batteryIndication = getBatteryIndication();
        assertThat(batteryIndication).isNotNull();

        if (expectedVisible) {
            assertThat(batteryIndication.wait(Until.hasObject(PERCENTAGE_SELECTOR), 10000))
                    .isTrue();
        } else {
            assertThat(batteryIndication.wait(Until.gone(PERCENTAGE_SELECTOR), 10000)).isTrue();
        }
    }

    private static UiObject2 getBatteryIndication() {
        return getUiDevice().wait(findObject(sysuiResSelector(BATTERY_ID)), 1000);
    }

    /** Assert that clock indicator is visible. */
    private void verifyClockIsVisible() {
        // Matches 12h or 24h time format
        Pattern timePattern = Pattern.compile("^(?:[01]?\\d|2[0-3]):[0-5]\\d");
        DeviceHelpers.waitForObj(
                By.pkg("com.android.systemui").text(timePattern),
                SHORT_WAIT,
                () -> "Clock should be visible.");
    }

    /** Assert that clock indicator is NOT visible. */
    public void verifyClockIsNotVisible() {
        assertWithMessage("StatusBar clock is visible")
                .that(
                        getUiDevice()
                                .wait(Until.gone(sysuiResSelector(CLOCK_ID)), LONG_WAIT.toMillis()))
                .isTrue();
    }

    /** Assert that the lights out notification dot is visible. */
    public void verifyLightsOutDotIsVisible() {
        SearchCondition<Boolean> searchCondition =
                Until.hasObject(NOTIFICATION_LIGHTS_OUT_DOT_SELECTOR);
        assertThat(getUiDevice().wait(searchCondition, LONG_WAIT.toMillis())).isTrue();
    }

    /** Assert that the lights out notification dot is NOT visible. */
    public void verifyLightsOutDotIsNotVisible() {
        SearchCondition<Boolean> searchCondition = Until.gone(NOTIFICATION_LIGHTS_OUT_DOT_SELECTOR);
        assertThat(getUiDevice().wait(searchCondition, LONG_WAIT.toMillis())).isTrue();
    }

    /** Returns the visible user switcher chip, or fails if it's not visible. */
    public UserSwitcherChip getUserSwitcherChip() {
        return new UserSwitcherChip();
    }

    /** Gets the icons object on the right hand side StatusBar. This is for screenshot test. */
    public Rect getBoundsOfRightHandSideStatusBarIconsForScreenshot() {
        return DeviceHelpers.INSTANCE
                .waitForObj(
                        /* UiDevice= */ getUiDevice(),
                        /* selector= */ sysuiResSelector(STATUS_ICON_CONTAINER_ID),
                        /* timeout= */ SHORT_WAIT,
                        /* errorProvider= */ () ->
                                "StatusBar icons are not found on the right hand side.")
                .getVisibleBounds();
    }

    /** Assert that alarm icon is visible. */
    public void verifyAlarmIconIsVisible() {
        DeviceHelpers.waitForObj(
                By.pkg("com.android.systemui")
                        .clazz("android.widget.ImageView")
                        .descContains(ALARM_ICON_DESC_PREFIX_STRING),
                SHORT_WAIT,
                () -> "Alarm icon should be visible.");
    }

    /** Assert that airplane mode icon is visible. */
    public void verifyAirplaneModeIconIsVisible() {
        assertThat(
                        getUiDevice()
                                .wait(
                                        Until.hasObject(
                                                sysuiResSelector(STATUS_ICON_CONTAINER_ID)
                                                        .hasChild(
                                                                By.desc(AIRPLANE_MODE_ICON_DESC))),
                                        SHORT_WAIT.toMillis()))
                .isTrue();
    }

    /** Assert that data saver icon is visible. */
    public void verifyDataSaverIconIsVisible() {
        assertThat(
                        getUiDevice()
                                .wait(
                                        Until.hasObject(
                                                sysuiResSelector(STATUS_ICON_CONTAINER_ID)
                                                        .hasChild(By.desc(DATA_SAVER_ICON_DESC))),
                                        SHORT_WAIT.toMillis()))
                .isTrue();
    }

    /** Assert that dock defend icon is visible. */
    public void verifyDockDefendIconIsVisible() {
        assertWithMessage("Dock Defend icon should be visible in status bar.")
                .that(
                        getUiDevice()
                                .wait(
                                        Until.hasObject(
                                                sysuiResSelector(UI_SYSTEM_ICONS_ID)
                                                        .hasChild(
                                                                By.descContains(
                                                                        DOCK_DEFEND_ICON_SUFFIX_STRING))),
                                        SHORT_WAIT.toMillis()))
                .isTrue();
    }

    /** Asserts that user switcher chip is invisible. */
    public void assertUserSwitcherChipIsInvisible() {
        DeviceHelpers.INSTANCE.assertInvisible(
                sysuiResSelector(UserSwitcherChip.USER_SWITCHER_CONTAINER_ID),
                SHORT_WAIT,
                () -> "User switcher chip should be invisible in status bar.");
    }

    /** Returns the clock time value on StatusBar. Experimental. */
    public String getClockTime() {
        UiObject2 clockTime =
                DeviceHelpers.INSTANCE.waitForObj(
                        /* UiDevice= */ getUiDevice(),
                        /* selector= */ sysuiResSelector(CLOCK_ID),
                        /* timeout= */ SHORT_WAIT,
                        /* errorProvider= */ () -> "Clock not found.");
        return clockTime.getText();
    }

    /** Returns the position of views in StatusBar. */
    public Set<UnfoldAnimationTestingUtils.Icon> getStatusBarViewPositions() {
        Set<UnfoldAnimationTestingUtils.Icon> statusBarViewPositions = new HashSet<>();
        mStatusBarViewIds.forEach(
                viewId -> {
                    UiObject2 viewUiObject =
                            DeviceHelpers.INSTANCE.waitForNullableObj(
                                    sysuiResSelector(viewId), SHORT_WAIT);
                    if (viewUiObject != null) {
                        Rect iconPosition = viewUiObject.getVisibleBounds();
                        statusBarViewPositions.add(
                                new UnfoldAnimationTestingUtils.Icon(
                                        viewId,
                                        new Point(iconPosition.centerX(), iconPosition.centerY())));
                    }
                });
        return statusBarViewPositions;
    }

    /** Assert that DND icon is visible. */
    public void verifyDndIconIsVisible() {
        assertWithMessage("DND icon should be visible in status bar.")
                .that(
                        getUiDevice()
                                .wait(
                                        Until.hasObject(
                                                sysuiResSelector(STATUS_ICON_CONTAINER_ID)
                                                        .hasChild(By.descContains(DND_ICON_DESC))),
                                        SHORT_WAIT.toMillis()))
                .isTrue();
    }

    /** Returns the value of the battery level on StatusBar. Experimental. */
    public String getBatteryLevel() {
        UiObject2 batteryPercentage =
                DeviceHelpers.INSTANCE.waitForObj(
                        /* UiDevice= */ getUiDevice(),
                        /* selector= */ sysuiResSelector(BATTERY_LEVEL_TEXT_ID),
                        /* timeout= */ LONG_WAIT,
                        /* errorProvider= */ () -> "Battery percentage not found.");
        return batteryPercentage.getText();
    }

    /** Assert that WiFi icon is visible. Experimental. */
    public void verifyWifiIconIsVisible() {
        DeviceHelpers.INSTANCE.assertVisible(
                sysuiResSelector(UI_SYSTEM_ICONS_ID).hasDescendant(sysuiResSelector(WIFI_ICON_ID)),
                LONG_WAIT,
                () -> "WiFi icon should be visible in status bar.");
    }

    /** Assert that silent icon is visible. */
    public void verifySilentIconIsVisible() {
        DeviceHelpers.INSTANCE.assertVisible(
                sysuiResSelector(STATUS_ICON_CONTAINER_ID)
                        .hasChild(By.descContains(SILENT_ICON_DESC_PREFIX_STRING)),
                LONG_WAIT,
                () -> "Silent icon should be visible in status bar.");
    }

    /** Assert that the screen record chip is visible. */
    public void verifyScreenRecordChipIsVisible() {
        DeviceHelpers.INSTANCE.assertVisible(
                sysuiResSelector(ONGOING_ACTIVITY_CHIP_ICON_ID)
                        .hasDescendant(By.descContains(SCREEN_RECORD_DESC_STRING)),
                LONG_WAIT,
                () -> "Recording chip should be visible in status bar.");
    }

    /** Assert there is at least one status icon visible. */
    public void verifyAtLeastOneStatusIconIsVisible() {
        UiObject2 statusBar = DeviceHelpers.waitForObj(sysuiResSelector(STATUS_ICON_CONTAINER_ID));
        assertWithMessage("Status bar should have at least one icon visible")
                .that(statusBar.getChildCount())
                .isGreaterThan(0);
    }

    /** Verifies visibility of the vibrate icon. */
    public void assertVibrateIconVisibility(boolean visible) {
        DeviceHelpers.INSTANCE.assertVisibility(
                /* UiDevice= */ getUiDevice(),
                /* selector= */ sysuiResSelector(STATUS_ICON_CONTAINER_ID)
                        .hasChild(By.descContains(VIBRATE_ICON_DESC_PREFIX_STRING)),
                /* visible= */ visible,
                /* timeout= */ LONG_WAIT);
    }

    /**
     * Fails when {@link #getNotificationIconCount()} doesn't become the expected value within a
     * timeout.
     */
    public void assertNotificationIconCount(int expected) {
        ensureThat(
                "Visible StatusBar icon count should be " + expected,
                () -> getNotificationIconCount() == expected);
    }
}
