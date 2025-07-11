package com.swvd.simplewebvideodownloader

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModelProvider

// ViewModels
import com.swvd.simplewebvideodownloader.viewmodel.*

// UI Components
import com.swvd.simplewebvideodownloader.ui.components.*
import com.swvd.simplewebvideodownloader.ui.theme.SimpleWebVideoDownloaderTheme
import com.swvd.simplewebvideodownloader.ui.screens.FullscreenUI

// Models
import com.swvd.simplewebvideodownloader.models.VideoInfo
import com.swvd.simplewebvideodownloader.models.VideoType

// Utils
import com.swvd.simplewebvideodownloader.utils.FullscreenManager

/**
 * 새로운 MainActivity - 리팩토링된 버전
 * MVVM 아키텍처와 컴포넌트 기반으로 간소화
 * 기존 1,286라인에서 약 100라인으로 축소
 */
class MainActivityNew : ComponentActivity() {

    // ViewModels
    private lateinit var mainViewModel: MainViewModel
    private lateinit var tabViewModel: TabViewModel
    private lateinit var webViewViewModel: WebViewViewModel
    private lateinit var videoDetectionViewModel: VideoDetectionViewModel
    private lateinit var downloadViewModel: DownloadViewModel

    // 권한 요청 처리
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (!permissions.values.all { it }) {
            downloadViewModel.showDownloadResult("저장소 권한이 필요합니다")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // FullscreenManager 설정
        FullscreenManager.enableEdgeToEdge(this)
        
        // ViewModels 초기화
        initViewModels()
        
        setContent {
            SimpleWebVideoDownloaderTheme {
                MainScreen()
            }
        }
    }

    /**
     * ViewModels 초기화
     */
    private fun initViewModels() {
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return when (modelClass) {
                    MainViewModel::class.java -> MainViewModel(applicationContext) as T
                    DownloadViewModel::class.java -> DownloadViewModel(applicationContext) as T
                    TabViewModel::class.java -> TabViewModel() as T
                    WebViewViewModel::class.java -> WebViewViewModel() as T
                    VideoDetectionViewModel::class.java -> VideoDetectionViewModel() as T
                    else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
        
        mainViewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]
        tabViewModel = ViewModelProvider(this, factory)[TabViewModel::class.java]
        webViewViewModel = ViewModelProvider(this, factory)[WebViewViewModel::class.java]
        videoDetectionViewModel = ViewModelProvider(this, factory)[VideoDetectionViewModel::class.java]
        downloadViewModel = ViewModelProvider(this, factory)[DownloadViewModel::class.java]
        
        // 권한 요청 콜백 설정
        mainViewModel.onRequestPermissions = {
            val permissions = mainViewModel.checkStoragePermissions()
            if (permissions.isNotEmpty()) {
                requestPermissionLauncher.launch(permissions.toTypedArray())
            }
        }
    }

