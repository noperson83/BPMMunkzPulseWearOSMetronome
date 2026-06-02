plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "bpm.munkz.pulse_wear.os.watchface"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "bpm.munkz.pulse_wear.os.watchface"
        minSdk = 33
        targetSdk = 36
        versionCode = 2
        versionName = "1.0"
    }
}
