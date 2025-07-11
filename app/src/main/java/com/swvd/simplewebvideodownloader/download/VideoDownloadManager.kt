package com.swvd.simplewebvideodownloader.download

import android.content.Context
import android.util.Log
import com.swvd.simplewebvideodownloader.models.VideoInfo
import com.swvd.simplewebvideodownloader.models.VideoType
import com.swvd.simplewebvideodownloader.models.VideoDownloadProgress
import com.swvd.simplewebvideodownloader.models.VideoDownloadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * 비디오 다운로드 관리자
 * 다양한 비디오 형식의 다운로드를 통합 관리
 */
class VideoDownloadManager(private val context: Context) {
    
    companion object {
        private const val TAG = "VideoDownloadManager"
    }
    
    private val downloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // 다운로드 핸들러들
    private val downloadHandler = DownloadHandler(context)
    private val hlsDownloader = HlsDownloader(context)
    
    // 다운로드 진행 상황 관리
    private val _downloadProgress = MutableStateFlow<Map<String, VideoDownloadProgress>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, VideoDownloadProgress>> = _downloadProgress.asStateFlow()
    
    private val activeDownloads = ConcurrentHashMap<String, VideoDownloadProgress>()
    
    init {
        setupHlsDownloaderCallback()
    }
    
    /**
     * HLS 다운로더 콜백 설정
     */
    private fun setupHlsDownloaderCallback() {
        hlsDownloader.setProgressCallback { hlsProgress ->
            // HLS 진행 상황을 VideoDownloadProgress로 변환
            val videoInfo = VideoInfo(
                url = hlsProgress.url,
                type = VideoType.HLS,
                title = "HLS 스트리밍"
            )
            
            val status = when (hlsProgress.status) {
                HlsDownloader.DownloadStatus.PENDING -> VideoDownloadStatus.PENDING
                HlsDownloader.DownloadStatus.DOWNLOADING -> VideoDownloadStatus.DOWNLOADING
                HlsDownloader.DownloadStatus.COMPLETED -> VideoDownloadStatus.COMPLETED
                HlsDownloader.DownloadStatus.FAILED -> VideoDownloadStatus.FAILED
                HlsDownloader.DownloadStatus.PAUSED -> VideoDownloadStatus.PAUSED
            }
            
            val progress = VideoDownloadProgress(
                videoInfo = videoInfo,
                status = status,
                progress = hlsProgress.progress,
                error = hlsProgress.error
            )
            
            updateDownloadProgress(hlsProgress.url, progress)
        }
    }
    
