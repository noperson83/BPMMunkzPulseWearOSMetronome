// Top-level build file where you can add configuration options common to all subprojects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

tasks.register("installDebugWithWatchFace") {
    group = "install"
    description = "Installs the BPM Munkz app and the separate BPM Munkz watch-face package."
    dependsOn(":app:installDebug", ":watchface:installDebug")
}

gradle.projectsEvaluated {
    tasks.findByPath(":app:installDebug")?.finalizedBy(":watchface:installDebug")
}
