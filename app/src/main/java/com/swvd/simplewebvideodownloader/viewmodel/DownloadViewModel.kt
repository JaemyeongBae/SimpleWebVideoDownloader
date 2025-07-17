package com.swvd.simplewebvideodownloader.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swvd.simplewebvideodownloader.download.VideoDownloadManager
import com.swvd.simplewebvideodownloader.models.VideoDownloadProgress
import com.swvd.simplewebvideodownloader.models.VideoDownloadStatus
import com.swvd.simplewebvideodownloader.models.VideoInfo
import com.swvd.simplewebvideodownloader.models.VideoType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 다운로드 관리 뷰모델
 * 비디오 다운로드 작업 및 상태를 관리
 */
class DownloadViewModel(context: Context) : ViewModel() {
    
    companion object {
        private const val TAG = "DownloadViewModel"
    }
    
    // 다운로드 관리자
    private val downloadManager = VideoDownloadManager(context)
    
    // 다운로드 진행 상황
    private val _downloadProgress = MutableStateFlow<Map<String, VideoDownloadProgress>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, VideoDownloadProgress>> = _downloadProgress.asStateFlow()
    
    // 다운로드 중인 URL 목록
    private val _downloadingUrls = MutableStateFlow<Set<String>>(emptySet())
    val downloadingUrls: StateFlow<Set<String>> = _downloadingUrls.asStateFlow()
    
    // 다운로드 결과 메시지
    private val _downloadResult = MutableStateFlow<String?>(null)
    val downloadResult: StateFlow<String?> = _downloadResult.asStateFlow()
    
    // 다운로드 결과 표시 상태
    private val _showDownloadResult = MutableStateFlow(false)
    val showDownloadResult: StateFlow<Boolean> = _showDownloadResult.asStateFlow()
    
    // 다운로드 통계
    private val _downloadStats = MutableStateFlow(DownloadStats())
    val downloadStats: StateFlow<DownloadStats> = _downloadStats.asStateFlow()
    
    init {
        // 다운로드 매니저의 진행 상황을 구독
        viewModelScope.launch {
            downloadManager.downloadProgress.collect { progressMap ->
                _downloadProgress.value = progressMap
                updateDownloadingUrls(progressMap)
                updateDownloadStats(progressMap)
            }
        }
    }
    
    /**
     * 비디오 다운로드 시작
     */
    fun downloadVideo(video: VideoInfo) {
        viewModelScope.launch {
            try {
                // 이미 다운로드 중인지 확인
                if (_downloadingUrls.value.contains(video.url)) {
                    showDownloadResult("이미 다운로드 중인 비디오입니다")
                    return@launch
                }
                
                // 다운로드 시작
                downloadManager.startDownload(video) { success, message ->
                    handleDownloadResult(success, message, video.url)
                }
                
            } catch (e: Exception) {
                showDownloadResult("다운로드 오류: ${e.message}")
            }
        }
    }
    
    /**
     * 다운로드 결과 처리
     */
    private fun handleDownloadResult(success: Boolean, message: String?, url: String) {
        val resultMessage = if (success) {
            "다운로드 완료!\n$message"
        } else {
            "다운로드 실패!\n$message"
        }
        
        showDownloadResult(resultMessage)
        
        // 통계 업데이트
        viewModelScope.launch {
            val currentStats = _downloadStats.value
            if (success) {
                _downloadStats.value = currentStats.copy(
                    completedCount = currentStats.completedCount + 1
                )
            } else {
                _downloadStats.value = currentStats.copy(
                    failedCount = currentStats.failedCount + 1
                )
            }
        }
    }
    
