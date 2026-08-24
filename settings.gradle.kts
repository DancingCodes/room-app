// Gradle 在开始构建前，需要先知道从哪里下载 Android、Kotlin 等构建插件。
pluginManagement {
    repositories {
        google() // Android Gradle Plugin 的下载地址。
        gradlePluginPortal() // Kotlin 等其他 Gradle 插件的下载地址。
    }
}

// 将 app/ 目录作为 Android 应用模块加入构建；没有这一行，Gradle 不会构建 APK。
include(":app")
