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
        
        // AAB를 위한 필수 설정
        ndk {
            // 지원할 CPU 아키텍처 명시
            abiFilters += listOf(
                "arm64-v8a",      // 최신 64비트 기기 (필수)
                "armeabi-v7a",    // 구형 32비트 기기
                "x86_64"          // 에뮬레이터 테스트용
            )
        }
    }
    
    // AAB 빌드 설정
    bundle {
        language {
            // 언어 리소스 분할 (선택사항)
            enableSplit = true
        }
        density {
            // 화면 밀도별 리소스 분할
            enableSplit = true
        }
        abi {
            // CPU 아키텍처별 분할 (핵심!)
            enableSplit = true
        }
    }

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
    
    // Mobile FFmpeg 최소 빌드 (HLS 지원)
    implementation("com.arthenica:mobile-ffmpeg-min-gpl:4.4.LTS")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}