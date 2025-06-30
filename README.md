# 🎬 Simple Web Video Downloader

Android 웹 비디오 다운로더 앱으로, 웹 페이지에서 MP4 비디오를 자동으로 감지하고 다운로드할 수 있는 사용자 친화적인 인터페이스를 제공합니다.

## ✨ 주요 기능

- 🌐 **내장 웹 브라우저**: 완전한 웹 브라우징 경험
- 🔍 **자동 MP4 감지**: 페이지의 모든 MP4 비디오 링크를 자동으로 탐지
- ⬇️ **간편한 다운로드**: 원클릭으로 비디오 다운로드
- 📱 **반응형 UI**: 다양한 Android 기기에서 최적화된 사용자 경험
- 🎯 **전체화면 모드**: 몰입형 브라우징 경험
- 📜 **브라우징 히스토리**: 최근 방문한 URL 관리
- 🎨 **Material Design 3**: 현대적이고 직관적인 UI

## 📱 스크린샷

> 스크린샷은 추후 추가 예정

## 🛠️ 기술 스택

- **언어**: Kotlin
- **UI 프레임워크**: Jetpack Compose
- **아키텍처**: MVVM with Compose State Management
- **웹뷰**: Android WebView with JavaScript injection
- **HTML 파싱**: Jsoup
- **최소 SDK**: API 24 (Android 7.0)
- **타겟 SDK**: API 35 (Android 15)

## 📋 요구사항

- Android 7.0 (API 24) 이상
- 인터넷 연결
- 저장소 권한 (다운로드용)

## 🚀 설치 방법

### APK 다운로드 (추후 제공)
1. [Releases](https://github.com/JaemyeongBae/SimpleWebVideoDownloader/releases) 페이지에서 최신 APK 다운로드
2. Android 기기에서 "알 수 없는 소스" 허용
3. APK 파일 설치

### 소스 코드에서 빌드
```bash
# 저장소 복제
git clone https://github.com/JaemyeongBae/SimpleWebVideoDownloader.git

# 프로젝트 디렉토리로 이동
cd SimpleWebVideoDownloader

# Android Studio에서 프로젝트 열기
# 또는 명령줄에서 빌드
./gradlew assembleDebug
```

## 🎯 사용 방법

1. **URL 입력**: 상단 URL 입력창에 웹사이트 주소 입력
2. **페이지 탐색**: 내장 브라우저로 원하는 페이지 방문
3. **비디오 감지**: 앱이 자동으로 MP4 비디오 링크 탐지
4. **다운로드**: 감지된 비디오 목록에서 원하는 비디오 다운로드
5. **전체화면**: 전체화면 모드로 더 나은 브라우징 경험

## 🔧 개발 환경 설정

### 필수 도구
- Android Studio Hedgehog | 2023.1.1 이상
- JDK 11 이상
- Android SDK 35

### 프로젝트 설정
```bash
# 의존성 동기화
./gradlew sync

# 디버그 빌드
./gradlew assembleDebug

# 릴리즈 빌드
./gradlew assembleRelease
```

## 📁 프로젝트 구조

```
app/
├── src/main/
│   ├── java/com/swvd/simplewebvideodownloader/
│   │   ├── MainActivity.kt          # 메인 액티비티
│   │   └── ui/theme/               # UI 테마 설정
│   ├── res/                        # 리소스 파일
│   └── AndroidManifest.xml         # 앱 매니페스트
├── build.gradle.kts                # 앱 레벨 빌드 설정
└── proguard-rules.pro             # ProGuard 설정
```

## 🤝 기여하기

프로젝트에 기여해주셔서 감사합니다! 다음 단계를 따라주세요:

1. 이 저장소를 Fork합니다
2. 새로운 기능 브랜치를 생성합니다 (`git checkout -b feature/AmazingFeature`)
3. 변경사항을 커밋합니다 (`git commit -m 'Add some AmazingFeature'`)
4. 브랜치에 Push합니다 (`git push origin feature/AmazingFeature`)
5. Pull Request를 생성합니다

### 개발 가이드라인
- Kotlin 코딩 컨벤션 준수
- Jetpack Compose 베스트 프랙티스 적용
- 모든 새 기능에 대한 테스트 작성
- 커밋 메시지는 명확하고 설명적으로 작성

## 📝 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 자세한 내용은 [LICENSE](LICENSE) 파일을 참조하세요.

## 🐛 버그 리포트 및 기능 요청

버그를 발견하거나 새로운 기능을 제안하고 싶으시면 [Issues](https://github.com/JaemyeongBae/SimpleWebVideoDownloader/issues) 페이지에서 새로운 이슈를 생성해주세요.

### 버그 리포트 시 포함할 정보
- Android 버전
- 기기 모델
- 앱 버전
- 재현 단계
- 예상 동작 vs 실제 동작

## 📞 연락처

프로젝트에 대한 질문이 있으시면 언제든지 연락주세요:

- GitHub: [@JaemyeongBae](https://github.com/JaemyeongBae)
- 이슈: [GitHub Issues](https://github.com/JaemyeongBae/SimpleWebVideoDownloader/issues)

## 🙏 감사의 말

- [Jsoup](https://jsoup.org/) - HTML 파싱 라이브러리
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - 현대적인 Android UI 툴킷
- [Material Design 3](https://m3.material.io/) - 디자인 시스템

---

⭐ 이 프로젝트가 유용하다면 별표를 눌러주세요! 