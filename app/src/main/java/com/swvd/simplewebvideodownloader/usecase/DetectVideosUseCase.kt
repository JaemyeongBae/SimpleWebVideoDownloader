package com.swvd.simplewebvideodownloader.usecase

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebView
import com.swvd.simplewebvideodownloader.analyzer.VideoAnalyzer
import com.swvd.simplewebvideodownloader.models.VideoInfo
import com.swvd.simplewebvideodownloader.models.VideoType
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 비디오 감지 유스케이스
 * 웹페이지에서 비디오를 감지하고 분석하는 과정을 관리
 */
class DetectVideosUseCase {
    
    companion object {
        private const val TAG = "DetectVideosUseCase"
        private const val DEFAULT_DELAY = 1000L // 기본 지연 시간
        private const val RETRY_DELAY = 2000L // 재시도 지연 시간
        private const val MAX_RETRIES = 3 // 최대 재시도 횟수
    }
    
    private val videoAnalyzer = VideoAnalyzer()
    
    /**
     * 비디오 감지 실행
     * @param webView 대상 WebView
     * @param delayMs 감지 시작 전 지연 시간
     * @param enableRetry 실패 시 재시도 여부
     * @return 감지된 비디오 목록
     */
    suspend fun execute(
        webView: WebView?,
        delayMs: Long = DEFAULT_DELAY,
        enableRetry: Boolean = true
    ): Result<List<VideoInfo>> {
        return try {
            // 1. WebView 유효성 확인
            if (webView == null) {
                return Result.failure(Exception("WebView가 null입니다"))
            }
            
            // 2. 페이지 로딩 대기
            if (delayMs > 0) {
                kotlinx.coroutines.delay(delayMs)
            }
            
            // 3. 비디오 감지 실행
            val videos = detectVideosWithRetry(webView, enableRetry)
            
            // 4. 결과 검증 및 필터링
            val validVideos = filterValidVideos(videos)
            
            Log.d(TAG, "비디오 감지 완료: ${validVideos.size}개 발견")
            Result.success(validVideos)
            
        } catch (e: Exception) {
            Log.e(TAG, "비디오 감지 실패: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 재시도 기능이 있는 비디오 감지
     */
    private suspend fun detectVideosWithRetry(
        webView: WebView,
        enableRetry: Boolean,
        currentRetry: Int = 0
    ): List<VideoInfo> {
        return try {
            val videos = performVideoDetection(webView)
            
            // 비디오가 발견되지 않고 재시도가 활성화된 경우
            if (videos.isEmpty() && enableRetry && currentRetry < MAX_RETRIES) {
                Log.d(TAG, "비디오 미발견, 재시도 ${currentRetry + 1}/${MAX_RETRIES}")
                kotlinx.coroutines.delay(RETRY_DELAY)
                return detectVideosWithRetry(webView, enableRetry, currentRetry + 1)
            }
            
            videos
            
        } catch (e: Exception) {
            if (enableRetry && currentRetry < MAX_RETRIES) {
                Log.w(TAG, "감지 실패, 재시도 ${currentRetry + 1}/${MAX_RETRIES}: ${e.message}")
                kotlinx.coroutines.delay(RETRY_DELAY)
                return detectVideosWithRetry(webView, enableRetry, currentRetry + 1)
            } else {
                throw e
            }
        }
    }
    
    /**
     * 실제 비디오 감지 수행
     */
    private suspend fun performVideoDetection(webView: WebView): List<VideoInfo> {
        return suspendCancellableCoroutine { continuation ->
            try {
                // 신 버전 VideoAnalyzer 사용
                videoAnalyzer.analyzePageForAllVideos(webView)
                
                // StateFlow에서 결과 수집 (한 번만)
                var collected = false
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    videoAnalyzer.detectedVideos.collect { videos ->
                        if (!collected && videos.isNotEmpty()) {
                            collected = true
                            continuation.resume(videos)
                        }
                    }
                }
                
                // 타임아웃 설정 (5초)
                Handler(Looper.getMainLooper()).postDelayed({
                    if (!collected) {
                        collected = true
                        continuation.resume(emptyList())
                    }
                }, 5000)
                
            } catch (e: Exception) {
                continuation.cancel(e)
            }
        }
    }
    
    /**
     * 유효한 비디오만 필터링
     */
    private fun filterValidVideos(videos: List<VideoInfo>): List<VideoInfo> {
        return videos.filter { video ->
            isValidVideo(video)
        }.distinctBy { it.url } // 중복 URL 제거
    }
    
    /**
     * 비디오 유효성 검사
     */
    private fun isValidVideo(video: VideoInfo): Boolean {
        return when {
            video.url.isBlank() -> false
            video.url.contains("javascript:", ignoreCase = true) -> false
            video.url.contains("data:", ignoreCase = true) -> false
            video.url.length < 10 -> false
            !video.url.startsWith("http") -> false
            video.url.contains("error", ignoreCase = true) -> false
            else -> true
        }
    }
    
    /**
     * 특정 타입의 비디오만 감지
     */
    suspend fun detectSpecificType(
        webView: WebView?,
        targetType: VideoType,
        delayMs: Long = DEFAULT_DELAY
    ): Result<List<VideoInfo>> {
        return try {
            val allVideos = execute(webView, delayMs, enableRetry = true)
            if (allVideos.isSuccess) {
                val filteredVideos = allVideos.getOrNull()?.filter { it.type == targetType } ?: emptyList()
                Result.success(filteredVideos)
            } else {
                allVideos
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 다운로드 가능한 비디오만 감지
     */
    suspend fun detectDownloadableVideos(
        webView: WebView?,
        delayMs: Long = DEFAULT_DELAY
    ): Result<List<VideoInfo>> {
        return try {
            val allVideos = execute(webView, delayMs, enableRetry = true)
            if (allVideos.isSuccess) {
                val downloadableVideos = allVideos.getOrNull()?.filter { video ->
                    isDownloadableType(video.type)
                } ?: emptyList()
                Result.success(downloadableVideos)
            } else {
                allVideos
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 다운로드 가능한 타입인지 확인
     */
    private fun isDownloadableType(type: VideoType): Boolean {
        return when (type) {
            VideoType.MP4,
            VideoType.WEBM,
            VideoType.MKV,
            VideoType.AVI,
            VideoType.MOV,
            VideoType.FLV,
            VideoType.HLS -> true
            else -> false
        }
    }
    
    /**
     * 페이지 변경 후 자동 감지
     */
    suspend fun autoDetectOnPageChange(
        webView: WebView?,
        oldUrl: String,
        newUrl: String
    ): Result<List<VideoInfo>> {
        return try {
            // URL이 실제로 변경되었는지 확인
            if (oldUrl == newUrl) {
                return Result.success(emptyList())
            }
            
            // 새 페이지 로딩 대기
            kotlinx.coroutines.delay(1500)
            
            // 비디오 감지 실행
            execute(webView, delayMs = 0, enableRetry = true)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 백그라운드에서 주기적 감지
     */
    fun startPeriodicDetection(
        webView: WebView?,
        intervalMs: Long = 30000L, // 30초마다
        onVideosDetected: (List<VideoInfo>) -> Unit
    ) {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                webView?.let { view ->
                    videoAnalyzer.analyzePageForAllVideos(view)
                    // StateFlow를 통해 결과 수집
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        videoAnalyzer.detectedVideos.collect { videos ->
                            val validVideos = filterValidVideos(videos)
                            if (validVideos.isNotEmpty()) {
                                onVideosDetected(validVideos)
                            }
                        }
                    }
                }
                handler.postDelayed(this, intervalMs)
            }
        }
        handler.post(runnable)
    }
    
    /**
     * 감지 결과 분석
     */
    fun analyzeDetectionResult(videos: List<VideoInfo>): DetectionAnalysis {
        val totalCount = videos.size
        val typeCount = videos.groupBy { it.type }.mapValues { it.value.size }
        val downloadableCount = videos.count { isDownloadableType(it.type) }
        val hasTitle = videos.count { !it.title.isNullOrBlank() }
        
        return DetectionAnalysis(
            totalVideos = totalCount,
            videosByType = typeCount,
            downloadableVideos = downloadableCount,
            videosWithTitle = hasTitle,
            detectionQuality = calculateDetectionQuality(videos)
        )
    }
    
    /**
     * 감지 품질 계산
     */
    private fun calculateDetectionQuality(videos: List<VideoInfo>): DetectionQuality {
        if (videos.isEmpty()) return DetectionQuality.POOR
        
        val downloadableRatio = videos.count { isDownloadableType(it.type) }.toFloat() / videos.size
        val titleRatio = videos.count { !it.title.isNullOrBlank() }.toFloat() / videos.size
        
        return when {
            downloadableRatio >= 0.8 && titleRatio >= 0.5 -> DetectionQuality.EXCELLENT
            downloadableRatio >= 0.6 && titleRatio >= 0.3 -> DetectionQuality.GOOD
            downloadableRatio >= 0.3 -> DetectionQuality.FAIR
            else -> DetectionQuality.POOR
        }
    }
}

/**
 * 감지 분석 결과
 */
data class DetectionAnalysis(
    val totalVideos: Int,
    val videosByType: Map<VideoType, Int>,
    val downloadableVideos: Int,
    val videosWithTitle: Int,
    val detectionQuality: DetectionQuality
)

/**
 * 감지 품질
 */
enum class DetectionQuality {
    EXCELLENT, GOOD, FAIR, POOR
}