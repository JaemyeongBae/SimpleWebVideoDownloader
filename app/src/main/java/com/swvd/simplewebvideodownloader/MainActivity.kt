package com.swvd.simplewebvideodownloader

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.activity.compose.BackHandler
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import java.util.UUID

// 간단한 탭 데이터 클래스
data class Tab(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "새 탭",
    var url: String = "https://www.google.com"
)

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Toast.makeText(this, "다운로드하려면 저장소 권한이 필요합니다", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 더 강력한 시스템 바 처리
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                ) {
                    MainScreen(
                        onRequestPermissions = { requestStoragePermissions() },
                        onDownloadFile = { url, filename -> downloadFile(url, filename) },
                        onFullscreenModeChange = { isFullscreen -> setFullscreenMode(isFullscreen) }
                    )
                }
            }
        }
    }

    private fun requestStoragePermissions() {
        val permissions = mutableListOf<String>()

        // Android 10 (API 29) 이하에서만 WRITE_EXTERNAL_STORAGE 권한 필요
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun downloadFile(url: String, filename: String) {
        try {
            // 파일명에서 특수문자 제거
            val safeFilename = filename.replace(Regex("[^a-zA-Z0-9._-]"), "_")

            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("비디오 다운로드")
                .setDescription("$safeFilename 다운로드 중...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            // Android 10 이상에서는 Downloads 폴더에 저장
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, safeFilename)
            } else {
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, safeFilename)
            }

            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)

            Toast.makeText(this, "다운로드가 시작되었습니다\n파일: $safeFilename\nDownloads 폴더에 저장됩니다", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Toast.makeText(this, "다운로드 실패: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // 전체화면 몰입형 모드 설정
    private fun setFullscreenMode(enable: Boolean) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (enable) {
                // Android 11+ 에서 몰입형 모드
                window.insetsController?.let { controller ->
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                // 몰입형 모드 해제
                window.insetsController?.show(WindowInsetsCompat.Type.systemBars())
            }
        } else {
            @Suppress("DEPRECATION")
            if (enable) {
                // Android 10 이하에서 몰입형 모드
                window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
            } else {
                // 몰입형 모드 해제
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onRequestPermissions: () -> Unit,
    onDownloadFile: (String, String) -> Unit,
    onFullscreenModeChange: (Boolean) -> Unit
) {
    // 탭 관련 상태
    var tabs by remember { mutableStateOf(listOf(Tab())) }
    var currentTabIndex by remember { mutableIntStateOf(0) }
    var showTabOverview by remember { mutableStateOf(false) }
    
    var urlText by remember { mutableStateOf("") }
    var currentUrl by remember { mutableStateOf("") }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var fullscreenWebView by remember { mutableStateOf<WebView?>(null) }
    var mp4Links by remember { mutableStateOf<List<String>>(emptyList()) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var hasAnalyzed by remember { mutableStateOf(false) }
    var downloadingUrls by remember { mutableStateOf<Set<String>>(emptySet()) }
    var urlSectionExpanded by remember { mutableStateOf(true) }
    
    // 다운로드 결과 알림 상태
    var downloadResultMessage by remember { mutableStateOf<String?>(null) }
    var showDownloadResult by remember { mutableStateOf(false) }
    var mp4SectionExpanded by remember { mutableStateOf(true) }
    var isWebViewFullscreen by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var webViewState by remember { mutableStateOf<Bundle?>(null) }
    var urlHistory by remember { mutableStateOf<List<String>>(emptyList()) }
    var showMp4List by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val currentTab = if (tabs.isNotEmpty() && currentTabIndex in tabs.indices) tabs[currentTabIndex] else null

    // 탭 관리 함수들
    fun addNewTab() {
        val newTab = Tab()
        tabs = tabs + newTab
        currentTabIndex = tabs.size - 1
    }
    
    fun closeTab(index: Int) {
        if (tabs.size <= 1) return // 최소 1개 탭 유지
        
        tabs = tabs.filterIndexed { i, _ -> i != index }
        if (index <= currentTabIndex && currentTabIndex > 0) {
            currentTabIndex--
        }
        if (currentTabIndex >= tabs.size) {
            currentTabIndex = tabs.size - 1
        }
    }
    
    fun switchTab(index: Int) {
        if (index in tabs.indices) {
            currentTabIndex = index
            // 탭 전환시 URL 동기화
            currentTab?.let { tab ->
                urlText = tab.url
                currentUrl = tab.url
            }
        }
    }

    // 네비게이션 상태 업데이트 함수
    fun updateNavigationState() {
        val activeWebView = if (isWebViewFullscreen) fullscreenWebView else webView
        val oldCanGoBack = canGoBack
        val oldCanGoForward = canGoForward
        
        canGoBack = activeWebView?.canGoBack() ?: false
        canGoForward = activeWebView?.canGoForward() ?: false
        
        Log.d("Navigation", "상태 업데이트 - 전체화면: $isWebViewFullscreen")
        Log.d("Navigation", "활성 WebView: ${activeWebView != null}")
        Log.d("Navigation", "뒤로가기: $oldCanGoBack → $canGoBack")
        Log.d("Navigation", "앞으로가기: $oldCanGoForward → $canGoForward")
    }

    // 전체화면 모드에서 뒤로가기 처리 및 상태 동기화
    BackHandler(enabled = isWebViewFullscreen) {
        // 전체화면 WebView의 상태 저장
        fullscreenWebView?.let { fsWebView ->
            val bundle = Bundle()
            fsWebView.saveState(bundle)
            webViewState = bundle
            
            // 현재 URL도 업데이트
            fsWebView.url?.let { url ->
                if (url != currentUrl && 
                    !url.startsWith("data:") && 
                    !url.startsWith("about:") &&
                    url != "about:blank") {
                    currentUrl = url
                    urlText = url
                }
            }
        }
        
        // 전체화면 모드 종료 후 네비게이션 상태 업데이트
        isWebViewFullscreen = false
        
        // 상태 업데이트를 위한 지연 실행
        Handler(Looper.getMainLooper()).postDelayed({
            updateNavigationState()
        }, 100)
    }

    fun analyzePageForMp4() {
        if (isAnalyzing) return
        
        // 현재 활성화된 WebView 사용 (전체화면 모드일 때는 fullscreenWebView 우선)
        val activeWebView = if (isWebViewFullscreen) {
            fullscreenWebView ?: webView
        } else {
            webView ?: fullscreenWebView
        }
        
        if (activeWebView == null) {
            Log.d("WebView", "활성 WebView가 없음")
            return
        }
        
        Log.d("WebView", "MP4 감지 시작: ${activeWebView.url} (전체화면: $isWebViewFullscreen)")
        
        isAnalyzing = true
        hasAnalyzed = true
        activeWebView.evaluateJavascript(
            """
        (function() {
            var results = [];
            var uniqueUrls = new Set();
            
            try {
                // 1. 모든 video 태그의 src 확인
                var videos = document.querySelectorAll('video');
                videos.forEach(function(v) {
                    if (v.src && v.src.includes('.mp4')) {
                        uniqueUrls.add(v.src);
                    }
                    if (v.currentSrc && v.currentSrc.includes('.mp4')) {
                        uniqueUrls.add(v.currentSrc);
                    }
                });
                
                // 2. 모든 source 태그의 src 확인
                var sources = document.querySelectorAll('source');
                sources.forEach(function(s) {
                    if (s.src && s.src.includes('.mp4')) {
                        uniqueUrls.add(s.src);
                    }
                });
                
                // 3. 모든 a 태그의 href 확인 (링크)
                var links = document.querySelectorAll('a[href]');
                links.forEach(function(a) {
                    if (a.href && a.href.includes('.mp4')) {
                        uniqueUrls.add(a.href);
                    }
                });
                
                // 4. 페이지 HTML에서 MP4 URL 정규식 검색
                var html = document.documentElement.outerHTML;
                var mp4Regex = /https?:\/\/[^\s"'<>()]+\.mp4[^\s"'<>()]*/gi;
                var matches = html.match(mp4Regex);
                if (matches) {
                    matches.forEach(function(match) {
                        // URL 정리
                        var cleanUrl = match.replace(/['"<>()]+$/, '');
                        if (cleanUrl.length > 20) { // 너무 짧은 URL 제외
                            uniqueUrls.add(cleanUrl);
                        }
                    });
                }
                
                // 5. 모든 img 태그의 data 속성 확인 (때로는 비디오 썸네일이 data 속성에 있음)
                var imgs = document.querySelectorAll('img[data-src], img[data-url]');
                imgs.forEach(function(img) {
                    var dataSrc = img.getAttribute('data-src') || img.getAttribute('data-url');
                    if (dataSrc && dataSrc.includes('.mp4')) {
                        uniqueUrls.add(dataSrc);
                    }
                });
                
                // 6. 모든 div의 data 속성 확인
                var divs = document.querySelectorAll('div[data-video], div[data-src], div[data-url]');
                divs.forEach(function(div) {
                    var dataVideo = div.getAttribute('data-video') || div.getAttribute('data-src') || div.getAttribute('data-url');
                    if (dataVideo && dataVideo.includes('.mp4')) {
                        uniqueUrls.add(dataVideo);
                    }
                });
                
                // 결과 정리
                uniqueUrls.forEach(function(url) {
                    results.push(url);
                });
                
                return JSON.stringify(results);
                
            } catch (e) {
                return JSON.stringify(['JavaScript 오류: ' + e.message]);
            }
        })();
        """.trimIndent()

        ) { result ->
            try {
                val cleanResult = result?.replace("\\\"", "\"")?.removeSurrounding("\"") ?: "[]"
                Log.d("WebView", "MP4 검색 결과: $cleanResult")

                val videoLinks = if (cleanResult.startsWith("[")) {
                    cleanResult.removeSurrounding("[", "]")
                        .split(",")
                        .map { it.trim().removeSurrounding("\"") }
                        .filter { it.isNotEmpty() && it.contains(".mp4") && !it.contains("JavaScript 오류") }
                } else {
                    emptyList()
                }

                mp4Links = videoLinks
                isAnalyzing = false
                
                Log.d("WebView", "최종 MP4 링크 ${videoLinks.size}개 발견")

            } catch (e: Exception) {
                Log.e("WebView", "MP4 분석 오류: ${e.message}")
                mp4Links = listOf("분석 오류: ${e.message}")
                isAnalyzing = false
            }
        }
    }

    fun loadUrl() {
        if (urlText.isNotBlank()) {
            val url = if (!urlText.startsWith("http://") && !urlText.startsWith("https://")) {
                "https://$urlText"
            } else {
                urlText
            }
            currentUrl = url
            
            // 현재 탭의 URL 업데이트
            currentTab?.let { tab ->
                tabs = tabs.map { if (it.id == tab.id) it.copy(url = url) else it }
            }
            
            // 히스토리에 URL 추가 (중복 제거)
            if (!urlHistory.contains(url)) {
                urlHistory = urlHistory + url
            }
            
            webView?.loadUrl(url)
            keyboardController?.hide()
            urlSectionExpanded = false  // 로드 후 접기
            
            // URL 로드 후 1회 MP4 감지 (1초 딜레이)
            Handler(Looper.getMainLooper()).postDelayed({
                analyzePageForMp4()
            }, 1000)
        }
    }

    fun downloadVideo(url: String) {
        val cleanUrl = url.trim()

        // HTTP/HTTPS URL 확인
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            downloadResultMessage = "다운로드 실패!\n유효하지 않은 URL입니다"
            showDownloadResult = true
            
            // 실패 알림은 3초 후 자동 사라짐
            Handler(Looper.getMainLooper()).postDelayed({
                showDownloadResult = false
                downloadResultMessage = null
            }, 3000)
            return
        }

        // 권한 확인 (Android 10 이하에서만)
        val needsPermission = android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED

        if (needsPermission) {
            downloadResultMessage = "다운로드 실패!\n저장소 권한이 필요합니다"
            showDownloadResult = true
            
            Handler(Looper.getMainLooper()).postDelayed({
                showDownloadResult = false
                downloadResultMessage = null
            }, 3000)
            
            onRequestPermissions()
            return
        }

        // 파일명 생성
        val filename = try {
            val uri = Uri.parse(cleanUrl)
            val pathSegment = uri.lastPathSegment
            when {
                pathSegment != null && pathSegment.contains(".mp4") -> pathSegment
                pathSegment != null -> "${pathSegment}.mp4"
                else -> "video_${System.currentTimeMillis()}.mp4"
            }
        } catch (e: Exception) {
            "video_${System.currentTimeMillis()}.mp4"
        }

        try {
            downloadingUrls = downloadingUrls + cleanUrl
            onDownloadFile(cleanUrl, filename)
            
            // 다운로드 성공 알림
            downloadResultMessage = "다운로드 시작!\n파일: $filename\nDownloads 폴더에 저장됩니다"
            showDownloadResult = true
            
            // 성공 알림은 3초 후 자동 사라짐
            Handler(Looper.getMainLooper()).postDelayed({
                showDownloadResult = false
                downloadResultMessage = null
            }, 3000)
            
        } catch (e: Exception) {
            // 다운로드 실패 시 상태 업데이트 및 에러 알림
            downloadingUrls = downloadingUrls - cleanUrl
            downloadResultMessage = "다운로드 실패!\n오류: ${e.message}"
            showDownloadResult = true
            
            // 실패 알림은 5초 후 자동 사라짐
            Handler(Looper.getMainLooper()).postDelayed({
                showDownloadResult = false
                downloadResultMessage = null
            }, 5000)
            
            Log.e("Download", "다운로드 실패: ${e.message}")
        }
    }

    // URL 변경 및 1회 MP4 감지 함수
    fun updateUrlIfChanged(newUrl: String) {
        if (newUrl != currentUrl && 
            !newUrl.startsWith("data:") && 
            !newUrl.startsWith("about:") &&
            newUrl != "about:blank") {
            currentUrl = newUrl
            urlText = newUrl
            
            // 히스토리에 URL 추가 (중복 제거)
            if (!urlHistory.contains(newUrl)) {
                urlHistory = urlHistory + newUrl
            }
            
            // 네비게이션 상태 업데이트
            updateNavigationState()
            
            // URL 변경 시 1회 MP4 감지 (1초 딜레이)
            Handler(Looper.getMainLooper()).postDelayed({
                analyzePageForMp4()
            }, 1000)
        }
    }

    // 네비게이션 후 URL 동기화 및 1회 MP4 감지
    fun handleNavigation(targetWebView: WebView?) {
        // 즉시 URL 체크 및 네비게이션 상태 업데이트
        targetWebView?.url?.let { newUrl ->
            updateUrlIfChanged(newUrl)
        }
        updateNavigationState()
        
        // 500ms 후 다시 체크 (페이지 로딩 완료 대기)
        Handler(Looper.getMainLooper()).postDelayed({
            targetWebView?.url?.let { newUrl ->
                updateUrlIfChanged(newUrl)
            }
            updateNavigationState()
        }, 500)
        
        // 네비게이션 후 추가 MP4 감지 (페이지 완전 로딩 후)
        Handler(Looper.getMainLooper()).postDelayed({
            analyzePageForMp4()
        }, 2000)
    }

    // 다운로딩 상태 관리
    LaunchedEffect(downloadingUrls) {
        if (downloadingUrls.isNotEmpty()) {
            kotlinx.coroutines.delay(3000)
            downloadingUrls = emptySet()
        }
    }

    // 전체화면 모드 변경 시 네비게이션 상태 업데이트
    LaunchedEffect(isWebViewFullscreen) {
        updateNavigationState()
        // 전체화면 모드 시 몰입형 모드 활성화
        onFullscreenModeChange(isWebViewFullscreen)
        
        // 전체화면 모드 전환 후 추가적인 상태 업데이트 (WebView 로딩 대기)
        kotlinx.coroutines.delay(500)
        updateNavigationState()
        kotlinx.coroutines.delay(1000)
        updateNavigationState()
    }

    // 탭 전환시 URL 동기화
    LaunchedEffect(currentTabIndex) {
        currentTab?.let { tab ->
            urlText = tab.url
            currentUrl = tab.url
        }
    }

    // URL 변경 시 네비게이션 상태 업데이트
    LaunchedEffect(currentUrl) {
        if (currentUrl.isNotEmpty()) {
            kotlinx.coroutines.delay(1000) // 페이지 로딩 대기
            updateNavigationState()
        }
    }

    // 전체화면 모드일 때 새로운 UI
    if (isWebViewFullscreen) {
        FullscreenUI(
            tabs = tabs,
            currentTabIndex = currentTabIndex,
            currentUrl = currentUrl,
            urlText = urlText,
            onUrlTextChange = { urlText = it },
            onLoadUrl = { url ->
                val finalUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    "https://$url"
                } else {
                    url
                }
                currentUrl = finalUrl
                urlText = finalUrl
                
                // 현재 탭의 URL도 업데이트
                currentTab?.let { tab ->
                    tabs = tabs.map {
                        if (it.id == tab.id) it.copy(url = finalUrl)
                        else it
                    }
                }
                
                // 히스토리에 URL 추가 (중복 제거)
                if (!urlHistory.contains(finalUrl)) {
                    urlHistory = urlHistory + finalUrl
                }
                
                // WebView에 URL 로드
                fullscreenWebView?.loadUrl(finalUrl)
                
                // URL 변경 후 MP4 감지
                Handler(Looper.getMainLooper()).postDelayed({
                    analyzePageForMp4()
                }, 1000)
            },
            showTabOverview = showTabOverview,
            onShowTabOverview = { showTabOverview = it },
            onAddNewTab = ::addNewTab,
            canGoBack = canGoBack,
            canGoForward = canGoForward,
            isAnalyzing = isAnalyzing,
            mp4Links = mp4Links,
            downloadingUrls = downloadingUrls,
            urlHistory = urlHistory,
            onGoBack = {
                Log.d("Navigation", "뒤로가기 클릭 - canGoBack: $canGoBack, fullscreenWebView: ${fullscreenWebView != null}")
                fullscreenWebView?.let { webView ->
                    Log.d("Navigation", "WebView canGoBack: ${webView.canGoBack()}")
                    if (webView.canGoBack()) {
                        webView.goBack()
                        // 즉시 네비게이션 상태 업데이트
                        Handler(Looper.getMainLooper()).postDelayed({
                            updateNavigationState()
                            Log.d("Navigation", "뒤로가기 후 상태 - canGoBack: $canGoBack, canGoForward: $canGoForward")
                        }, 100)
                    }
                }
            },
            onGoForward = {
                Log.d("Navigation", "앞으로가기 클릭 - canGoForward: $canGoForward, fullscreenWebView: ${fullscreenWebView != null}")
                fullscreenWebView?.let { webView ->
                    Log.d("Navigation", "WebView canGoForward: ${webView.canGoForward()}")
                    if (webView.canGoForward()) {
                        webView.goForward()
                        // 즉시 네비게이션 상태 업데이트
                        Handler(Looper.getMainLooper()).postDelayed({
                            updateNavigationState()
                            Log.d("Navigation", "앞으로가기 후 상태 - canGoBack: $canGoBack, canGoForward: $canGoForward")
                        }, 100)
                    }
                }
            },
            onRefresh = {
                fullscreenWebView?.reload()
                Handler(Looper.getMainLooper()).postDelayed({
                    analyzePageForMp4()
                }, 1500)
            },
            onShowMp4List = { showMp4List = !showMp4List },
            onSwitchTab = ::switchTab,
            onCloseTab = ::closeTab,
            onDownloadVideo = ::downloadVideo,
            onExitFullscreen = { 
                // 전체화면 WebView의 상태 저장
                fullscreenWebView?.let { fsWebView ->
                    val bundle = Bundle()
                    fsWebView.saveState(bundle)
                    webViewState = bundle
                    
                    // 현재 URL도 업데이트
                    fsWebView.url?.let { url ->
                        if (url != currentUrl && 
                            !url.startsWith("data:") && 
                            !url.startsWith("about:") &&
                            url != "about:blank") {
                            currentUrl = url
                            urlText = url
                        }
                    }
                }
                
                // 전체화면 모드 종료 후 네비게이션 상태 업데이트
                isWebViewFullscreen = false
                
                // 상태 업데이트를 위한 지연 실행
                Handler(Looper.getMainLooper()).postDelayed({
                    updateNavigationState()
                }, 100)
            },
            webViewContent = {
                // 전체화면 WebView
                if (currentUrl.isNotEmpty()) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                        url?.let { newUrl ->
                                            updateUrlIfChanged(newUrl)
                                        }
                                        return false
                                    }

                                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        url?.let { newUrl ->
                                            updateUrlIfChanged(newUrl)
                                        }
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        url?.let { newUrl ->
                                            updateUrlIfChanged(newUrl)
                                        }
                                        
                                        // 페이지 로딩 완료 후 MP4 감지
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            analyzePageForMp4()
                                        }, 1000)
                                    }
                                }
                                webChromeClient = object : WebChromeClient() {
                                    override fun onReceivedTitle(view: WebView?, title: String?) {
                                        super.onReceivedTitle(view, title)
                                        // 탭 제목 업데이트
                                        title?.let { 
                                            currentTab?.let { tab ->
                                                tabs = tabs.map { 
                                                    if (it.id == tab.id) it.copy(title = title.take(15)) 
                                                    else it 
                                                }
                                            }
                                        }
                                    }
                                }
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                
                                // 전체화면 WebView 참조 설정
                                fullscreenWebView = this
                                
                                // 저장된 WebView 상태 복원 (히스토리 포함)
                                webViewState?.let { bundle ->
                                    restoreState(bundle)
                                    // 상태 복원 후 네비게이션 상태 업데이트
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        updateNavigationState()
                                    }, 500)
                                } ?: run {
                                    // 저장된 상태가 없으면 현재 URL로 로드
                                    val syncUrl = webView?.url ?: currentUrl
                                    if (syncUrl.isNotEmpty()) {
                                        this.loadUrl(syncUrl)
                                        // URL 로드 후 네비게이션 상태 업데이트
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            updateNavigationState()
                                        }, 1000)
                                    }
                                }
                            }
                        },
                        update = { view ->
                            if (currentUrl.isNotEmpty() && view.url != currentUrl) {
                                view.loadUrl(currentUrl)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "웹 페이지가 로드되지 않았습니다",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        )
        
        // MP4 목록 표시 다이얼로그
        if (showMp4List) {
            Mp4ListDialog(
                mp4Links = mp4Links,
                downloadingUrls = downloadingUrls,
                onDownload = { url -> downloadVideo(url) },
                onDismiss = { showMp4List = false }
            )
        }
        
        // 다운로드 결과 알림 팝업
        if (showDownloadResult && downloadResultMessage != null) {
            DownloadResultDialog(
                message = downloadResultMessage!!,
                onDismiss = { 
                    showDownloadResult = false
                    downloadResultMessage = null
                }
            )
        }
        
        return
    }

    // 일반 모드 레이아웃
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        // 탭바
        TabBar(
            tabs = tabs,
            currentTabIndex = currentTabIndex,
            onNewTab = ::addNewTab,
            onCloseTab = ::closeTab,
            onSwitchTab = ::switchTab,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 앱 제목
        Text(
            text = "Simple Web Video Downloader v5.6",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )

        // URL 입력 섹션
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // 섹션 헤더 (클릭 가능)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { urlSectionExpanded = !urlSectionExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "웹 페이지 URL 입력",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (urlSectionExpanded) "▲" else "▼",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // 로드된 URL이 있고 섹션이 접혀있을 때 간단히 표시
                if (!urlSectionExpanded && currentUrl.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentUrl.let { url ->
                            if (url.length > 40) "...${url.takeLast(37)}" else url
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                // 접힐 수 있는 내용
                AnimatedVisibility(
                    visible = urlSectionExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(6.dp))

                        // URL 입력 필드
                        OutlinedTextField(
                            value = urlText,
                            onValueChange = { urlText = it },
                            label = { Text("URL을 입력하세요") },
                            placeholder = { Text("https://example.com") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = { loadUrl() }
                            ),
                            trailingIcon = {
                                TextButton(
                                    onClick = {
                                        clipboardManager.getText()?.text?.let { clipText ->
                                            if (clipText.contains(".")) {
                                                urlText = clipText
                                            }
                                        }
                                    }
                                ) {
                                    Text("붙여넣기")
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 버튼들
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 첫 번째 줄: 페이지 로드, 초기화
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { loadUrl() },
                                    modifier = Modifier.weight(1f),
                                    enabled = urlText.isNotBlank() && !isAnalyzing
                                ) {
                                    if (isAnalyzing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("MP4 감지 중...")
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("페이지 로드")
                                    }
                                }

                                Button(
                                                                    onClick = { 
                                    // 초기화 기능
                                    urlText = ""
                                    currentUrl = ""
                                    mp4Links = emptyList()
                                    webView = null
                                    fullscreenWebView = null
                                    urlSectionExpanded = true
                                    mp4SectionExpanded = true
                                    isAnalyzing = false
                                    hasAnalyzed = false
                                    downloadingUrls = emptySet()
                                },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("초기화")
                                }
                            }

                            // 두 번째 줄: 새로고침 & MP4 감지 (현재 URL이 있을 때만 표시)
                            if (currentUrl.isNotEmpty()) {
                                Button(
                                    onClick = { 
                                        webView?.reload()
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            analyzePageForMp4()
                                        }, 1000)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isAnalyzing,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiary
                                    )
                                ) {
                                    if (isAnalyzing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = MaterialTheme.colorScheme.onTertiary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("새로고침 중...")
                                    } else {
                                        Text("🔄 새로고침 & MP4 감지")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 감지된 MP4 목록 및 다운로드 버튼
        if (hasAnalyzed) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // 섹션 헤더 (클릭 가능)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { mp4SectionExpanded = !mp4SectionExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (mp4Links.isEmpty()) {
                                "🎯 감지된 MP4 비디오 (없음)"
                            } else {
                                "🎯 감지된 MP4 비디오 (${mp4Links.size}개)"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (mp4SectionExpanded) "▲" else "▼",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    // 접힌 상태에서 간단한 요약 표시
                    if (!mp4SectionExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        if (mp4Links.isEmpty()) {
                            Text(
                                text = "이 페이지에서 MP4 비디오를 찾을 수 없습니다",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            val downloadableCount = mp4Links.count { it.contains(".mp4") && it.contains("http") }
                            Text(
                                text = "다운로드 가능한 비디오: ${downloadableCount}개",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // 접힐 수 있는 목록 내용
                    AnimatedVisibility(
                        visible = mp4SectionExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))

                            if (mp4Links.isEmpty()) {
                                // MP4가 없을 때 표시
                                Text(
                                    text = "이 페이지에서 MP4 비디오를 찾을 수 없습니다.\n비디오가 iframe이나 스트리밍으로 구현되었을 수 있습니다.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.height(200.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    itemsIndexed(mp4Links) { index, link ->
                                    if (link.contains(".mp4") && link.contains("http")) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = "${index + 1}. ${if (link.length > 50) "...${link.takeLast(50)}" else link}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.padding(bottom = 8.dp)
                                                )

                                                Button(
                                                    onClick = { downloadVideo(link) },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    enabled = !downloadingUrls.contains(link),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.tertiary
                                                    )
                                                ) {
                                                    if (downloadingUrls.contains(link)) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(16.dp),
                                                            color = MaterialTheme.colorScheme.onTertiary
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text("다운로드 중...")
                                                    } else {
                                                        Text("⬇️ 다운로드")
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "${index + 1}. $link",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // MP4가 감지되지 않았을 때는 하단 여백 없음
            if (mp4Links.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // WebView 영역 - 개선된 레이아웃
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // WebView 카드
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (currentUrl.isNotEmpty()) 100.dp else 0.dp), // 네비게이션 바를 위한 하단 여백 증가 (80dp → 100dp)
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                if (currentUrl.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "웹 페이지를 로드하면 여기에 브라우저가 표시됩니다",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    AndroidView(
                        factory = { context ->
                            // 일반 모드 WebView 생성 또는 재사용
                            webView ?: WebView(context).apply {
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                        url?.let { newUrl ->
                                            updateUrlIfChanged(newUrl)
                                        }
                                        return false
                                    }

                                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        url?.let { newUrl ->
                                            updateUrlIfChanged(newUrl)
                                        }
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        url?.let { newUrl ->
                                            updateUrlIfChanged(newUrl)
                                        }
                                        
                                        // 페이지 로딩 완료 후 MP4 감지
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            analyzePageForMp4()
                                        }, 1000)
                                    }
                                }
                                webChromeClient = object : WebChromeClient() {
                                    override fun onReceivedTitle(view: WebView?, title: String?) {
                                        super.onReceivedTitle(view, title)
                                        // 탭 제목 업데이트
                                        title?.let { 
                                            currentTab?.let { tab ->
                                                tabs = tabs.map { 
                                                    if (it.id == tab.id) it.copy(title = title.take(15)) 
                                                    else it 
                                                }
                                            }
                                        }
                                    }
                                }
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                
                                // 일반 모드 WebView 참조 설정
                                webView = this
                                
                                // 저장된 WebView 상태 복원 (히스토리 포함)
                                webViewState?.let { bundle ->
                                    restoreState(bundle)
                                } ?: run {
                                    // 저장된 상태가 없으면 현재 URL로 로드
                                    val syncUrl = fullscreenWebView?.url ?: currentUrl
                                    if (syncUrl.isNotEmpty()) {
                                        this.loadUrl(syncUrl)
                                    }
                                }
                            }
                        },
                        update = { view ->
                            if (currentUrl.isNotEmpty() && view.url != currentUrl) {
                                view.loadUrl(currentUrl)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // 네비게이션 바 (WebView가 로드된 경우에만 표시)
            if (currentUrl.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom))
                        .padding(horizontal = 16.dp, vertical = 20.dp),  // vertical 패딩 증가 (12dp → 20dp)
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 뒤로가기 버튼
                        FloatingActionButton(
                            onClick = { 
                                Log.d("Navigation", "일반모드 뒤로가기 클릭 - canGoBack: $canGoBack, webView: ${webView != null}")
                                webView?.let { view ->
                                    Log.d("Navigation", "일반모드 WebView canGoBack: ${view.canGoBack()}")
                                    if (view.canGoBack()) {
                                        view.goBack()
                                        // 즉시 네비게이션 상태 업데이트
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            updateNavigationState()
                                            Log.d("Navigation", "일반모드 뒤로가기 후 상태 - canGoBack: $canGoBack, canGoForward: $canGoForward")
                                        }, 100)
                                    }
                                }
                            },
                            modifier = Modifier.size(44.dp),
                            containerColor = if (canGoBack) 
                                MaterialTheme.colorScheme.secondaryContainer 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "뒤로가기",
                                modifier = Modifier.size(20.dp),
                                tint = if (canGoBack) 
                                    MaterialTheme.colorScheme.onSecondaryContainer 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }

                        // 새로고침 + MP4 감지 버튼
                        FloatingActionButton(
                            onClick = { 
                                webView?.reload()
                                Handler(Looper.getMainLooper()).postDelayed({
                                    analyzePageForMp4()
                                }, 1500)
                            },
                            modifier = Modifier.size(44.dp),
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "새로고침",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }

                        // 앞으로가기 버튼
                        FloatingActionButton(
                            onClick = { 
                                Log.d("Navigation", "일반모드 앞으로가기 클릭 - canGoForward: $canGoForward, webView: ${webView != null}")
                                webView?.let { view ->
                                    Log.d("Navigation", "일반모드 WebView canGoForward: ${view.canGoForward()}")
                                    if (view.canGoForward()) {
                                        view.goForward()
                                        // 즉시 네비게이션 상태 업데이트
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            updateNavigationState()
                                            Log.d("Navigation", "일반모드 앞으로가기 후 상태 - canGoBack: $canGoBack, canGoForward: $canGoForward")
                                        }, 100)
                                    }
                                }
                            },
                            modifier = Modifier.size(44.dp),
                            containerColor = if (canGoForward) 
                                MaterialTheme.colorScheme.secondaryContainer 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "앞으로가기",
                                modifier = Modifier.size(20.dp),
                                tint = if (canGoForward) 
                                    MaterialTheme.colorScheme.onSecondaryContainer 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }

                        // 전체화면 버튼
                        FloatingActionButton(
                            onClick = { 
                                // 일반 모드 WebView의 상태 저장
                                webView?.let { mainWebView ->
                                    val bundle = Bundle()
                                    mainWebView.saveState(bundle)
                                    webViewState = bundle
                                    
                                    // 현재 URL도 업데이트
                                    mainWebView.url?.let { url ->
                                        if (url != currentUrl && 
                                            !url.startsWith("data:") && 
                                            !url.startsWith("about:") &&
                                            url != "about:blank") {
                                            currentUrl = url
                                            urlText = url
                                        }
                                    }
                                }
                                isWebViewFullscreen = true 
                            },
                            modifier = Modifier.size(44.dp),
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "⛶",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
        
        // 다운로드 결과 알림 팝업 (일반 모드)
        if (showDownloadResult && downloadResultMessage != null) {
            DownloadResultDialog(
                message = downloadResultMessage!!,
                onDismiss = { 
                    showDownloadResult = false
                    downloadResultMessage = null
                }
            )
        }
    }
}

@Composable
fun URLDisplay(
    currentUrl: String,
    onUrlChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    fillMaxSize: Boolean = false,
    urlHistory: List<String> = emptyList()  // URL 히스토리 추가
) {
    var isEditing by remember { mutableStateOf(false) }
    var editingUrl by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    if (isEditing && fillMaxSize) {
        // URL 편집 모드 - 전체 화면 모드에서 최근 URL과 입력창 표시
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { 
                    // 배경 클릭 시 편집 모드 종료
                    isEditing = false
                    keyboardController?.hide()
                }
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom))
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .padding(bottom = 100.dp), // 네비게이션 버튼 바로 위 (76dp → 100dp)
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { 
                        // Card 클릭 시 아무것도 하지 않음 (이벤트 전파 방지)
                    },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "URL 편집",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    // 최근 URL 목록 표시 (위쪽)
                    if (urlHistory.isNotEmpty()) {
                        Text(
                            text = "최근 방문 URL",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(urlHistory.reversed().take(5)) { historyUrl -> // 최근 5개만 표시
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            editingUrl = historyUrl
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                    )
                                ) {
                                    Text(
                                        text = historyUrl.let { url ->
                                            when {
                                                url.length <= 45 -> url
                                                else -> "${url.take(20)}...${url.takeLast(22)}"
                                            }
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(8.dp),
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    // URL 입력창 (아래쪽)
                    Text(
                        text = "URL 입력",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    OutlinedTextField(
                        value = editingUrl,
                        onValueChange = { editingUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                onUrlChange(editingUrl)
                                isEditing = false
                                keyboardController?.hide()
                            }
                        ),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = {
                                isEditing = false
                                keyboardController?.hide()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("취소")
                        }
                        
                        Button(
                            onClick = {
                                onUrlChange(editingUrl)
                                isEditing = false
                                keyboardController?.hide()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("이동")
                        }
                    }
                }
            }
        }
    } else if (!isEditing) {
        // URL 표시 모드 - 네비게이션 버튼 높이에 맞춤
        if (currentUrl.isNotEmpty()) {
            Card(
                modifier = modifier
                    .clickable {
                        editingUrl = currentUrl
                        isEditing = true
                    }
                    .width(140.dp) // 크기 조정 (120dp → 140dp)
                    .height(44.dp), // 네비게이션 버튼과 동일한 높이 (36dp → 44dp)
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = currentUrl.let { url ->
                            when {
                                url.length <= 25 -> url
                                else -> "...${url.takeLast(22)}"
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun Mp4ListDialog(
    mp4Links: List<String>,
    downloadingUrls: Set<String>,
    onDownload: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(max = 400.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MP4 비디오 목록",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "(${mp4Links.size}개)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (mp4Links.isEmpty()) {
                    Text(
                        text = "MP4 비디오를 찾을 수 없습니다.\n페이지를 새로고침하거나 다른 페이지를 시도해보세요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(mp4Links) { index, url ->
                            val isDownloading = downloadingUrls.contains(url)
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDownloading) 
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    else 
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        text = "비디오 ${index + 1}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    
                                    Text(
                                        text = url.let { 
                                            when {
                                                it.length <= 50 -> it
                                                else -> "${it.take(25)}...${it.takeLast(22)}"
                                            }
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    
                                    Button(
                                        onClick = { onDownload(url) },
                                        enabled = !isDownloading,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (isDownloading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("다운로드 중...")
                                        } else {
                                            Text("다운로드")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("닫기")
                }
            }
        }
    }
}

// 탭바 컴포넌트
@Composable
fun TabBar(
    tabs: List<Tab>,
    currentTabIndex: Int,
    onNewTab: () -> Unit,
    onCloseTab: (Int) -> Unit,
    onSwitchTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 탭 목록
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(tabs) { index, tab ->
                    TabItem(
                        tab = tab,
                        isSelected = index == currentTabIndex,
                        onClick = { onSwitchTab(index) },
                        onClose = if (tabs.size > 1) { { onCloseTab(index) } } else null
                    )
                }
            }
            
            // 새 탭 버튼
            IconButton(
                onClick = onNewTab,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    Icons.Default.Add, 
                    contentDescription = "새 탭",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            // 탭 개수 표시
            Card(
                modifier = Modifier.padding(start = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "${tabs.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// 개별 탭 아이템 컴포넌트
@Composable
fun TabItem(
    tab: Tab,
    isSelected: Boolean,
    onClick: () -> Unit,
    onClose: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .widthIn(min = 100.dp, max = 180.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (tab.title.isNotBlank() && tab.title != "새 탭") tab.title else "새 탭",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp,
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = tab.url.take(25) + if (tab.url.length > 25) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 9.sp
                )
            }
            
            onClose?.let {
                IconButton(
                    onClick = it,
                    modifier = Modifier.size(18.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "탭 닫기",
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// 전체화면 UI 컴포넌트
@Composable
fun FullscreenUI(
    tabs: List<Tab>,
    currentTabIndex: Int,
    currentUrl: String,
    urlText: String,
    onUrlTextChange: (String) -> Unit,
    onLoadUrl: (String) -> Unit,
    showTabOverview: Boolean,
    onShowTabOverview: (Boolean) -> Unit,
    onAddNewTab: () -> Unit,
    canGoBack: Boolean,
    canGoForward: Boolean,
    isAnalyzing: Boolean,
    mp4Links: List<String>,
    downloadingUrls: Set<String>,
    urlHistory: List<String>,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    onRefresh: () -> Unit,
    onShowMp4List: () -> Unit,
    onSwitchTab: (Int) -> Unit,
    onCloseTab: (Int) -> Unit,
    onDownloadVideo: (String) -> Unit,
    onExitFullscreen: () -> Unit,
    webViewContent: @Composable () -> Unit
) {
    var isEditingUrl by remember { mutableStateOf(false) }
    var editUrlText by remember { mutableStateOf("") }
    var showHistoryDropdown by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showTabOverview) {
            // 탭 오버뷰 화면
            TabOverviewScreen(
                tabs = tabs,
                currentTabIndex = currentTabIndex,
                onTabSelected = { index ->
                    onSwitchTab(index)
                    onShowTabOverview(false)
                },
                onTabClosed = { index ->
                    onCloseTab(index)
                    if (tabs.size == 1) {
                        onShowTabOverview(false)
                    }
                },
                onAddNewTab = {
                    onAddNewTab()
                    onShowTabOverview(false)
                },
                onBackToWebView = { onShowTabOverview(false) }
            )
        } else {
            // 웹뷰 화면
            Column(modifier = Modifier.fillMaxSize()) {
                // 상단 바
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top)),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 좌측: URL 표시/편집
                        if (isEditingUrl) {
                            OutlinedTextField(
                                value = editUrlText,
                                onValueChange = { editUrlText = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("URL 입력") },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(
                                    onGo = {
                                        if (editUrlText.isNotBlank()) {
                                            val url = if (!editUrlText.startsWith("http")) {
                                                "https://$editUrlText"
                                            } else {
                                                editUrlText
                                            }
                                            onLoadUrl(url)
                                        }
                                        isEditingUrl = false
                                    }
                                ),
                                trailingIcon = {
                                    Row {
                                        IconButton(
                                            onClick = {
                                                if (editUrlText.isNotBlank()) {
                                                    val url = if (!editUrlText.startsWith("http")) {
                                                        "https://$editUrlText"
                                                    } else {
                                                        editUrlText
                                                    }
                                                    onLoadUrl(url)
                                                }
                                                isEditingUrl = false
                                            }
                                        ) {
                                            Icon(Icons.Default.Check, "확인")
                                        }
                                        IconButton(onClick = { isEditingUrl = false }) {
                                            Icon(Icons.Default.Close, "취소")
                                        }
                                        IconButton(onClick = { showHistoryDropdown = !showHistoryDropdown }) {
                                            Icon(Icons.Default.Info, "기록")
                                        }
                                    }
                                },
                                singleLine = true
                            )
                        } else {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        editUrlText = currentUrl
                                        isEditingUrl = true
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = currentUrl.takeIf { it.isNotEmpty() } ?: "URL을 입력하세요",
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // 우측: 새 탭 추가, 탭 개수, 전체화면 종료 (3개 버튼)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 1. 새 탭 추가 버튼
                            IconButton(onClick = onAddNewTab) {
                                Icon(
                                    Icons.Default.Add, 
                                    contentDescription = "새 탭",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            // 2. 탭 개수 (탭 오버뷰로 이동)
                            Surface(
                                modifier = Modifier.clickable { onShowTabOverview(true) },
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = tabs.size.toString(),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            
                            // 3. 전체화면 종료 버튼
                            IconButton(onClick = onExitFullscreen) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "전체화면 종료",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    // URL 히스토리 드롭다운
                    if (showHistoryDropdown && urlHistory.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(8.dp)
                        ) {
                            items(urlHistory.reversed()) { url ->
                                Text(
                                    text = url,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onLoadUrl(url)
                                            showHistoryDropdown = false
                                        }
                                        .padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // 웹뷰 콘텐츠
                Box(modifier = Modifier.weight(1f)) {
                    webViewContent()
                }

                // 하단 네비게이션 바
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom)),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. 뒤로가기
                        IconButton(
                            onClick = onGoBack,
                            enabled = canGoBack
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                "뒤로가기",
                                tint = if (canGoBack) 
                                    MaterialTheme.colorScheme.onSurface 
                                else 
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }

                        // 2. 새로고침 및 MP4 감지
                        IconButton(
                            onClick = onRefresh
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(Icons.Default.Refresh, "새로고침")
                            }
                        }

                        // 3. MP4 목록 보기
                        IconButton(
                            onClick = onShowMp4List
                        ) {
                            Icon(Icons.Default.PlayArrow, "MP4 목록")
                        }

                        // 4. 최근 방문한 페이지 목록
                        IconButton(
                            onClick = { showHistoryDropdown = !showHistoryDropdown },
                            enabled = urlHistory.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Default.Info,
                                "방문 기록",
                                tint = if (urlHistory.isNotEmpty()) 
                                    MaterialTheme.colorScheme.onSurface 
                                else 
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }

                        // 5. 앞으로가기
                        IconButton(
                            onClick = onGoForward,
                            enabled = canGoForward
                        ) {
                            Icon(
                                Icons.Default.ArrowForward,
                                "앞으로가기",
                                tint = if (canGoForward) 
                                    MaterialTheme.colorScheme.onSurface 
                                else 
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }

        // 전체화면 종료 버튼은 이제 상단 바에 통합됨
    }
}

// 탭 오버뷰 화면
@Composable
fun TabOverviewScreen(
    tabs: List<Tab>,
    currentTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    onTabClosed: (Int) -> Unit,
    onAddNewTab: () -> Unit,
    onBackToWebView: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(16.dp)
    ) {
        // 상단 바
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "탭 ${tabs.size}개",
                style = MaterialTheme.typography.headlineSmall
            )
            IconButton(onClick = onBackToWebView) {
                Icon(Icons.Default.Close, "닫기")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 탭 그리드
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 기존 탭들
            itemsIndexed(tabs) { index, tab ->
                TabCard(
                    tab = tab,
                    isCurrentTab = index == currentTabIndex,
                    onClick = { onTabSelected(index) },
                    onClose = { onTabClosed(index) }
                )
            }

            // 새 탭 추가 카드
            item {
                AddTabCard(onClick = onAddNewTab)
            }
        }
    }
}

// 탭 카드 컴포넌트
@Composable
fun TabCard(
    tab: Tab,
    isCurrentTab: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentTab) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = tab.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isCurrentTab) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = tab.url.takeIf { it.isNotEmpty() } ?: "새 탭",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isCurrentTab) 
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // 닫기 버튼
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "탭 닫기",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }

            // 현재 탭 표시
            if (isCurrentTab) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "현재",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

// 새 탭 추가 카드
@Composable
fun AddTabCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        border = BorderStroke(
            1.dp, 
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Add,
                    "새 탭 추가",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "새 탭",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

// 다운로드 결과 알림 다이얼로그
@Composable
fun DownloadResultDialog(
    message: String,
    onDismiss: () -> Unit
) {
    // 자동으로 사라지도록 LaunchedEffect 사용
    LaunchedEffect(message) {
        kotlinx.coroutines.delay(if (message.contains("실패")) 5000 else 3000)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(32.dp)
                .widthIn(min = 280.dp, max = 400.dp)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { /* 카드 클릭 시 이벤트 전파 방지 */ },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 아이콘 (성공/실패에 따라)
                Icon(
                    imageVector = if (message.contains("실패")) Icons.Default.Close else Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = if (message.contains("실패")) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 메시지
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 확인 버튼
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (message.contains("실패")) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("확인")
                }
            }
        }
    }
}