    /**
     * 메인 화면 컴포저블
     */
    @Composable
    private fun MainScreen() {
        // ViewModels 상태 구독
        val mainUiState by mainViewModel.uiState.collectAsState()
        val downloadResult by mainViewModel.downloadResult.collectAsState()
        
        val tabs by tabViewModel.tabs.collectAsState()
        val currentTabIndex by tabViewModel.currentTabIndex.collectAsState()
        val showTabOverview by tabViewModel.showTabOverview.collectAsState()
        
        val urlText by webViewViewModel.urlText.collectAsState()
        val currentUrl by webViewViewModel.currentUrl.collectAsState()
        val canGoBack by webViewViewModel.canGoBack.collectAsState()
        val canGoForward by webViewViewModel.canGoForward.collectAsState()
        val isFullscreen by webViewViewModel.isFullscreen.collectAsState()
        val webViewState by webViewViewModel.webViewState.collectAsState()
        
        val videoList = mainUiState.videoList
        val isAnalyzing = mainUiState.isAnalyzing
        val hasAnalyzed = mainUiState.hasAnalyzed
        
        val downloadingUrls by downloadViewModel.downloadingUrls.collectAsState()
        val showDownloadResult by downloadViewModel.showDownloadResult.collectAsState()
        val downloadResultMessage by downloadViewModel.downloadResult.collectAsState()

        // WebView 참조 관리
        var mainWebView by remember { mutableStateOf<WebView?>(null) }
        var fullscreenWebView by remember { mutableStateOf<WebView?>(null) }

        // 전체화면 모드 BackHandler
        BackHandler(enabled = isFullscreen) {
            handleExitFullscreen(mainWebView, fullscreenWebView)
        }

        // 전체화면 UI 또는 일반 UI
        if (isFullscreen) {
            FullscreenModeScreen(
                currentUrl = currentUrl,
                webViewState = webViewState,
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                isAnalyzing = isAnalyzing,
                onFullscreenWebViewCreated = { fullscreenWebView = it },
                onExitFullscreen = { handleExitFullscreen(mainWebView, fullscreenWebView) },
                onGoBack = { webViewViewModel.goBack(fullscreenWebView) },
                onGoForward = { webViewViewModel.goForward(fullscreenWebView) },
                onRefresh = { handleRefresh(fullscreenWebView) },
                onUrlChanged = { webViewViewModel.updateCurrentUrl(it) },
                onPageFinished = { handlePageFinished(fullscreenWebView) },
                onTitleReceived = { tabViewModel.updateCurrentTabTitle(it) }
            )
        } else {
            NormalScreen(
                mainWebView = mainWebView,
                onMainWebViewCreated = { mainWebView = it },
                onEnterFullscreen = { handleEnterFullscreen(mainWebView) }
            )
        }

        // 다이얼로그들
        if (showDownloadResult && downloadResultMessage != null) {
            DownloadResultDialog(
                show = showDownloadResult,
                message = downloadResultMessage,
                onDismiss = { downloadViewModel.hideDownloadResult() }
            )
        }
    }

    /**
     * 일반 모드 화면
     */
    @Composable
    private fun NormalScreen(
        mainWebView: WebView?,
        onMainWebViewCreated: (WebView) -> Unit,
        onEnterFullscreen: () -> Unit
    ) {
        val mainUiState by mainViewModel.uiState.collectAsState()
        val urlText by webViewViewModel.urlText.collectAsState()
        val currentUrl by webViewViewModel.currentUrl.collectAsState()
        val canGoBack by webViewViewModel.canGoBack.collectAsState()
        val canGoForward by webViewViewModel.canGoForward.collectAsState()
        val isAnalyzing = mainUiState.isAnalyzing
        val videoList = mainUiState.videoList
        val downloadingUrls by downloadViewModel.downloadingUrls.collectAsState()
        
        val tabs by tabViewModel.tabs.collectAsState()
        val currentTabIndex by tabViewModel.currentTabIndex.collectAsState()

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // 탭바
                TabBar(
                    tabs = tabs,
                    currentTabIndex = currentTabIndex,
                    onNewTab = { tabViewModel.addNewTab() },
                    onCloseTab = { tabViewModel.closeTab(it) },
                    onSwitchTab = { tabViewModel.switchTab(it) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 앱 제목
                Text(
                    text = "Simple Web Video Downloader v5.8",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // URL 입력 섹션
                UrlInputSection(
                    urlText = urlText,
                    currentUrl = currentUrl,
                    isAnalyzing = isAnalyzing,
                    urlSectionExpanded = mainUiState.urlSectionExpanded,
                    onUrlTextChange = { webViewViewModel.updateUrlText(it) },
                    onLoadUrl = { handleLoadUrl() },
                    onReset = { handleReset() },
                    onRefresh = { handleRefresh() },
                    onToggleExpanded = { mainViewModel.toggleUrlSection() }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 비디오 목록 섹션
                VideoListSection(
                    hasAnalyzed = mainUiState.hasAnalyzed,
                    videoList = mainUiState.videoList,
                    mp4Links = mainUiState.mp4Links,
                    downloadingUrls = downloadingUrls,
                    videoSectionExpanded = mainUiState.videoSectionExpanded,
                    onToggleExpanded = { mainViewModel.toggleVideoSection() },
                    onDownloadVideo = { downloadViewModel.downloadVideo(it) },
                    onDownloadMp4 = { /* 기존 호환성 */ }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // WebView 및 네비게이션
                Box(modifier = Modifier.weight(1f)) {
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = if (currentUrl.isNotEmpty()) 80.dp else 0.dp)
                    ) {
                        WebViewContainer(
                            currentUrl = currentUrl,
                            webViewState = webViewViewModel.webViewState.collectAsState().value,
                            onWebViewCreated = { onMainWebViewCreated(it) },
                            onUrlChanged = { webViewViewModel.updateCurrentUrl(it) },
                            onPageFinished = { handlePageFinished(mainWebView) },
                            onTitleReceived = { tabViewModel.updateCurrentTabTitle(it) }
                        )
                    }

                    // 네비게이션 바
                    if (currentUrl.isNotEmpty()) {
                        NavigationBottomBar(
                            modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter),
                            canGoBack = canGoBack,
                            canGoForward = canGoForward,
                            isAnalyzing = isAnalyzing,
                            onGoBack = { webViewViewModel.goBack(mainWebView) },
                            onGoForward = { webViewViewModel.goForward(mainWebView) },
                            onRefresh = { handleRefresh(mainWebView) },
                            onFullscreen = onEnterFullscreen
                        )
                    }
                }
            }
        }
    }

