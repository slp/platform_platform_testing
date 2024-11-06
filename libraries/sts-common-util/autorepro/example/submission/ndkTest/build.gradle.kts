plugins {
    id("com.android.security.autorepro.ndktest")
}

nativeTest {
    minSdk = 33
    targetSdk = 33
    compileSdk = 33
}
