import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.kapt")
}

// ---- release 签名 ----
// 取值优先级：仓库根目录 keystore.properties（本机与 CI 通用，不进库）
//            → Gradle 属性 RELEASE_STORE_FILE / RELEASE_STORE_PASSWORD / RELEASE_KEY_ALIAS / RELEASE_KEY_PASSWORD（本机放 ~/.gradle/gradle.properties）
// 本仓库不含任何机器路径；都拿不到时 release 构建回退 debug 签名（仅适合自测，日志会告警）。
val releaseKeystoreProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val releaseProp: (String, String) -> String? = { keystoreKey, gradleKey ->
    releaseKeystoreProps.getProperty(keystoreKey)?.trim()?.takeIf { it.isNotEmpty() }
        ?: providers.gradleProperty(gradleKey).orNull?.trim()?.takeIf { it.isNotEmpty() }
}
val releaseStoreFilePath = releaseProp("storeFile", "RELEASE_STORE_FILE")
val releaseStoreFile = releaseStoreFilePath?.let { rootProject.file(it) }?.takeIf { it.exists() }
val releaseStorePassword = releaseProp("storePassword", "RELEASE_STORE_PASSWORD")
val releaseKeyAlias = releaseProp("keyAlias", "RELEASE_KEY_ALIAS") ?: "rakor"
val releaseKeyPassword = releaseProp("keyPassword", "RELEASE_KEY_PASSWORD")
val hasReleaseSigning = releaseStoreFile != null && releaseStorePassword != null && releaseKeyPassword != null
if (!hasReleaseSigning) {
    logger.warn("WARNING: 未找到可用的 release 签名（keystore.properties 或 RELEASE_* 属性），release 包将回退 debug 签名，仅适合自测")
}

android {
    namespace = "com.rakor.sanzijing"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rakor.sanzijing"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        // 缺签名信息时不创建该配置，release 构建直接复用 debug 签名
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }
    buildTypes {
        release {
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            isMinifyEnabled = true   // R8 压缩：老设备解释执行下 dex 越大首启越慢
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true   // java.time API 26 起才有；小米盒子4 是 API 23，靠脱糖支持
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    androidResources { noCompress += "mp3" }   // MediaPlayer openFd 要求 assets 音频不压缩
    testOptions {
        unitTests {
            isIncludeAndroidResources = true   // Robolectric
            all { t ->
                t.systemProperty("http.proxyHost", "127.0.0.1")
                t.systemProperty("http.proxyPort", "9999")
                t.systemProperty("https.proxyHost", "127.0.0.1")
                t.systemProperty("https.proxyPort", "9999")
            }
        }
    }
}

// 产物命名：sanzijing-v<versionName>.apk（release）；debug 追加 -debug 后缀
// 例：sanzijing-v1.0.apk / sanzijing-v1.0-debug.apk，避免默认的 app-release.apk
androidComponents {
    onVariants { variant ->
        val buildTypeName = variant.buildType ?: "release"
        val suffix = if (buildTypeName == "release") "" else "-$buildTypeName"
        variant.outputs.forEach { output ->
            val impl = output as com.android.build.api.variant.impl.VariantOutputImpl
            val version = output.versionName?.orNull?.replace(Regex("[^0-9A-Za-z._-]"), "-") ?: "unversioned"
            impl.outputFileName.set("sanzijing-v$version$suffix.apk")
        }
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    val composeBom = platform("androidx.compose:compose-bom:2026.02.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")   // MainActivity 使用 material3.Text；tv-material 1.1.0 不传递 material3
    implementation("androidx.tv:tv-material:1.1.0")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    kapt("androidx.room:room-compiler:2.7.2")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    testImplementation(kotlin("test"))   // kotlin.test 断言 API（stdlib 不含，需显式引入）
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.room:room-testing:2.7.2")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
