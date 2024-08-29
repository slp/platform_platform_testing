plugins {
    // my sts sdk plugin
    id("com.android.sts.ndktest")
}

nativeTest {
    minSdk = 33
    targetSdk = 33
    compileSdk = 33
}
