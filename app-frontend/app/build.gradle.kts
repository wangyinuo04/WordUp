// 模块级构建配置文件
plugins {
    alias(libs.plugins.android.application)
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
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

    // 测试依赖
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // 直接引入的第三方库
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}