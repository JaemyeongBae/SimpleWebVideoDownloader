package com.swvd.simplewebvideodownloader.viewmodel

import android.util.Log
import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swvd.simplewebvideodownloader.webview.Mp4Analyzer
import com.swvd.simplewebvideodownloader.analyzer.VideoAnalyzer
import com.swvd.simplewebvideodownloader.models.VideoInfo
import com.swvd.simplewebvideodownloader.models.VideoType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 비디오 감지 뷰모델
 * 웹페이지에서 비디오 분석 및 감지 기능을 담당
 */
class VideoDetectionViewModel : ViewModel() {
    
    companion object {
        private const val TAG = "VideoDetectionViewModel"
    }
    
    // 비디오 분석기들
    private val mp4Analyzer = Mp4Analyzer()
    private val videoAnalyzer = VideoAnalyzer()
    
    // 분석 상태
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()
    
    private val _hasAnalyzed = MutableStateFlow(false)
    val hasAnalyzed: StateFlow<Boolean> = _hasAnalyzed.asStateFlow()
    
    // 감지된 비디오 목록
    private val _videoList = MutableStateFlow<List<VideoInfo>>(emptyList())
    val videoList: StateFlow<List<VideoInfo>> = _videoList.asStateFlow()
    
    // 기존 호환성을 위한 MP4 링크
    private val _mp4Links = MutableStateFlow<List<String>>(emptyList())
    val mp4Links: StateFlow<List<String>> = _mp4Links.asStateFlow()
    
    // 분석 결과 통계
    private val _analysisStats = MutableStateFlow(AnalysisStats())
    val analysisStats: StateFlow<AnalysisStats> = _analysisStats.asStateFlow()
    
    // 마지막 분석 시간
    private val _lastAnalysisTime = MutableStateFlow(0L)
    val lastAnalysisTime: StateFlow<Long> = _lastAnalysisTime.asStateFlow()
    
    /**
     * 비디오 분석 시작
     */
    fun startAnalysis(webView: WebView?) {
        if (_isAnalyzing.value) {
            Log.w(TAG, "이미 분석 중입니다")
            return
        }
        
        if (webView == null) {
            Log.w(TAG, "WebView가 null입니다")
            return
        }
        
        Log.d(TAG, "비디오 분석 시작: ${webView.url}")
        
        _isAnalyzing.value = true
        _hasAnalyzed.value = true
        _lastAnalysisTime.value = System.currentTimeMillis()
        
        // 향상된 비디오 분석기 사용 - 신 버전 메서드 호출
        videoAnalyzer.analyzePageForAllVideos(webView)
        
        // StateFlow를 통해 결과 수집
        viewModelScope.launch {
            videoAnalyzer.detectedVideos.collect { videos ->
                processAnalysisResult(videos)
            }
        }
    }
    
    /**
     * MP4 전용 분석 (기존 호환성)
     */
    fun startMp4Analysis(webView: WebView?) {
        if (_isAnalyzing.value) return
        
        Log.d(TAG, "MP4 분석 시작")
        
        _isAnalyzing.value = true
        _hasAnalyzed.value = true
        
        mp4Analyzer.analyzePageForMp4(webView) { mp4Links ->
            // MP4 링크를 VideoInfo로 변환
            val videoInfoList = mp4Links.map { url ->
                VideoInfo(
                    url = url,
                    type = VideoType.MP4,
                    title = extractTitleFromUrl(url)
                )
            }
            
            processAnalysisResult(videoInfoList)
        }
    }
    
    /**
     * 분석 결과 처리
     */
    private fun processAnalysisResult(videos: List<VideoInfo>) {
        viewModelScope.launch {
            try {
                _videoList.value = videos
                
                // 기존 호환성을 위한 MP4 링크 추출
                _mp4Links.value = videos.filter { it.type == VideoType.MP4 }
                    .map { it.url }
                
                // 분석 통계 업데이트
                updateAnalysisStats(videos)
                
                _isAnalyzing.value = false
                
                Log.d(TAG, "분석 완료 - 총 ${videos.size}개 비디오 감지")
                
            } catch (e: Exception) {
                Log.e(TAG, "분석 결과 처리 오류: ${e.message}", e)
                _isAnalyzing.value = false
            }
        }
    }
    
