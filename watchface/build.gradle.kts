plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.bpmmunkzface"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.bpmmunkzface"
        minSdk = 36
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
}
