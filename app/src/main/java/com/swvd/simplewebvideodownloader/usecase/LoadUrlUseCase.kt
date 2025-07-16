package com.swvd.simplewebvideodownloader.usecase

import android.util.Log
import android.webkit.WebView

/**
 * URL 로드 유스케이스
 * URL 유효성 검사, 정규화, 로드 과정을 담당
 */
class LoadUrlUseCase {
    
    companion object {
        private const val TAG = "LoadUrlUseCase"
    }
    
    /**
     * URL 로드 실행
     * @param webView 대상 WebView
     * @param inputUrl 입력된 URL
     * @param onUrlUpdated URL 업데이트 콜백
     * @param onHistoryAdded 히스토리 추가 콜백
     * @return 로드 성공 여부
     */
    fun execute(
        webView: WebView?,
        inputUrl: String,
        onUrlUpdated: (String) -> Unit = {},
        onHistoryAdded: (String) -> Unit = {}
    ): Result<String> {
        return try {
            // 1. WebView 유효성 확인
            if (webView == null) {
                return Result.failure(Exception("WebView가 null입니다"))
            }
            
            // 2. URL 유효성 검사
            val validationResult = validateUrl(inputUrl)
            if (validationResult.isFailure) {
                return validationResult
            }
            
            // 3. URL 정규화
            val normalizedUrl = normalizeUrl(inputUrl)
            
            // 4. URL 로드
            webView.loadUrl(normalizedUrl)
            
            // 5. 콜백 실행
            onUrlUpdated(normalizedUrl)
            onHistoryAdded(normalizedUrl)
            
            Log.d(TAG, "URL 로드 완료: $normalizedUrl")
            Result.success(normalizedUrl)
            
        } catch (e: Exception) {
            Log.e(TAG, "URL 로드 실패: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * URL 유효성 검사
     */
    private fun validateUrl(url: String): Result<String> {
        return when {
            url.isBlank() -> {
                Result.failure(Exception("URL이 비어있습니다"))
            }
            url.contains("javascript:") -> {
                Result.failure(Exception("JavaScript URL은 지원하지 않습니다"))
            }
            url.contains(" ") && !url.startsWith("http") -> {
                Result.failure(Exception("공백이 포함된 잘못된 URL입니다"))
            }
            else -> Result.success(url)
        }
    }
    
    /**
     * URL 정규화
     * HTTP/HTTPS 프로토콜 추가, 공백 제거 등
     */
    private fun normalizeUrl(url: String): String {
        val trimmedUrl = url.trim()
        
        return when {
            trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://") -> {
                trimmedUrl
            }
            trimmedUrl.startsWith("www.") -> {
                "https://$trimmedUrl"
            }
            trimmedUrl.contains(".") && !trimmedUrl.contains(" ") -> {
                "https://$trimmedUrl"
            }
            else -> {
                // 검색어로 처리
                "https://www.google.com/search?q=${trimmedUrl.replace(" ", "+")}"
            }
        }
    }
    
    /**
     * URL 프리로드 (미리 DNS 조회 등)
     */
    fun preloadUrl(url: String): Boolean {
        return try {
            val normalizedUrl = normalizeUrl(url)
            // 실제 구현에서는 DNS 프리로드 등을 수행
            Log.d(TAG, "URL 프리로드: $normalizedUrl")
            true
        } catch (e: Exception) {
            Log.e(TAG, "URL 프리로드 실패: ${e.message}")
            false
        }
    }
    
    /**
     * URL에서 도메인 추출
     */
    fun extractDomain(url: String): String? {
        return try {
            val normalizedUrl = normalizeUrl(url)
            val urlPattern = Regex("https?://([^/]+)")
            urlPattern.find(normalizedUrl)?.groupValues?.get(1)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * URL 단축 여부 확인
     */
    fun isShortUrl(url: String): Boolean {
        val shortUrlDomains = listOf(
            "bit.ly", "tinyurl.com", "t.co", "goo.gl", 
            "ow.ly", "short.link", "cutt.ly"
        )
        
        val domain = extractDomain(url)
        return domain?.let { d ->
            shortUrlDomains.any { d.contains(it, ignoreCase = true) }
        } ?: false
    }
    
    /**
     * 안전한 URL인지 확인 (기본적인 검사)
     */
    fun isSafeUrl(url: String): Boolean {
        val unsafePatterns = listOf(
            "javascript:",
            "data:",
            "file:",
            "chrome:",
            "about:"
        )
        
        return unsafePatterns.none { pattern ->
            url.startsWith(pattern, ignoreCase = true)
        }
    }
}