plugins {
    // 三个插件的用途和版本在 gradle/libs.versions.toml 中说明；删除后 Android、Compose 或序列化代码无法编译。
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

repositories {
    // AndroidX、Compose 和声网等依赖从 Google Maven 下载；删除后这些依赖无法解析。
    google()
    // Retrofit、OkHttp、Coil 等第三方依赖从 Maven Central 下载；删除后网络和图片依赖无法解析。
    mavenCentral()
}

android {
    // Kotlin 包名空间，用于生成 R、BuildConfig 等代码；删除后 Android 构建无法确定生成代码的包名。
    namespace = "love.moonc.room"
    // 使用 Android API 37 编译；删除后 Gradle 无法确定可用的 Android API。
    compileSdk = 37

    defaultConfig {
        // 安装包唯一标识，也是 FileProvider 等系统配置的基础；删除后无法生成有效 APK。
        applicationId = "love.moonc.room"
        // 支持 Android 8.0 及以上设备；提高该值会缩小可安装设备范围。
        minSdk = 26
        // 按 Android 16 行为规范运行；删除后 Gradle 使用默认目标版本，系统兼容行为可能改变。
        targetSdk = 37
        // 更新比较使用的递增内部版本号；不递增时系统不会将新 APK 视为更新。
        versionCode = 1
        // 展示给用户的版本名称；删除后安装包不再有明确的显示版本。
        versionName = "1.0"
    }

    buildTypes {
        release {
            // Release APK 只包含 64 位 ARM 原生库；删除后会为更多 CPU 架构打包，安装包会变大。
            ndk {
                //noinspection ChromeOsAbiSupport
                abiFilters += "arm64-v8a"
            }
            // 移除 Release 中未使用的代码；关闭后 APK 会变大。
            isMinifyEnabled = true
            // 在代码压缩后移除未使用资源；关闭后 APK 会保留无用资源。
            isShrinkResources = true
            // 使用 Android 默认优化规则；删除后 Release 的代码压缩规则会退回默认行为。
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    compileOptions {
        // 以 Java 11 语言级别编译 Java 代码；删除后使用默认级别，语言兼容范围可能改变。
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        // 启用 Compose UI；关闭后所有 @Composable 页面无法编译。
        compose = true
        // 生成 BuildConfig；关闭后更新比较和 Debug 网络日志无法编译。
        buildConfig = true
    }
}

dependencies {
    // 每项依赖的用途和删除影响见 gradle/libs.versions.toml，避免在两个文件重复维护相同说明。
    implementation(libs.androidx.core)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.coil.compose)
    implementation(libs.agora.rtc)
}
