package com.swvd.simplewebvideodownloader.viewmodel

import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * WebView 관리 뷰모델
 * WebView의 상태, 네비게이션, URL 처리를 담당
 */
class WebViewViewModel : ViewModel() {
    
    companion object {
        private const val TAG = "WebViewViewModel"
    }
    
    // URL 관련 상태
    private val _urlText = MutableStateFlow("")
    val urlText: StateFlow<String> = _urlText.asStateFlow()
    
    private val _currentUrl = MutableStateFlow("")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()
    
    // 네비게이션 상태
    private val _canGoBack = MutableStateFlow(false)
    val canGoBack: StateFlow<Boolean> = _canGoBack.asStateFlow()
    
    private val _canGoForward = MutableStateFlow(false)
    val canGoForward: StateFlow<Boolean> = _canGoForward.asStateFlow()
    
    // 전체화면 상태
    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()
    
    // WebView 상태
    private val _webViewState = MutableStateFlow<Bundle?>(null)
    val webViewState: StateFlow<Bundle?> = _webViewState.asStateFlow()
    
    // URL 히스토리
    private val _urlHistory = MutableStateFlow<List<String>>(emptyList())
    val urlHistory: StateFlow<List<String>> = _urlHistory.asStateFlow()
    
    // 페이지 로딩 상태
    private val _isPageLoading = MutableStateFlow(false)
    val isPageLoading: StateFlow<Boolean> = _isPageLoading.asStateFlow()
    
    // 페이지 로딩 진행률
    private val _loadingProgress = MutableStateFlow(0)
    val loadingProgress: StateFlow<Int> = _loadingProgress.asStateFlow()
    
    // 페이지 제목
    private val _pageTitle = MutableStateFlow("")
    val pageTitle: StateFlow<String> = _pageTitle.asStateFlow()
    
    /**
     * URL 텍스트 업데이트
     */
    fun updateUrlText(text: String) {
        _urlText.value = text
    }
    
    /**
     * 현재 URL 업데이트
     */
    fun updateCurrentUrl(url: String) {
        if (url != _currentUrl.value && 
            !url.startsWith("data:") && 
            !url.startsWith("about:") &&
            url != "about:blank") {
            
            _currentUrl.value = url
            _urlText.value = url
            
            // 히스토리에 추가
            addToHistory(url)
            
            Log.d(TAG, "URL 업데이트: $url")
        }
    }
    
    /**
     * URL 히스토리에 추가
     */
    private fun addToHistory(url: String) {
        val currentHistory = _urlHistory.value.toMutableList()
        if (!currentHistory.contains(url)) {
            currentHistory.add(url)
            // 최대 20개까지만 유지
            if (currentHistory.size > 20) {
                currentHistory.removeAt(0)
            }
            _urlHistory.value = currentHistory
        }
    }
    
