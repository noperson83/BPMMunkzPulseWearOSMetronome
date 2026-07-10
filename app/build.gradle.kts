import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val releaseKeystorePropertiesFile = rootProject.file("keystore.properties")
val releaseKeystoreProperties = Properties().apply {
    if (releaseKeystorePropertiesFile.exists()) {
        releaseKeystorePropertiesFile.inputStream().use(::load)
    }
}
val hasReleaseSigning = listOf(
    "storeFile",
    "storePassword",
    "keyAlias",
    "keyPassword",
).all(releaseKeystoreProperties::containsKey)

android {
    namespace = "bpm.munkz.pulse_wear.os.bpm"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

    }

    flavorDimensions += "edition"
    productFlavors {
        create("bpm") {
            dimension = "edition"
            applicationId = "bpm.munkz.pulse_wear.os.metronome"
            versionCode = 6
            buildConfigField("String", "APP_EDITION", "\"bpm\"")
        }
        create("phonebpm") {
            dimension = "edition"
            applicationId = "bpm.munkz.pulse.phone.metronome"
            versionCode = 1
            versionNameSuffix = "-phone"
            buildConfigField("String", "APP_EDITION", "\"phonebpm\"")
        }
        create("phonetune") {
            dimension = "edition"
            applicationId = "bpm.munkz.pulse.phone.tuner"
            versionCode = 1
            versionNameSuffix = "-phone-tune"
            buildConfigField("String", "APP_EDITION", "\"phonetune\"")
        }
        create("phonerhythm") {
            dimension = "edition"
            applicationId = "bpm.munkz.pulse.phone.rhythm"
            versionCode = 1
            versionNameSuffix = "-phone-rhythm"
            buildConfigField("String", "APP_EDITION", "\"phonerhythm\"")
        }
        create("phoneplaylist") {
            dimension = "edition"
            applicationId = "bpm.munkz.pulse.phone.playlist"
            versionCode = 1
            versionNameSuffix = "-phone-playlist"
            buildConfigField("String", "APP_EDITION", "\"phoneplaylist\"")
        }
        create("phonepro") {
            dimension = "edition"
            applicationId = "bpm.munkz.pulse.phone.pro"
            versionCode = 1
            versionNameSuffix = "-phone-pro"
            buildConfigField("String", "APP_EDITION", "\"phonepro\"")
        }
        create("phonebeatmachine") {
            dimension = "edition"
            applicationId = "bpm.munkz.pulse.phone.beatmachine"
            versionCode = 1
            versionNameSuffix = "-phone-beatmachine"
            buildConfigField("String", "APP_EDITION", "\"phonebeatmachine\"")
        }
        create("tune") {
            dimension = "edition"
            applicationId = "bpm.munkz.pulse_wear.os.tuner"
            versionCode = 10
            versionNameSuffix = "-tune"
            buildConfigField("String", "APP_EDITION", "\"tune\"")
        }
        create("rhythm") {
            dimension = "edition"
            applicationId = "bpm.munkz.pulse_wear.os.rhythm"
            versionCode = 6
            versionNameSuffix = "-rhythm"
            buildConfigField("String", "APP_EDITION", "\"rhythm\"")
        }
        create("playlist") {
            dimension = "edition"
            applicationId = "bpm.munkz.pulse_wear.os.playlist"
            versionCode = 10
            buildConfigField("String", "APP_EDITION", "\"playlist\"")
        }
        create("pro") {
            dimension = "edition"
            applicationId = "bpm.munkz.pulse_wear.os.pro"
            versionCode = 8
            versionNameSuffix = "-pro"
            buildConfigField("String", "APP_EDITION", "\"pro\"")
        }
        create("hearnoevil") {
            dimension = "edition"
            applicationId = "bpm.munkz.pulse_wear.os.hearnoevil"
            versionNameSuffix = "-hearnoevil"
            buildConfigField("String", "APP_EDITION", "\"hearnoevil\"")
        }
        create("beatmachine") {
            dimension = "edition"
            applicationId = "bpm.munkz.pulse_wear.os.beatmachine"
            versionCode = 1
            versionNameSuffix = "-beatmachine"
            buildConfigField("String", "APP_EDITION", "\"beatmachine\"")
        }
        create("fidgettoy") {
            dimension = "edition"
            applicationId = "bpm.munkz.pulse_wear.os.fidgettoy"
            versionCode = 3
            versionName = "1.2"
            versionNameSuffix = "-fidgettoy"
            buildConfigField("String", "APP_EDITION", "\"fidgettoy\"")
        }
        create("fidgetphone") {
            dimension = "edition"
            applicationId = "bpm.munkz.fidgettoy.phone"
            versionCode = 1
            versionName = "1.0"
            versionNameSuffix = "-fidgetphone"
            buildConfigField("String", "APP_EDITION", "\"fidgettoy\"")
        }
    }

    sourceSets {
        getByName("phonebpm") {
            res.srcDir("src/bpm/res")
        }
        getByName("phonetune") {
            res.srcDir("src/tune/res")
        }
        getByName("phonerhythm") {
            res.srcDir("src/rhythm/res")
        }
        getByName("phoneplaylist") {
            res.srcDir("src/playlist/res")
        }
        getByName("phonepro") {
            res.srcDir("src/pro/res")
        }
        getByName("phonebeatmachine") {
            res.srcDir("src/beatmachine/res")
        }
        getByName("fidgetphone") {
            res.srcDir("src/fidgettoy/res")
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseKeystoreProperties["storeFile"] as String)
                storePassword = releaseKeystoreProperties["storePassword"] as String
                keyAlias = releaseKeystoreProperties["keyAlias"] as String
                keyPassword = releaseKeystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    useLibrary("wear-sdk")
    buildFeatures {
        compose = true
        buildConfig = true
    }
    buildToolsVersion = "36.0.0"
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.core.splashscreen)
    implementation(libs.play.services.wearable)
    implementation(libs.play.billing)
    implementation(libs.wear.ongoing)
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.wear.tooling.preview)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.test.manifest)
    debugImplementation(libs.ui.tooling)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling)
    implementation(libs.wear.tiles)
    implementation(libs.wear.protolayout)
    implementation(libs.wear.protolayout.material3)
    implementation(libs.wear.protolayout.expression)
    implementation(libs.wear.watchface.complications.data.source)
    testImplementation("junit:junit:4.13.2")
    debugImplementation(libs.wear.tiles.renderer)
}
