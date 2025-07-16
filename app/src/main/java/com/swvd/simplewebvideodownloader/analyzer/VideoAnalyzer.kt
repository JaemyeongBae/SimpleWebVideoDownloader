package com.swvd.simplewebvideodownloader.analyzer

import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import com.swvd.simplewebvideodownloader.webview.Mp4Analyzer
import com.swvd.simplewebvideodownloader.hls.HlsDetector
import com.swvd.simplewebvideodownloader.hls.HlsStream
import com.swvd.simplewebvideodownloader.models.VideoInfo
import com.swvd.simplewebvideodownloader.models.VideoType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

/**
 * 통합 비디오 감지 및 분석 엔진
 * MP4 직접 링크와 HLS 스트리밍을 모두 감지하는 통합 클래스
 * 
 * 주요 기능:
 * - MP4 직접 링크 감지 (기존 기능)
 * - HLS 스트리밍 실시간 감지 (신규 기능)
 * - 네트워크 요청 실시간 모니터링
 * - 즉시 감지 시 콜백 호출
 * - 통합된 비디오 정보 제공
 */
class VideoAnalyzer {
    
    companion object {
        private const val TAG = "VideoAnalyzer"
    }
    
    // 기존 MP4 분석기
    private val mp4Analyzer = Mp4Analyzer()
    
    // 새로운 HLS 감지기
    private val hlsDetector = HlsDetector()
    
    // 감지된 비디오 정보 상태
    private val _detectedVideos = MutableStateFlow<List<VideoInfo>>(emptyList())
    val detectedVideos: StateFlow<List<VideoInfo>> = _detectedVideos
    
    // MP4 링크 상태
    private val _mp4Links = MutableStateFlow<List<String>>(emptyList())
    
    // 즉시 감지 콜백
    private var onVideoDetectedCallback: ((VideoInfo) -> Unit)? = null
    
    init {
        // HLS 스트림과 MP4 링크를 통합하여 비디오 정보 업데이트
        observeVideoSources()
    }
    
