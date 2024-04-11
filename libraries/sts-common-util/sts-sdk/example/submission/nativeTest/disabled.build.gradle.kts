plugins {
    // my sts sdk plugin
    id("com.android.sts.sdk.nativetest")
}

nativeTest {
    name = "myapp"
    minSdk = 11
    targetSdk = 33
    compileSdk = 33
    srcs = listOf("src")
    includeDirs = listOf("foo/include", "bar/include", "foobar/include")
    cflags = listOf("-lm")
}
