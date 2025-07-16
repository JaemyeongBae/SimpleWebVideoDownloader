package com.swvd.simplewebvideodownloader.models

/**
 * 비디오 정보 데이터 클래스
 * 감지된 비디오의 상세 정보를 저장
 */
data class VideoInfo(
    val url: String,
    val type: VideoType,
    val title: String? = null,
    val duration: String? = null,
    val quality: String? = null,
    val size: String? = null,
    val thumbnail: String? = null
)

/**
 * 지원하는 비디오 형식 타입
 */
enum class VideoType(
    val displayName: String, 
    val extensions: List<String>,
    val icon: String,
    val downloadable: Boolean = true
) {
    MP4("MP4 비디오", listOf(".mp4"), "🎬", true),
    HLS("HLS 스트리밍", listOf(".m3u8", ".m3u"), "📺", true),
    DASH("DASH 스트리밍", listOf(".mpd"), "🎞️", false),
    WEBM("WebM 비디오", listOf(".webm"), "🎬", true),
    MKV("MKV 비디오", listOf(".mkv"), "🎬", true),
    AVI("AVI 비디오", listOf(".avi"), "🎬", true),
    MOV("QuickTime 비디오", listOf(".mov"), "🎬", true),
    FLV("Flash 비디오", listOf(".flv"), "🎬", true),
    YOUTUBE("YouTube 비디오", listOf("youtube.com"), "🔴", false),
    VIMEO("Vimeo 비디오", listOf("vimeo.com"), "🔵", false),
    UNKNOWN("알 수 없는 형식", emptyList(), "❓", false)
}

/**
 * 비디오 다운로드 상태
 */
enum class VideoDownloadStatus {
    PENDING,     // 대기 중
    DOWNLOADING, // 다운로드 중
    COMPLETED,   // 완료
    FAILED,      // 실패
    PAUSED       // 일시정지
}

/**
 * 비디오 다운로드 진행 정보
 */
data class VideoDownloadProgress(
    val videoInfo: VideoInfo,
    val status: VideoDownloadStatus,
    val progress: Int = 0,
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val downloadSpeed: String = "",
    val remainingTime: String = "",
    val error: String? = null
)