    /**
     * 비디오 소스들을 관찰하여 통합된 비디오 정보 생성
     */
    private fun observeVideoSources() {
        // HLS 스트림과 MP4 링크를 결합하여 통합 리스트 생성
        hlsDetector.detectedStreams.combine(_mp4Links) { hlsStreams, mp4Links ->
            val videoList = mutableListOf<VideoInfo>()
            
            // HLS 스트림을 VideoInfo로 변환
            hlsStreams.forEach { stream ->
                videoList.add(
                    VideoInfo(
                        url = stream.url,
                        type = VideoType.HLS,
                        title = stream.title ?: "HLS 스트림",
                        duration = stream.duration?.let { "${it}초" }
                    )
                )
            }
            
            // MP4 링크를 VideoInfo로 변환
            mp4Links.forEach { url ->
                if (videoList.none { it.url == url }) { // 중복 방지
                    videoList.add(
                        VideoInfo(
                            url = url,
                            type = VideoType.MP4,
                            title = extractTitleFromUrl(url)
                        )
                    )
                }
            }
            
            // HLS 스트림을 먼저, 그 다음 MP4 순으로 정렬
            videoList.sortedBy { it.type.ordinal }
            
        }.let { flow ->
            // StateFlow 업데이트를 위한 코루틴 실행
            try {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    // 임시적으로 직접 업데이트 (향후 코루틴 스코프로 개선 필요)
                    updateVideoList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "비디오 목록 업데이트 오류: ${e.message}")
            }
        }
    }
    
    /**
     * 비디오 목록 업데이트 (HLS + MP4 통합)
     */
    private fun updateVideoList() {
        val videoList = mutableListOf<VideoInfo>()
        
        // HLS 스트림을 VideoInfo로 변환
        hlsDetector.detectedStreams.value.forEach { stream ->
            videoList.add(
                VideoInfo(
                    url = stream.url,
                    type = VideoType.HLS,
                    title = stream.title ?: "HLS 스트림",
                    duration = stream.duration?.let { "${it}초" }
                )
            )
        }
        
        // MP4 링크를 VideoInfo로 변환
        _mp4Links.value.forEach { url ->
            if (videoList.none { it.url == url }) { // 중복 방지
                videoList.add(
                    VideoInfo(
                        url = url,
                        type = VideoType.MP4,
                        title = extractTitleFromUrl(url)
                    )
                )
            }
        }
        
        // HLS 먼저, MP4 나중 순으로 정렬
        val sortedList = videoList.sortedBy { it.type.ordinal }
        _detectedVideos.value = sortedList
        
        Log.i(TAG, "통합 비디오 목록 업데이트: ${sortedList.size}개")
    }
    
    /**
     * 향상된 WebViewClient 생성 (HLS + MP4 감지)
     * @param onPageFinished 페이지 로딩 완료 콜백
     * @param onVideoDetected 비디오 즉시 감지 콜백
     */
    fun createEnhancedWebViewClient(
        onPageFinished: (String?) -> Unit = {},
        onVideoDetected: ((VideoInfo) -> Unit)? = null
    ): WebViewClient {
        
        this.onVideoDetectedCallback = onVideoDetected
        
        return hlsDetector.createEnhancedWebViewClient(
            onPageFinished = { url ->
                Log.d(TAG, "페이지 로딩 완료, 통합 비디오 감지 시작: $url")
                
                // 페이지 로딩 완료 콜백 호출
                onPageFinished(url)
                
                // MP4 감지도 함께 실행 (1초 후)
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    analyzePageForMp4(null) // WebView는 페이지 완료 시점에 전달되지 않음
                }, 1000)
            },
            onHlsDetected = { hlsStream ->
                Log.i(TAG, "HLS 스트림 즉시 감지됨: ${hlsStream.title} - ${hlsStream.url}")
                
                // VideoInfo로 변환하여 즉시 콜백 호출
                val videoInfo = VideoInfo(
                    url = hlsStream.url,
                    type = VideoType.HLS,
                    title = hlsStream.title ?: "HLS 스트림",
                    duration = hlsStream.duration?.let { "${it}초" }
                )
                
                onVideoDetectedCallback?.invoke(videoInfo)
            }
        )
    }
    
    /**
     * 페이지에서 MP4 비디오 분석
     * @param webView 분석할 WebView (null일 경우 마지막 활성 WebView 사용)
     */
    fun analyzePageForMp4(webView: WebView?) {
        Log.d(TAG, "MP4 분석 시작")
        
        mp4Analyzer.analyzePageForMp4(webView) { mp4Links ->
            Log.i(TAG, "MP4 링크 ${mp4Links.size}개 발견")
            
            // 유효한 MP4 링크만 필터링
            val validMp4Links = mp4Links.filter { link ->
                link.contains(".mp4") && 
                link.contains("http") && 
                !link.contains("JavaScript 오류")
            }
            
            _mp4Links.value = validMp4Links
            
            // 새로 발견된 MP4 링크에 대해 즉시 콜백 호출
            validMp4Links.forEach { url ->
                val videoInfo = VideoInfo(
                    url = url,
                    type = VideoType.MP4,
                    title = extractTitleFromUrl(url)
                )
                
                onVideoDetectedCallback?.invoke(videoInfo)
            }
        }
    }
    
    /**
     * 통합 비디오 분석 실행
     * MP4와 HLS를 모두 분석
     * @param webView 분석할 WebView
     */
    fun analyzePageForAllVideos(webView: WebView?) {
        Log.i(TAG, "통합 비디오 분석 시작 (MP4 + HLS)")
        
        // MP4 분석 실행 (HLS는 자동으로 실시간 감지됨)
        analyzePageForMp4(webView)
    }
    
    /**
     * URL에서 제목 추출
     * @param url 비디오 URL
     * @return 추출된 제목
     */
    private fun extractTitleFromUrl(url: String): String {
        return try {
            val fileName = url.substringAfterLast("/").substringBefore("?")
            when {
                fileName.contains(".mp4") -> fileName.substringBefore(".mp4")
                fileName.contains(".m3u8") -> fileName.substringBefore(".m3u8")
                fileName.length > 20 -> fileName.take(20) + "..."
                else -> fileName.ifEmpty { "비디오" }
            }
        } catch (e: Exception) {
            "비디오"
        }
    }
    
    /**
     * 감지된 비디오 목록 초기화
     */
    fun clearDetectedVideos() {
        _detectedVideos.value = emptyList()
        _mp4Links.value = emptyList()
        hlsDetector.clearDetectedStreams()
        Log.d(TAG, "감지된 비디오 목록 초기화")
    }
    
    /**
     * 특정 타입의 비디오만 필터링
     * @param type 필터링할 비디오 타입
     * @return 필터링된 비디오 목록
     */
    fun getVideosByType(type: VideoType): List<VideoInfo> {
        return _detectedVideos.value.filter { it.type == type }
    }
    
    /**
     * 현재 감지 상태 정보
     */
    val detectionStatus: String
        get() {
            val current = _detectedVideos.value
            val mp4Count = current.count { it.type == VideoType.MP4 }
            val hlsCount = current.count { it.type == VideoType.HLS }
            
            return "MP4: ${mp4Count}개, HLS: ${hlsCount}개"
        }
}

