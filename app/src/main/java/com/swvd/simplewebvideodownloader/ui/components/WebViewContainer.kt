package com.swvd.simplewebvideodownloader.ui.components

import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * WebView 컨테이너 컴포넌트
 * WebView 설정 및 관리를 담당
 */
@Composable
fun WebViewContainer(
    modifier: Modifier = Modifier,
    currentUrl: String,
    webViewState: Bundle? = null,
    onWebViewCreated: (WebView) -> Unit = {},
    onUrlChanged: (String) -> Unit = {},
    onPageStarted: (String) -> Unit = {},
    onPageFinished: (String) -> Unit = {},
    onTitleReceived: (String) -> Unit = {},
    onProgressChanged: (Int) -> Unit = {}
) {
    if (currentUrl.isEmpty()) {
        // URL이 없을 때 플레이스홀더 표시
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🌐",
                    style = MaterialTheme.typography.displayMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "웹 페이지를 로드하면 여기에 브라우저가 표시됩니다",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    // WebView 기본 설정
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                        allowFileAccess = false
                        allowContentAccess = false
                        
                        // 사용자 에이전트 설정
                        userAgentString = "Mozilla/5.0 (Linux; Android 10; SM-G975F) " +
                                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Mobile Safari/537.36"
                    }
                    
                    // WebViewClient 설정
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                            url?.let { newUrl ->
                                onUrlChanged(newUrl)
                            }
                            return false
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            url?.let { newUrl ->
                                onPageStarted(newUrl)
                                onUrlChanged(newUrl)
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            url?.let { newUrl ->
                                onPageFinished(newUrl)
                                onUrlChanged(newUrl)
                            }
                        }
                        
                        override fun onReceivedError(
                            view: WebView?, 
                            errorCode: Int, 
                            description: String?, 
                            failingUrl: String?
                        ) {
                            super.onReceivedError(view, errorCode, description, failingUrl)
                            // 에러 처리 로직 추가 가능
                        }
                    }
                    
                    // WebChromeClient 설정
                    webChromeClient = object : WebChromeClient() {
                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            super.onReceivedTitle(view, title)
                            title?.let { onTitleReceived(it) }
                        }
                        
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            onProgressChanged(newProgress)
                        }
                        
                        override fun onReceivedIcon(view: WebView?, icon: android.graphics.Bitmap?) {
                            super.onReceivedIcon(view, icon)
                            // 파비콘 처리 로직 추가 가능
                        }
                    }
                    
                    // WebView 생성 콜백 호출
                    onWebViewCreated(this)
                    
                    // 상태 복원 또는 URL 로드
                    webViewState?.let { bundle ->
                        restoreState(bundle)
                    } ?: run {
                        if (currentUrl.isNotEmpty()) {
                            loadUrl(currentUrl)
                        }
                    }
                }
            },
            update = { webView ->
                // URL이 변경되었을 때 업데이트
                if (currentUrl.isNotEmpty() && webView.url != currentUrl) {
                    webView.loadUrl(currentUrl)
                }
            },
            modifier = modifier.fillMaxSize()
        )
    }
}

/**
 * 전체화면 WebView 컨테이너
 */
@Composable
fun FullscreenWebViewContainer(
    modifier: Modifier = Modifier,
    currentUrl: String,
    webViewState: Bundle? = null,
    syncWebView: WebView? = null,
    onWebViewCreated: (WebView) -> Unit = {},
    onUrlChanged: (String) -> Unit = {},
    onPageStarted: (String) -> Unit = {},
    onPageFinished: (String) -> Unit = {},
    onTitleReceived: (String) -> Unit = {},
    onProgressChanged: (Int) -> Unit = {}
) {
    if (currentUrl.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🌐",
                    style = MaterialTheme.typography.displayLarge
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "웹 페이지가 로드되지 않았습니다",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    // 전체화면용 WebView 설정
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                        allowFileAccess = false
                        allowContentAccess = false
                        
                        // 전체화면에 최적화된 설정
                        setSupportMultipleWindows(false)
                        mediaPlaybackRequiresUserGesture = false
                        
                        userAgentString = "Mozilla/5.0 (Linux; Android 10; SM-G975F) " +
                                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Mobile Safari/537.36"
                    }
                    
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                            url?.let { newUrl -> onUrlChanged(newUrl) }
                            return false
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            url?.let { newUrl ->
                                onPageStarted(newUrl)
                                onUrlChanged(newUrl)
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            url?.let { newUrl ->
                                onPageFinished(newUrl)
                                onUrlChanged(newUrl)
                            }
                        }
                    }
                    
                    webChromeClient = object : WebChromeClient() {
                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            super.onReceivedTitle(view, title)
                            title?.let { onTitleReceived(it) }
                        }
                        
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            onProgressChanged(newProgress)
                        }
                    }
                    
                    onWebViewCreated(this)
                    
                    // 상태 복원 또는 동기화
                    webViewState?.let { bundle ->
                        restoreState(bundle)
                    } ?: run {
                        val syncUrl = syncWebView?.url ?: currentUrl
                        if (syncUrl.isNotEmpty()) {
                            loadUrl(syncUrl)
                        }
                    }
                }
            },
            update = { webView ->
                if (currentUrl.isNotEmpty() && webView.url != currentUrl) {
                    webView.loadUrl(currentUrl)
                }
            },
            modifier = modifier.fillMaxSize()
        )
    }
}