plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.swvd.simplewebvideodownloader"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.swvd.simplewebvideodownloader"
        minSdk = 24
        targetSdk = 35
        versionCode = 61
        versionName = "6.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // AAB 설정 - Phase 3 FFmpeg 통합 시 활성화 예정
        // ndk {
        //     abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        // }
    }
    
    // AAB 빌드 설정 - Phase 3에서 FFmpeg와 함께 활성화 예정
    // bundle {
    //     language { enableSplit = true }
    //     density { enableSplit = true }
    //     abi { enableSplit = true }
    // }

    buildTypes {
        release {
            isMinifyEnabled = true
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // HTML 파싱을 위한 Jsoup 라이브러리 추가
    implementation("org.jsoup:jsoup:1.17.2")

    // FFmpeg - Phase 3에서 추가 예정 (현재 MVP에서는 제외)
    // implementation("com.arthenica:ffmpeg-kit-min-gpl:4.5.1")

    // 테스트를 위한 라이브러리들
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    // 코루틴 지원
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}