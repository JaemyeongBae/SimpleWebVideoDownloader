package com.swvd.simplewebvideodownloader.hls

import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * HLS 스트림 감지 엔진
 * WebView의 네트워크 요청을 모니터링하여 M3U8 파일을 실시간으로 감지
 * 
 * 주요 기능:
 * - 네트워크 요청 실시간 감지
 * - M3U8 URL 중복 방지
 * - JavaScript 기반 추가 감지
 * - 페이지 로딩 완료 시 자동 감지
 */
class HlsDetector {
    
    companion object {
        private const val TAG = "HlsDetector"
    }
    
    // 감지된 HLS 스트림 목록 상태
    private val _detectedStreams = MutableStateFlow<List<HlsStream>>(emptyList())
    val detectedStreams: StateFlow<List<HlsStream>> = _detectedStreams
    
    /**
     * HLS 감지 기능이 강화된 WebViewClient 생성
     * @param onPageFinished 페이지 로딩 완료 콜백
     * @param onHlsDetected HLS 감지 즉시 콜백
     */
    fun createEnhancedWebViewClient(
        onPageFinished: (String?) -> Unit = {},
        onHlsDetected: (HlsStream) -> Unit = {}
    ): WebViewClient {
        return object : WebViewClient() {
            
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): android.webkit.WebResourceResponse? {
                
                request?.url?.toString()?.let { url ->
                    Log.d(TAG, "네트워크 요청 감지: $url")
                    
                    if (isHlsUrl(url)) {
                        Log.i(TAG, "HLS URL 감지됨: $url")
                        val stream = addDetectedStream(url)
                        stream?.let { onHlsDetected(it) }
                    }
                }
                
                return super.shouldInterceptRequest(view, request)
            }
            
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                
                Log.d(TAG, "페이지 로딩 완료: $url")
                onPageFinished(url)
                
                // JavaScript로 추가 HLS 감지 (1초 후 실행)
                view?.postDelayed({
                    view.evaluateJavascript(hlsDetectionScript) { result ->
                        parseJavaScriptResult(result, onHlsDetected)
                    }
                }, 1000)
            }
        }
    }
    
    /**
     * URL이 HLS 관련인지 확인
     * @param url 확인할 URL
     * @return HLS URL 여부
     */
    private fun isHlsUrl(url: String): Boolean {
        val lowerUrl = url.lowercase()
        
        return lowerUrl.contains(".m3u8") || 
               lowerUrl.contains("/hls/") ||
               lowerUrl.contains("application/x-mpegurl") ||
               lowerUrl.contains("application/vnd.apple.mpegurl") ||
               (lowerUrl.contains("playlist") && lowerUrl.contains("m3u"))
    }
    
    /**
     * 감지된 HLS 스트림 추가 (중복 방지)
     * @param url HLS URL
     * @return 새로 추가된 스트림 또는 null (중복인 경우)
     */
    private fun addDetectedStream(url: String): HlsStream? {
        val existingStreams = _detectedStreams.value
        
        // 중복 확인
        if (existingStreams.any { it.url == url }) {
            Log.d(TAG, "이미 감지된 HLS URL: $url")
            return null
        }
        
        val newStream = HlsStream(
            url = url,
            detectedAt = System.currentTimeMillis(),
            title = extractTitleFromUrl(url)
        )
        
        _detectedStreams.value = existingStreams + newStream
        Log.i(TAG, "새 HLS 스트림 추가: ${newStream.title} - $url")
        
        return newStream
    }
    
    /**
     * URL에서 제목 추출
     * @param url HLS URL
     * @return 추출된 제목
     */
    private fun extractTitleFromUrl(url: String): String {
        return try {
            val fileName = url.substringAfterLast("/").substringBefore("?")
            when {
                fileName.contains("playlist") -> "HLS 플레이리스트"
                fileName.contains("master") -> "마스터 플레이리스트"
                fileName.contains(".m3u8") -> fileName.substringBefore(".m3u8")
                else -> "HLS 스트림"
            }
        } catch (e: Exception) {
            "HLS 스트림"
        }
    }
    
    /**
     * JavaScript 결과 파싱
     * @param result JavaScript 실행 결과
     * @param onHlsDetected HLS 감지 콜백
     */
    private fun parseJavaScriptResult(result: String?, onHlsDetected: (HlsStream) -> Unit) {
        try {
            if (result.isNullOrEmpty() || result == "null") return
            
            val cleanResult = result.replace("\\\"", "\"").removeSurrounding("\"")
            Log.d(TAG, "JavaScript HLS 검색 결과: $cleanResult")
            
            if (cleanResult.startsWith("[") && cleanResult.endsWith("]")) {
                val urls = cleanResult.removeSurrounding("[", "]")
                    .split(",")
                    .map { it.trim().removeSurrounding("\"") }
                    .filter { it.isNotEmpty() && isHlsUrl(it) }
                
                urls.forEach { url ->
                    val stream = addDetectedStream(url)
                    stream?.let { onHlsDetected(it) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "JavaScript 결과 파싱 오류: ${e.message}")
        }
    }
    
    /**
     * 모든 감지된 스트림 초기화
     */
    fun clearDetectedStreams() {
        _detectedStreams.value = emptyList()
        Log.d(TAG, "감지된 HLS 스트림 목록 초기화")
    }
    
    /**
     * JavaScript HLS 감지 스크립트
     * 다양한 HLS 플레이어 및 구현 방식을 감지
     */
    private val hlsDetectionScript = """
        (function() {
            const results = [];
            
            try {
                // 1. Video 태그 src 확인
                document.querySelectorAll('video').forEach(v => {
                    if (v.src && v.src.includes('.m3u8')) {
                        results.push(v.src);
                    }
                    if (v.currentSrc && v.currentSrc.includes('.m3u8')) {
                        results.push(v.currentSrc);
                    }
                });
                
                // 2. Source 태그 확인
                document.querySelectorAll('source').forEach(s => {
                    if (s.src && s.src.includes('.m3u8')) {
                        results.push(s.src);
                    }
                    if (s.getAttribute('type') === 'application/x-mpegURL' && s.src) {
                        results.push(s.src);
                    }
                });
                
                // 3. HLS.js 감지
                if (typeof Hls !== 'undefined' && window.hls) {
                    const url = window.hls.url || window.hls.media?.src;
                    if (url && url.includes('.m3u8')) {
                        results.push(url);
                    }
                }
                
                // 4. 전역 플레이어 변수 확인
                ['player', 'videoPlayer', 'hlsPlayer', 'jwplayer'].forEach(name => {
                    const player = window[name];
                    if (player) {
                        // JW Player
                        if (typeof player.getPlaylist === 'function') {
                            try {
                                const playlist = player.getPlaylist();
                                if (playlist && playlist[0] && playlist[0].file) {
                                    const file = playlist[0].file;
                                    if (file.includes('.m3u8')) {
                                        results.push(file);
                                    }
                                }
                            } catch (e) {}
                        }
                        
                        // 일반 플레이어
                        if (player.src && player.src.includes('.m3u8')) {
                            results.push(player.src);
                        }
                        if (player.currentSrc && player.currentSrc.includes('.m3u8')) {
                            results.push(player.currentSrc);
                        }
                    }
                });
                
                // 5. data 속성에서 HLS URL 찾기
                document.querySelectorAll('[data-src], [data-url], [data-video]').forEach(el => {
                    const dataSrc = el.getAttribute('data-src') || 
                                   el.getAttribute('data-url') || 
                                   el.getAttribute('data-video');
                    if (dataSrc && dataSrc.includes('.m3u8')) {
                        results.push(dataSrc);
                    }
                });
                
                // 6. 페이지 HTML에서 m3u8 URL 정규식 검색
                const htmlContent = document.documentElement.outerHTML;
                const m3u8Regex = /https?:\/\/[^\s"'<>()]+\.m3u8[^\s"'<>()]*/gi;
                const matches = htmlContent.match(m3u8Regex);
                if (matches) {
                    matches.forEach(match => {
                        const cleanUrl = match.replace(/['"<>()]+$/, '');
                        if (cleanUrl.length > 20) {
                            results.push(cleanUrl);
                        }
                    });
                }
                
                // 중복 제거
                return JSON.stringify([...new Set(results)]);
                
            } catch (e) {
                console.error('HLS 감지 오류:', e);
                return JSON.stringify([]);
            }
        })();
    """.trimIndent()
}

/**
 * HLS 스트림 정보
 * @param url HLS 스트림 URL
 * @param detectedAt 감지 시각 (타임스탬프)
 * @param title 스트림 제목
 * @param duration 영상 길이 (초, 옵션)
 * @param variants 다양한 화질 옵션 (옵션)
 */
data class HlsStream(
    val url: String,
    val detectedAt: Long,
    val title: String? = null,
    val duration: Long? = null,
    val variants: List<HlsVariant>? = null
)

/**
 * HLS 변형 (화질 옵션)
 * @param url 특정 화질의 스트림 URL
 * @param bandwidth 대역폭 (bps)
 * @param resolution 해상도 (예: "1920x1080")
 * @param codecs 코덱 정보 (옵션)
 */
data class HlsVariant(
    val url: String,
    val bandwidth: Int,
    val resolution: String,
    val codecs: String?
)