    /**
     * 전체화면 모드 화면
     */
    @Composable
    private fun FullscreenModeScreen(
        currentUrl: String,
        webViewState: Bundle?,
        canGoBack: Boolean,
        canGoForward: Boolean,
        isAnalyzing: Boolean,
        onFullscreenWebViewCreated: (WebView) -> Unit,
        onExitFullscreen: () -> Unit,
        onGoBack: () -> Unit,
        onGoForward: () -> Unit,
        onRefresh: () -> Unit,
        onUrlChanged: (String) -> Unit,
        onPageFinished: (String) -> Unit,
        onTitleReceived: (String) -> Unit
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 전체화면 WebView
            FullscreenWebViewContainer(
                currentUrl = currentUrl,
                webViewState = webViewState,
                syncWebView = null, // 전체화면에서는 sync 없음
                onWebViewCreated = onFullscreenWebViewCreated,
                onUrlChanged = onUrlChanged,
                onPageFinished = onPageFinished,
                onTitleReceived = onTitleReceived
            )
            
            // 전체화면 네비게이션 바
            FullscreenNavigationBar(
                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter),
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                isAnalyzing = isAnalyzing,
                onGoBack = onGoBack,
                onGoForward = onGoForward,
                onRefresh = onRefresh,
                onExitFullscreen = onExitFullscreen,
                onShowMp4List = { /* TODO: 구현 */ }
            )
        }
    }

    // Helper 함수들
    private fun handleLoadUrl() {
        webViewViewModel.loadUrl(null, webViewViewModel.urlText.value)
    }

    private fun handleReset() {
        mainViewModel.resetState()
        webViewViewModel.resetAll()
    }

    private fun handleRefresh(webView: WebView? = null) {
        webView?.let { 
            mainViewModel.startVideoAnalysis(it) 
        }
    }

    private fun handlePageFinished(webView: WebView? = null) {
        webView?.let { 
            mainViewModel.startVideoAnalysis(it) 
        }
    }

    private fun handleEnterFullscreen(mainWebView: WebView?) {
        webViewViewModel.saveWebViewState(mainWebView)
        webViewViewModel.setFullscreen(true)
        FullscreenManager.setFullscreenMode(this, true)
    }

    private fun handleExitFullscreen(mainWebView: WebView?, fullscreenWebView: WebView?) {
        webViewViewModel.saveWebViewState(fullscreenWebView)
        webViewViewModel.setFullscreen(false)
        FullscreenManager.setFullscreenMode(this, false)
    }
}