    /**
     * 다운로드 결과 메시지 표시
     */
    fun showDownloadResult(message: String) {
        _downloadResult.value = message
        _showDownloadResult.value = true
        
        // 3초 후 자동 숨김
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            hideDownloadResult()
        }
    }
    
    /**
     * 다운로드 결과 메시지 숨김
     */
    fun hideDownloadResult() {
        _downloadResult.value = null
        _showDownloadResult.value = false
    }
    
    /**
     * 다운로드 중인 URL 목록 업데이트
     */
    private fun updateDownloadingUrls(progressMap: Map<String, VideoDownloadProgress>) {
        val downloadingUrls = progressMap.filter { (_, progress) ->
            progress.status == VideoDownloadStatus.DOWNLOADING ||
            progress.status == VideoDownloadStatus.PENDING
        }.keys.toSet()
        
        _downloadingUrls.value = downloadingUrls
    }
    
    /**
     * 다운로드 통계 업데이트
     */
    private fun updateDownloadStats(progressMap: Map<String, VideoDownloadProgress>) {
        val stats = DownloadStats(
            totalCount = progressMap.size,
            downloadingCount = progressMap.count { it.value.status == VideoDownloadStatus.DOWNLOADING },
            pendingCount = progressMap.count { it.value.status == VideoDownloadStatus.PENDING },
            completedCount = progressMap.count { it.value.status == VideoDownloadStatus.COMPLETED },
            failedCount = progressMap.count { it.value.status == VideoDownloadStatus.FAILED },
            pausedCount = progressMap.count { it.value.status == VideoDownloadStatus.PAUSED }
        )
        
        _downloadStats.value = stats
    }
    
    /**
     * 다운로드 취소
     */
    fun cancelDownload(url: String) {
        downloadManager.cancelDownload(url)
        showDownloadResult("다운로드가 취소되었습니다")
    }
    
    /**
     * 모든 다운로드 취소
     */
    fun cancelAllDownloads() {
        downloadManager.cancelAllDownloads()
        showDownloadResult("모든 다운로드가 취소되었습니다")
    }
    
    /**
     * 다운로드 일시정지 (미래 구현)
     */
    fun pauseDownload(url: String) {
        // 추후 구현 예정
        showDownloadResult("일시정지 기능은 추후 구현 예정입니다")
    }
    
    /**
     * 다운로드 재개 (미래 구현)
     */
    fun resumeDownload(url: String) {
        // 추후 구현 예정
        showDownloadResult("재개 기능은 추후 구현 예정입니다")
    }
    
    /**
     * 특정 다운로드 진행 상황 조회
     */
    fun getDownloadProgress(url: String): VideoDownloadProgress? {
        return _downloadProgress.value[url]
    }
    
    /**
     * 다운로드 중인지 확인
     */
    fun isDownloading(url: String): Boolean {
        return _downloadingUrls.value.contains(url)
    }
    
    /**
     * 활성 다운로드 개수 조회
     */
    fun getActiveDownloadCount(): Int {
        return downloadManager.getActiveDownloadCount()
    }
    
    /**
     * 다운로드 가능 여부 확인
     */
    fun canDownload(video: VideoInfo): Boolean {
        return !isDownloading(video.url) && isValidVideoType(video.type)
    }
    
    /**
     * 유효한 비디오 타입인지 확인
     */
    private fun isValidVideoType(type: VideoType): Boolean {
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
     * 다운로드 히스토리 초기화
     */
    fun clearDownloadHistory() {
        _downloadProgress.value = emptyMap()
        _downloadingUrls.value = emptySet()
        _downloadStats.value = DownloadStats()
        showDownloadResult("다운로드 히스토리가 초기화되었습니다")
    }
    
    /**
     * 완료된 다운로드 제거
     */
    fun clearCompletedDownloads() {
        val currentProgress = _downloadProgress.value.toMutableMap()
        val completedUrls = currentProgress.filter { 
            it.value.status == VideoDownloadStatus.COMPLETED 
        }.keys
        
        completedUrls.forEach { url ->
            currentProgress.remove(url)
        }
        
        _downloadProgress.value = currentProgress
        showDownloadResult("완료된 다운로드 ${completedUrls.size}개가 제거되었습니다")
    }
    
    /**
     * 실패한 다운로드 재시도
     */
    fun retryFailedDownloads() {
        val failedDownloads = _downloadProgress.value.filter { 
            it.value.status == VideoDownloadStatus.FAILED 
        }
        
        if (failedDownloads.isEmpty()) {
            showDownloadResult("재시도할 실패한 다운로드가 없습니다")
            return
        }
        
        failedDownloads.forEach { (_, progress) ->
            downloadVideo(progress.videoInfo)
        }
        
        showDownloadResult("${failedDownloads.size}개의 실패한 다운로드를 재시도합니다")
    }
    
    /**
     * 다운로드 우선순위 설정 (미래 구현)
     */
    fun setDownloadPriority(url: String, priority: Int) {
        // 추후 구현 예정
    }
    
    /**
     * 동시 다운로드 개수 제한 설정 (미래 구현)
     */
    fun setMaxConcurrentDownloads(count: Int) {
        // 추후 구현 예정
    }
}

/**
 * 다운로드 통계 데이터 클래스
 */
data class DownloadStats(
    val totalCount: Int = 0,
    val downloadingCount: Int = 0,
    val pendingCount: Int = 0,
    val completedCount: Int = 0,
    val failedCount: Int = 0,
    val pausedCount: Int = 0
) {
    val activeCount: Int
        get() = downloadingCount + pendingCount
    
    val completionRate: Float
        get() = if (totalCount > 0) (completedCount.toFloat() / totalCount) * 100 else 0f
    
    val failureRate: Float
        get() = if (totalCount > 0) (failedCount.toFloat() / totalCount) * 100 else 0f
}