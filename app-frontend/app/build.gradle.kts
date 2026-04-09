// 模块级构建配置文件
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.wordup"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.wordup"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1" // 请与您新建的独立项目中的版本号保持一致
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
configurations.all {
    exclude(group = "com.google.guava", module = "listenablefuture")
}

dependencies {
    // 使用版本目录 (Version Catalog) 引入的依赖
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.volley)
    implementation(libs.androidx.tracing.perfetto.handshake)
    implementation(libs.androidx.compiler)
    implementation(libs.cronet.embedded)
    implementation(libs.core.ktx)

    // 测试依赖
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // 直接引入的第三方库
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Compose 核心依赖
    val composeBom = platform("androidx.compose:compose-bom:2024.04.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Compose 与 View 的桥接依赖
    implementation("androidx.activity:activity-compose:1.8.2")

    // OkHttp 网络请求与 Gson 数据解析
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // ViewModel 与 Compose 的桥接支持
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    // 1. WordUp AI 核心 SDK
    implementation(files("libs/wordup-ai-core-release.aar"))

    // 2. 第三方算法框架依赖
    implementation("com.google.mediapipe:tasks-vision:0.10.0")
    implementation("org.tensorflow:tensorflow-lite:2.14.0")

    // 3. CameraX 官方依赖
    val cameraxVersion = "1.3.0"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")


}