    /**
     * 비디오 다운로드 시작
     */
    fun startDownload(videoInfo: VideoInfo, onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        Log.d(TAG, "다운로드 시작: ${videoInfo.type.displayName} - ${videoInfo.url}")
        
        // 이미 다운로드 중인지 확인
        if (activeDownloads.containsKey(videoInfo.url)) {
            Log.w(TAG, "이미 다운로드 중인 비디오: ${videoInfo.url}")
            return
        }
        
        // 다운로드 가능한지 확인
        if (!isDownloadable(videoInfo)) {
            Log.w(TAG, "다운로드 불가능한 비디오 형식: ${videoInfo.type}")
            onComplete(false, "지원하지 않는 비디오 형식입니다")
            return
        }
        
        // 초기 진행 상황 설정
        val initialProgress = VideoDownloadProgress(
            videoInfo = videoInfo,
            status = VideoDownloadStatus.PENDING
        )
        updateDownloadProgress(videoInfo.url, initialProgress)
        
        // 비디오 타입에 따른 다운로드 시작
        downloadScope.launch {
            try {
                when (videoInfo.type) {
                    VideoType.MP4, VideoType.WEBM, VideoType.MKV, 
                    VideoType.AVI, VideoType.MOV, VideoType.FLV -> {
                        downloadDirectVideo(videoInfo, onComplete)
                    }
                    VideoType.HLS -> {
                        downloadHlsVideo(videoInfo, onComplete)
                    }
                    VideoType.DASH -> {
                        onComplete(false, "DASH 스트리밍은 아직 지원하지 않습니다")
                    }
                    else -> {
                        onComplete(false, "지원하지 않는 비디오 형식입니다")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "다운로드 오류: ${e.message}", e)
                updateDownloadProgress(videoInfo.url, initialProgress.copy(
                    status = VideoDownloadStatus.FAILED,
                    error = e.message
                ))
                onComplete(false, "다운로드 오류: ${e.message}")
            }
        }
    }
    
    /**
     * 직접 다운로드 가능한 비디오 다운로드
     */
    private fun downloadDirectVideo(videoInfo: VideoInfo, onComplete: (Boolean, String?) -> Unit) {
        try {
            // 권한 확인
            val permissions = downloadHandler.checkStoragePermissions()
            if (permissions.isNotEmpty()) {
                onComplete(false, "저장소 권한이 필요합니다")
                return
            }
            
            // 다운로드 진행 상황 업데이트
            updateDownloadProgress(videoInfo.url, VideoDownloadProgress(
                videoInfo = videoInfo,
                status = VideoDownloadStatus.DOWNLOADING
            ))
            
            // 파일명 생성
            val filename = generateFilename(videoInfo)
            
            // 다운로드 실행
            downloadHandler.downloadFile(videoInfo.url, filename)
            
            // 다운로드 완료 처리
            updateDownloadProgress(videoInfo.url, VideoDownloadProgress(
                videoInfo = videoInfo,
                status = VideoDownloadStatus.COMPLETED,
                progress = 100
            ))
            
            onComplete(true, "다운로드 완료: $filename")
            
        } catch (e: Exception) {
            Log.e(TAG, "직접 다운로드 오류: ${e.message}", e)
            updateDownloadProgress(videoInfo.url, VideoDownloadProgress(
                videoInfo = videoInfo,
                status = VideoDownloadStatus.FAILED,
                error = e.message
            ))
            onComplete(false, "다운로드 실패: ${e.message}")
        }
    }
    
    /**
     * HLS 비디오 다운로드
     */
    private suspend fun downloadHlsVideo(videoInfo: VideoInfo, onComplete: (Boolean, String?) -> Unit) {
        try {
            val filename = generateFilename(videoInfo)
            
            hlsDownloader.downloadHls(videoInfo.url, filename) { success, message ->
                if (success) {
                    Log.d(TAG, "HLS 다운로드 완료: $message")
                } else {
                    Log.e(TAG, "HLS 다운로드 실패: $message")
                }
                onComplete(success, message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "HLS 다운로드 오류: ${e.message}", e)
            onComplete(false, "HLS 다운로드 오류: ${e.message}")
        }
    }
    
    /**
     * 다운로드 가능 여부 확인
     */
    private fun isDownloadable(videoInfo: VideoInfo): Boolean {
        return when (videoInfo.type) {
            VideoType.MP4, VideoType.WEBM, VideoType.MKV, 
            VideoType.AVI, VideoType.MOV, VideoType.FLV -> true
            VideoType.HLS -> true
            VideoType.DASH -> false // 복잡한 구현 필요
            VideoType.YOUTUBE, VideoType.VIMEO -> false // 별도 처리 필요
            VideoType.UNKNOWN -> videoInfo.url.contains("http") && 
                                 videoInfo.url.contains(".")
        }
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
            VideoType.AVI -> ".avi"
            VideoType.MOV -> ".mov"
            VideoType.FLV -> ".flv"
            VideoType.HLS -> ".mp4" // HLS는 MP4로 병합
            else -> ".mp4"
        }
        
        return if (title.isNullOrBlank()) {
            "video_${System.currentTimeMillis()}$extension"
        } else {
            "${title}_${System.currentTimeMillis()}$extension"
        }
    }
    
    /**
     * 다운로드 진행 상황 업데이트
     */
    private fun updateDownloadProgress(url: String, progress: VideoDownloadProgress) {
        activeDownloads[url] = progress
        _downloadProgress.value = activeDownloads.toMap()
        
        // 완료되거나 실패한 경우 일정 시간 후 제거
        if (progress.status == VideoDownloadStatus.COMPLETED || 
            progress.status == VideoDownloadStatus.FAILED) {
            downloadScope.launch {
                kotlinx.coroutines.delay(5000) // 5초 후 제거
                activeDownloads.remove(url)
                _downloadProgress.value = activeDownloads.toMap()
            }
        }
    }
    
    /**
     * 다운로드 취소
     */
    fun cancelDownload(url: String) {
        activeDownloads.remove(url)
        _downloadProgress.value = activeDownloads.toMap()
        
        // HLS 다운로드 취소
        hlsDownloader.cancelDownload(url)
        
        Log.d(TAG, "다운로드 취소: $url")
    }
    
    /**
     * 모든 다운로드 취소
     */
    fun cancelAllDownloads() {
        val urls = activeDownloads.keys.toList()
        urls.forEach { cancelDownload(it) }
        Log.d(TAG, "모든 다운로드 취소")
    }
    
    /**
     * 다운로드 진행 상황 조회
     */
    fun getDownloadProgress(url: String): VideoDownloadProgress? {
        return activeDownloads[url]
    }
    
    /**
     * 활성 다운로드 수 조회
     */
    fun getActiveDownloadCount(): Int {
        return activeDownloads.values.count { 
            it.status == VideoDownloadStatus.DOWNLOADING || 
            it.status == VideoDownloadStatus.PENDING 
        }
    }
}