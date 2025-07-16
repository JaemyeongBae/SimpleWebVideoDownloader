package com.swvd.simplewebvideodownloader.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swvd.simplewebvideodownloader.download.DownloadHandler
import com.swvd.simplewebvideodownloader.download.HlsDownloader
import com.swvd.simplewebvideodownloader.download.VideoDownloadManager
import com.swvd.simplewebvideodownloader.webview.VideoAnalyzer
import com.swvd.simplewebvideodownloader.models.VideoInfo
import com.swvd.simplewebvideodownloader.models.VideoType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 메인 뷰모델
 * 전체 앱의 상태와 비즈니스 로직을 관리
 */
class MainViewModel(context: Context) : ViewModel() {
    
    // 다운로드 관리자들
    private val downloadHandler = DownloadHandler(context)
    private val hlsDownloader = HlsDownloader(context)
    private val videoDownloadManager = VideoDownloadManager(context)
    
    // 비디오 분석기
    private val videoAnalyzer = VideoAnalyzer()
    
    // UI 상태 관리
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    // 다운로드 결과 메시지
    private val _downloadResult = MutableStateFlow<String?>(null)
    val downloadResult: StateFlow<String?> = _downloadResult.asStateFlow()
    
    // 권한 요청 콜백
    var onRequestPermissions: (() -> Unit)? = null
    
    /**
     * 비디오 분석 시작
     */
    fun startVideoAnalysis(webView: android.webkit.WebView?) {
        if (_uiState.value.isAnalyzing) return
        
        _uiState.value = _uiState.value.copy(
            isAnalyzing = true,
            hasAnalyzed = true
        )
        
        videoAnalyzer.analyzeVideos(webView) { videos ->
            _uiState.value = _uiState.value.copy(
                isAnalyzing = false,
                videoList = videos,
                mp4Links = videos.filter { it.type == VideoType.MP4 }
                    .map { it.url }
            )
        }
    }
    
    /**
     * 비디오 다운로드 시작
     */
    fun downloadVideo(videoInfo: VideoInfo) {
        viewModelScope.launch {
            try {
                // 다운로드 중 상태 추가
                val currentDownloading = _uiState.value.downloadingUrls.toMutableSet()
                currentDownloading.add(videoInfo.url)
                _uiState.value = _uiState.value.copy(downloadingUrls = currentDownloading)
                
                // 비디오 타입에 따른 다운로드 처리
                when (videoInfo.type) {
                    VideoType.HLS -> {
                        downloadHlsVideo(videoInfo)
                    }
                    VideoType.MP4,
                    VideoType.WEBM,
                    VideoType.MKV -> {
                        downloadDirectVideo(videoInfo)
                    }
                    else -> {
                        showDownloadResult("지원하지 않는 비디오 형식입니다")
                        removeFromDownloading(videoInfo.url)
                    }
                }
            } catch (e: Exception) {
                showDownloadResult("다운로드 오류: ${e.message}")
                removeFromDownloading(videoInfo.url)
            }
        }
    }
    
    /**
     * 직접 다운로드
     */
    private fun downloadDirectVideo(videoInfo: VideoInfo) {
        try {
            // 권한 확인
            val permissions = downloadHandler.checkStoragePermissions()
            if (permissions.isNotEmpty()) {
                showDownloadResult("저장소 권한이 필요합니다")
                onRequestPermissions?.invoke()
                removeFromDownloading(videoInfo.url)
                return
            }
            
            // URL 유효성 검사
            if (!downloadHandler.isValidUrl(videoInfo.url)) {
                showDownloadResult("유효하지 않은 URL입니다")
                removeFromDownloading(videoInfo.url)
                return
            }
            
            // 파일명 생성 및 다운로드
            val filename = generateFilename(videoInfo)
            downloadHandler.downloadFile(videoInfo.url, filename)
            
            showDownloadResult("다운로드 시작!\n파일: $filename")
            
        } catch (e: Exception) {
            showDownloadResult("다운로드 실패: ${e.message}")
        } finally {
            removeFromDownloading(videoInfo.url)
        }
    }
    
    /**
     * HLS 다운로드
     */
    private suspend fun downloadHlsVideo(videoInfo: VideoInfo) {
        val filename = generateFilename(videoInfo)
        
        hlsDownloader.downloadHls(videoInfo.url, filename) { success, message ->
            if (success) {
                showDownloadResult("HLS 다운로드 완료!\n$message")
            } else {
                showDownloadResult("HLS 다운로드 실패!\n$message")
            }
            removeFromDownloading(videoInfo.url)
        }
    }
    
    /**
     * 다운로드 결과 표시
     */
    private fun showDownloadResult(message: String) {
        _downloadResult.value = message
        
        // 3초 후 메시지 자동 숨김
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            _downloadResult.value = null
        }
    }
    
    /**
     * 다운로드 중 목록에서 제거
     */
    private fun removeFromDownloading(url: String) {
        val currentDownloading = _uiState.value.downloadingUrls.toMutableSet()
        currentDownloading.remove(url)
        _uiState.value = _uiState.value.copy(downloadingUrls = currentDownloading)
    }
    
    /**
     * 파일명 생성
     */
    private fun generateFilename(videoInfo: VideoInfo): String {
        val title = videoInfo.title?.take(30)?.replace(Regex("[^a-zA-Z0-9가-힣._-]"), "_")
        val extension = when (videoInfo.type) {
            VideoType.MP4 -> ".mp4"
            VideoType.WEBM -> ".webm"
            VideoType.MKV -> ".mkv"
            VideoType.HLS -> ".mp4"
            else -> ".mp4"
        }
        
        return if (title.isNullOrBlank()) {
            "video_${System.currentTimeMillis()}$extension"
        } else {
            "${title}_${System.currentTimeMillis()}$extension"
        }
    }
    
    /**
     * 초기화
     */
    fun resetState() {
        _uiState.value = MainUiState()
        _downloadResult.value = null
    }
    
    /**
     * 섹션 확장/축소 토글
     */
    fun toggleUrlSection() {
        _uiState.value = _uiState.value.copy(
            urlSectionExpanded = !_uiState.value.urlSectionExpanded
        )
    }
    
    fun toggleVideoSection() {
        _uiState.value = _uiState.value.copy(
            videoSectionExpanded = !_uiState.value.videoSectionExpanded
        )
    }
    
    /**
     * 권한 확인
     */
    fun checkStoragePermissions(): List<String> {
        return downloadHandler.checkStoragePermissions()
    }
}

/**
 * 메인 UI 상태
 */
data class MainUiState(
    val isAnalyzing: Boolean = false,
    val hasAnalyzed: Boolean = false,
    val videoList: List<VideoInfo> = emptyList(),
    val mp4Links: List<String> = emptyList(),
    val downloadingUrls: Set<String> = emptySet(),
    val urlSectionExpanded: Boolean = true,
    val videoSectionExpanded: Boolean = true
)