    /**
     * URL 유효성 검사 및 정규화
     */
    fun normalizeUrl(url: String): String {
        val trimmedUrl = url.trim()
        return if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
            "https://$trimmedUrl"
        } else {
            trimmedUrl
        }
    }
    
    /**
     * 네비게이션 상태 업데이트
     */
    fun updateNavigationState(webView: WebView?) {
        webView?.let { view ->
            val oldCanGoBack = _canGoBack.value
            val oldCanGoForward = _canGoForward.value
            
            _canGoBack.value = view.canGoBack()
            _canGoForward.value = view.canGoForward()
            
            Log.d(TAG, "네비게이션 상태 업데이트 - 뒤로: $oldCanGoBack → ${_canGoBack.value}, " +
                    "앞으로: $oldCanGoForward → ${_canGoForward.value}")
        }
    }
    
    /**
     * WebView 상태 저장
     */
    fun saveWebViewState(webView: WebView?) {
        webView?.let { view ->
            val bundle = Bundle()
            view.saveState(bundle)
            _webViewState.value = bundle
            
            // 현재 URL도 업데이트
            view.url?.let { url ->
                updateCurrentUrl(url)
            }
            
            Log.d(TAG, "WebView 상태 저장 완료")
        }
    }
    
    /**
     * WebView 상태 복원
     */
    fun restoreWebViewState(webView: WebView?) {
        webView?.let { view ->
            _webViewState.value?.let { bundle ->
                view.restoreState(bundle)
                Log.d(TAG, "WebView 상태 복원 완료")
            }
        }
    }
    
    /**
     * 전체화면 모드 토글
     */
    fun toggleFullscreen() {
        _isFullscreen.value = !_isFullscreen.value
    }
    
    fun setFullscreen(fullscreen: Boolean) {
        _isFullscreen.value = fullscreen
    }
    
    /**
     * 페이지 로딩 상태 관리
     */
    fun onPageStarted(url: String) {
        _isPageLoading.value = true
        _loadingProgress.value = 0
        updateCurrentUrl(url)
        Log.d(TAG, "페이지 로딩 시작: $url")
    }
    
    fun onPageFinished(url: String) {
        _isPageLoading.value = false
        _loadingProgress.value = 100
        updateCurrentUrl(url)
        Log.d(TAG, "페이지 로딩 완료: $url")
    }
    
    fun onProgressChanged(progress: Int) {
        _loadingProgress.value = progress
    }
    
    fun onReceivedTitle(title: String) {
        _pageTitle.value = title
    }
    
    /**
     * 페이지 새로고침
     */
    fun refreshPage(webView: WebView?) {
        webView?.reload()
        Log.d(TAG, "페이지 새로고침")
    }
    
    /**
     * 뒤로 가기
     */
    fun goBack(webView: WebView?) {
        webView?.let { view ->
            if (view.canGoBack()) {
                view.goBack()
                Log.d(TAG, "뒤로 가기 실행")
                
                // 상태 업데이트를 위한 지연 실행
                viewModelScope.launch {
                    kotlinx.coroutines.delay(100)
                    updateNavigationState(view)
                }
            }
        }
    }
    
    /**
     * 앞으로 가기
     */
    fun goForward(webView: WebView?) {
        webView?.let { view ->
            if (view.canGoForward()) {
                view.goForward()
                Log.d(TAG, "앞으로 가기 실행")
                
                // 상태 업데이트를 위한 지연 실행
                viewModelScope.launch {
                    kotlinx.coroutines.delay(100)
                    updateNavigationState(view)
                }
            }
        }
    }
    
    /**
     * URL 로드
     */
    fun loadUrl(webView: WebView?, url: String? = null) {
        webView?.let { view ->
            val targetUrl = url ?: normalizeUrl(_urlText.value)
            if (targetUrl.isNotBlank()) {
                view.loadUrl(targetUrl)
                updateCurrentUrl(targetUrl)
                Log.d(TAG, "URL 로드: $targetUrl")
            }
        }
    }
    
    /**
     * 페이지 정지
     */
    fun stopLoading(webView: WebView?) {
        webView?.stopLoading()
        _isPageLoading.value = false
        Log.d(TAG, "페이지 로딩 정지")
    }
    
    /**
     * 홈 페이지로 이동
     */
    fun goHome(webView: WebView?) {
        loadUrl(webView, "https://www.google.com")
    }
    
    /**
     * 현재 페이지 URL 복사
     */
    fun getCurrentPageUrl(): String {
        return _currentUrl.value
    }
    
    /**
     * URL 히스토리 초기화
     */
    fun clearHistory() {
        _urlHistory.value = emptyList()
        Log.d(TAG, "URL 히스토리 초기화")
    }
    
    /**
     * 모든 상태 초기화
     */
    fun resetAll() {
        _urlText.value = ""
        _currentUrl.value = ""
        _canGoBack.value = false
        _canGoForward.value = false
        _isFullscreen.value = false
        _webViewState.value = null
        _urlHistory.value = emptyList()
        _isPageLoading.value = false
        _loadingProgress.value = 0
        _pageTitle.value = ""
        Log.d(TAG, "모든 상태 초기화")
    }
    
    /**
     * 사용자 에이전트 설정
     */
    fun setupUserAgent(webView: WebView?) {
        webView?.let { view ->
            view.settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; SM-G975F) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Mobile Safari/537.36"
        }
    }
    
    /**
     * 자바스크립트 실행
     */
    fun executeJavaScript(webView: WebView?, script: String, callback: ((String) -> Unit)? = null) {
        webView?.evaluateJavascript(script) { result ->
            callback?.invoke(result)
        }
    }
}