package com.swvd.simplewebvideodownloader

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.compose.BackHandler
import androidx.core.view.WindowCompat


// Import separated components and utilities
import com.swvd.simplewebvideodownloader.models.Tab
import com.swvd.simplewebvideodownloader.download.DownloadHandler
import com.swvd.simplewebvideodownloader.webview.Mp4Analyzer
import com.swvd.simplewebvideodownloader.utils.FullscreenManager
import com.swvd.simplewebvideodownloader.ui.components.*
import com.swvd.simplewebvideodownloader.ui.screens.FullscreenUI

/**
 * MainActivity - 분리된 구조로 리팩토링
 * 각 기능이 별도 클래스/컴포넌트로 분리되어 관리가 용이함
 */
class MainActivity : ComponentActivity() {

    // 다운로드 관련 기능을 담당하는 핸들러
    private lateinit var downloadHandler: DownloadHandler
    
    // MP4 분석 기능을 담당하는 애널라이저
    private val mp4Analyzer = Mp4Analyzer()

    // 권한 요청 처리
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted && this::downloadHandler.isInitialized) {
            // 권한 요청 결과를 DownloadHandler에게 위임할 수 있도록 추후 확장 가능
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 분리된 FullscreenManager 사용
        FullscreenManager.enableEdgeToEdge(this)
        
        // DownloadHandler 초기화
        downloadHandler = DownloadHandler(this)
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                ) {
                    MainScreen(
                        downloadHandler = downloadHandler,
                        mp4Analyzer = mp4Analyzer,
                        onRequestPermissions = { requestStoragePermissions() },
                        onFullscreenModeChange = { isFullscreen -> 
                            FullscreenManager.setFullscreenMode(this@MainActivity, isFullscreen) 
                        }
                    )
                }
            }
        }
    }

    /**
     * 저장소 권한 요청
     * DownloadHandler로 권한 확인 로직 위임
     */
    private fun requestStoragePermissions() {
        val permissions = downloadHandler.checkStoragePermissions()
        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }
}

/**
 * 메인 화면 컴포저블
 * 분리된 컴포넌트들을 조합하여 전체 화면을 구성
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    downloadHandler: DownloadHandler,
    mp4Analyzer: Mp4Analyzer,
    onRequestPermissions: () -> Unit,
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
        
        Log.d("WebView", "MP4 감지 시작 (전체화면: $isWebViewFullscreen)")
        
        isAnalyzing = true
        hasAnalyzed = true
        
        // 분리된 Mp4Analyzer 사용
        mp4Analyzer.analyzePageForMp4(activeWebView) { videoLinks ->
            mp4Links = videoLinks
            isAnalyzing = false
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

        // URL 유효성 검사
        if (!downloadHandler.isValidUrl(cleanUrl)) {
            downloadResultMessage = "다운로드 실패!\n유효하지 않은 URL입니다"
            showDownloadResult = true
            
            Handler(Looper.getMainLooper()).postDelayed({
                showDownloadResult = false
                downloadResultMessage = null
            }, 3000)
            return
        }

        // 권한 확인
        val permissions = downloadHandler.checkStoragePermissions()
        if (permissions.isNotEmpty()) {
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
        val filename = downloadHandler.generateFilename(cleanUrl)

        try {
            downloadingUrls = downloadingUrls + cleanUrl
            downloadHandler.downloadFile(cleanUrl, filename)
            
            // 다운로드 성공 알림
            downloadResultMessage = "다운로드 시작!\n파일: $filename\nDownloads 폴더에 저장됩니다"
            showDownloadResult = true
            
            Handler(Looper.getMainLooper()).postDelayed({
                showDownloadResult = false
                downloadResultMessage = null
            }, 3000)
            
        } catch (e: Exception) {
            // 다운로드 실패 시 상태 업데이트 및 에러 알림
            downloadingUrls = downloadingUrls - cleanUrl
            downloadResultMessage = "다운로드 실패!\n오류: ${e.message}"
            showDownloadResult = true
            
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

            // 전체화면 모드일 때 분리된 FullscreenUI 컴포넌트 사용
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
                            text = "Simple Web Video Downloader v5.8",
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