    /**
     * 분석 통계 업데이트
     */
    private fun updateAnalysisStats(videos: List<VideoInfo>) {
        val stats = AnalysisStats(
            totalVideos = videos.size,
            mp4Count = videos.count { it.type == VideoType.MP4 },
            hlsCount = videos.count { it.type == VideoType.HLS },
            dashCount = videos.count { it.type == VideoType.DASH },
            webmCount = videos.count { it.type == VideoType.WEBM },
            otherCount = videos.count { 
                it.type !in listOf(
                    VideoType.MP4,
                    VideoType.HLS,
                    VideoType.DASH,
                    VideoType.WEBM
                )
            },
            downloadableCount = videos.count { isDownloadable(it) }
        )
        
        _analysisStats.value = stats
    }
    
    /**
     * 다운로드 가능 여부 확인
     */
    fun isDownloadable(video: VideoInfo): Boolean {
        return when (video.type) {
            VideoType.MP4,
            VideoType.WEBM,
            VideoType.MKV,
            VideoType.AVI,
            VideoType.MOV,
            VideoType.FLV -> true
            VideoType.HLS -> true
            VideoType.DASH -> false
            VideoType.YOUTUBE,
            VideoType.VIMEO -> false
            VideoType.UNKNOWN -> 
                video.url.contains("http") && video.url.contains(".")
        }
    }
    
    /**
     * 특정 타입의 비디오만 필터링
     */
    fun getVideosByType(type: VideoType): List<VideoInfo> {
        return _videoList.value.filter { it.type == type }
    }
    
    /**
     * 다운로드 가능한 비디오만 필터링
     */
    fun getDownloadableVideos(): List<VideoInfo> {
        return _videoList.value.filter { isDownloadable(it) }
    }
    
    /**
     * URL에서 제목 추출
     */
    private fun extractTitleFromUrl(url: String): String? {
        return try {
            val path = url.substringAfterLast("/")
            if (path.contains(".")) {
                path.substringBeforeLast(".")
            } else {
                path.take(20)
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 분석 재시도
     */
    fun retryAnalysis(webView: WebView?) {
        Log.d(TAG, "분석 재시도")
        clearResults()
        startAnalysis(webView)
    }
    
    /**
     * 결과 초기화
     */
    fun clearResults() {
        _videoList.value = emptyList()
        _mp4Links.value = emptyList()
        _hasAnalyzed.value = false
        _isAnalyzing.value = false
        _analysisStats.value = AnalysisStats()
        Log.d(TAG, "분석 결과 초기화")
    }
    
    /**
     * 분석 중단
     */
    fun stopAnalysis() {
        _isAnalyzing.value = false
        Log.d(TAG, "분석 중단")
    }
    
    /**
     * 특정 비디오 URL 검증
     */
    fun validateVideoUrl(url: String): Boolean {
        return try {
            url.isNotBlank() && 
            (url.startsWith("http://") || url.startsWith("https://")) &&
            !url.contains("javascript:") &&
            !url.contains("data:")
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 비디오 검색
     */
    fun searchVideos(query: String): List<VideoInfo> {
        if (query.isBlank()) return _videoList.value
        
        return _videoList.value.filter { video ->
            video.url.contains(query, ignoreCase = true) ||
            video.title?.contains(query, ignoreCase = true) == true ||
            video.type.displayName.contains(query, ignoreCase = true)
        }
    }
    
    /**
     * 마지막 분석으로부터 경과 시간 (초)
     */
    fun getTimeSinceLastAnalysis(): Long {
        return if (_lastAnalysisTime.value > 0) {
            (System.currentTimeMillis() - _lastAnalysisTime.value) / 1000
        } else {
            0
        }
    }
}

/**
 * 분석 통계 데이터 클래스
 */
data class AnalysisStats(
    val totalVideos: Int = 0,
    val mp4Count: Int = 0,
    val hlsCount: Int = 0,
    val dashCount: Int = 0,
    val webmCount: Int = 0,
    val otherCount: Int = 0,
    val downloadableCount: Int = 0
) {
    val downloadablePercentage: Float
        get() = if (totalVideos > 0) (downloadableCount.toFloat() / totalVideos) * 100 else 0f
}