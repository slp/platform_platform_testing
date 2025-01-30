platform_tests += \
    ActivityManagerPerfTests \
    ActivityManagerPerfTestsStubApp1 \
    ActivityManagerPerfTestsStubApp2 \
    ActivityManagerPerfTestsStubApp3 \
    ActivityManagerPerfTestsTestApp \
    AdServicesScenarioTests \
    AndroidAutomotiveDialScenarioTests \
    AndroidTVJankTests \
    AndroidXComposeStartupApp \
    ApiDemos \
    AppCompatibilityTest \
    AppLaunch \
    AppTransitionTests \
    BackgroundDexOptServiceIntegrationTests \
    BandwidthEnforcementTest \
    BandwidthTests \
    benchmarks \
    BootHelperApp \
    BusinessCard \
    CalendarTests \
    camera_client_test \
    camera_metadata_tests \
    CellBroadcastReceiverTests \
    ConnectivityUIDTest \
    CtsCameraTestCases \
    Development \
    DeviceHealthChecks \
    DialerJankTests \
    DynamicCodeLoggerIntegrationTests \
    FacebookAppsScenarioTests \
    flatland \
    FlickerTestApp \
    FrameworkPerf \
    FrameworkPermissionTests \
    FrameworksCoreSystemPropertiesTests \
    FrameworksCoreTests \
    FrameworksMockingCoreTests \
    FrameworksPrivacyLibraryTests \
    FrameworksSaxTests \
    FrameworksServicesTests \
    FrameworksUtilTests \
    FrameworkTestRunnerTests \
    hwuimacro \
    ImageProcessing \
    JankMicroBenchmarkTests \
    LauncherIconsApp \
    long_trace_binder_config.textproto \
    long_trace_config.textproto \
    mediaframeworktest \
    MemoryUsage \
    mmapPerf \
    OverviewFunctionalTests \
    perfetto_trace_processor_shell \
    PerformanceAppTest \
    PerformanceLaunch \
    PermissionFunctionalTests \
    PermissionTestAppMV1 \
    PermissionUtils \
    PlatformCommonScenarioTests \
    PlatformComposeSceneTransitionLayoutDemo \
    PMC \
    PowerPerfTest \
    SdkSandboxClient \
    SdkSandboxCodeProvider \
    SdkSandboxMediateeProvider \
    SdkSandboxPerfScenarioTests \
    SdkSandboxWebViewProvider \
    SettingsUITests \
    SimpleTestApp \
    sl4a \
    SmokeTest \
    SmokeTestApp \
    StubIME \
    trace_config.textproto \
    trace_config_boot_time.textproto \
    trace_config_boot_time_stop.textproto \
    trace_config_detailed.textproto \
    trace_config_detailed_heapdump.textproto \
    trace_config_experimental.textproto \
    trace_config_multi_user_cuj_tests.textproto \
    trace_config_post_boot.textproto \
    trace_config_power.textproto \
    UbSystemUiJankTests \
    UbWebViewJankTests \
    UiBench \
    UiBenchJankTests \
    UiBenchJankTestsWear \
    UiBenchMicrobenchmark \
    uwb_snippet \
    wifi_direct_mobly_snippet \
    wifi_aware_snippet_new \
    WifiStrengthScannerUtil \
    xaVideoDecoderCapabilities \

ifneq ($(strip $(BOARD_PERFSETUP_SCRIPT)),)
platform_tests += perf-setup
endif

ifneq ($(filter vsoc_arm vsoc_arm64 vsoc_x86 vsoc_x86_64, $(TARGET_BOARD_PLATFORM)),)
  platform_tests += \
    CuttlefishRilTests \
    CuttlefishWifiTests
endif

ifeq ($(HOST_OS),linux)
platform_tests += root-canal
endif
