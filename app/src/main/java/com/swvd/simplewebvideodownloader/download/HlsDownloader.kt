package com.swvd.simplewebvideodownloader.download

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.swvd.simplewebvideodownloader.hls.M3u8Parser
import com.swvd.simplewebvideodownloader.hls.M3u8Playlist
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * HLS (M3U8) 다운로드 핸들러
 * HLS 스트리밍 비디오를 다운로드하는 기능을 담당
 * 
 * MVP 버전: .ts 세그먼트들을 단순 바이너리 연결로 병합
 * Phase 3: FFmpeg를 사용한 완전한 MP4 변환 예정
 */
class HlsDownloader(private val context: Context) {
    
    companion object {
        private const val TAG = "HlsDownloader"
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36"
        private const val CONNECT_TIMEOUT = 10000
        private const val READ_TIMEOUT = 30000
    }
    
    // M3U8 파서 인스턴스
    private val m3u8Parser = M3u8Parser()
    
    /**
     * HLS 다운로드 상태
     */
    enum class DownloadStatus {
        PENDING,
        DOWNLOADING,
        COMPLETED,
        FAILED,
        PAUSED
    }
    
    /**
     * HLS 다운로드 진행 정보
     */
    data class DownloadProgress(
        val url: String,
        val status: DownloadStatus,
        val progress: Int = 0,
        val totalSegments: Int = 0,
        val downloadedSegments: Int = 0,
        val error: String? = null
    )
    
    private val downloadProgressMap = ConcurrentHashMap<String, DownloadProgress>()
    private var progressCallback: ((DownloadProgress) -> Unit)? = null
    
    /**
     * HLS 다운로드 진행 상황 콜백 설정
     */
    fun setProgressCallback(callback: (DownloadProgress) -> Unit) {
        progressCallback = callback
    }
    
    /**
     * HLS 스트림 다운로드 시작 (최고 화질 자동 선택)
     * @param m3u8Url HLS 매니페스트 URL
     * @param filename 저장할 파일명
     * @param onComplete 완료 콜백
     */
    suspend fun downloadHls(
        m3u8Url: String,
        filename: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "HLS 다운로드 시작: $m3u8Url")
                
                // 다운로드 상태 초기화
                updateProgress(m3u8Url, DownloadStatus.DOWNLOADING)
                
                // 1. M3U8 플레이리스트 파싱 (M3u8Parser 사용)
                val playlist = m3u8Parser.parseM3u8(m3u8Url)
                Log.i(TAG, "플레이리스트 파싱 완료: ${playlist.description}")
                
                // 2. 최고 화질 URL 결정
                val targetUrl = if (playlist.isMaster) {
                    // 마스터 플레이리스트인 경우 최고 화질 URL 선택
                    val bestQualityUrl = playlist.bestQualityUrl
                    if (bestQualityUrl != null) {
                        Log.i(TAG, "최고 화질 선택: ${playlist.variants.maxByOrNull { it.bandwidth }?.resolution} - $bestQualityUrl")
                        bestQualityUrl
                    } else {
                        updateProgress(m3u8Url, DownloadStatus.FAILED, error = "화질 옵션을 찾을 수 없음")
                        onComplete(false, "화질 옵션을 찾을 수 없음")
                        return@withContext
                    }
                } else {
                    // 미디어 플레이리스트인 경우 원본 URL 사용
                    m3u8Url
                }
                
                // 3. 실제 세그먼트 다운로드 수행
                val finalPlaylist = if (playlist.isMaster) {
                    // 최고 화질의 미디어 플레이리스트 다시 파싱
                    m3u8Parser.parseM3u8(targetUrl)
                } else {
                    playlist
                }
                
                if (finalPlaylist.segments.isEmpty()) {
                    updateProgress(m3u8Url, DownloadStatus.FAILED, error = "세그먼트를 찾을 수 없음")
                    onComplete(false, "세그먼트를 찾을 수 없음")
                    return@withContext
                }
                
                Log.d(TAG, "총 ${finalPlaylist.segments.size}개 세그먼트 발견")
                updateProgress(m3u8Url, DownloadStatus.DOWNLOADING, totalSegments = finalPlaylist.segments.size)
                
                // 4. 출력 파일 준비
                val outputFile = prepareOutputFile(filename)
                if (outputFile == null) {
                    updateProgress(m3u8Url, DownloadStatus.FAILED, error = "출력 파일 생성 실패")
                    onComplete(false, "출력 파일 생성 실패")
                    return@withContext
                }
                
                // 5. 세그먼트 다운로드 및 병합
                val success = downloadAndMergeSegments(m3u8Url, finalPlaylist, outputFile)
                
                if (success) {
                    updateProgress(m3u8Url, DownloadStatus.COMPLETED, progress = 100)
                    val qualityInfo = if (playlist.isMaster) {
                        val bestVariant = playlist.variants.maxByOrNull { it.bandwidth }
                        " (${bestVariant?.resolution ?: "최고화질"})"
                    } else ""
                    onComplete(true, "다운로드 완료: ${outputFile.name}$qualityInfo")
                } else {
                    updateProgress(m3u8Url, DownloadStatus.FAILED, error = "세그먼트 다운로드 실패")
                    onComplete(false, "세그먼트 다운로드 실패")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "HLS 다운로드 오류: ${e.message}", e)
                updateProgress(m3u8Url, DownloadStatus.FAILED, error = e.message)
                onComplete(false, "다운로드 오류: ${e.message}")
            }
        }
    }
    
    
    
    /**
     * 출력 파일 준비
     */
    private fun prepareOutputFile(filename: String): File? {
        return try {
            val downloadsDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "SWVD"
            )
            
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            
            val safeFilename = filename.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val outputFile = File(downloadsDir, safeFilename)
            
            // 기존 파일이 있다면 삭제
            if (outputFile.exists()) {
                outputFile.delete()
            }
            
            outputFile.createNewFile()
            outputFile
        } catch (e: Exception) {
            Log.e(TAG, "출력 파일 준비 오류: ${e.message}")
            null
        }
    }
    
    /**
     * 세그먼트 다운로드 및 병합 (M3u8Playlist 사용)
     */
    private suspend fun downloadAndMergeSegments(
        m3u8Url: String,
        playlist: M3u8Playlist,
        outputFile: File
    ): Boolean {
        return try {
            val segments = playlist.segments
            
            FileOutputStream(outputFile).use { outputStream ->
                segments.forEachIndexed { index, segment ->
                    try {
                        // 세그먼트 다운로드
                        val segmentData = downloadSegment(segment.url)
                        if (segmentData != null) {
                            outputStream.write(segmentData)
                            outputStream.flush()
                            
                            // 진행률 업데이트
                            val progress = ((index + 1) * 100) / segments.size
                            updateProgress(
                                m3u8Url, 
                                DownloadStatus.DOWNLOADING, 
                                progress = progress,
                                downloadedSegments = index + 1
                            )
                            
                            Log.d(TAG, "세그먼트 ${index + 1}/${segments.size} 다운로드 완료 ($progress%) - ${segment.duration}초")
                        } else {
                            Log.e(TAG, "세그먼트 다운로드 실패: ${segment.url}")
                            return@use false
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "세그먼트 처리 오류: ${e.message}")
                        return@use false
                    }
                }
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "세그먼트 병합 오류: ${e.message}")
            false
        }
    }
    
    /**
     * 개별 세그먼트 다운로드
     */
    private fun downloadSegment(url: String): ByteArray? {
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
            }
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.readBytes()
            } else {
                Log.e(TAG, "세그먼트 다운로드 실패: HTTP $responseCode for $url")
                null
            }
        } catch (e: IOException) {
            Log.e(TAG, "세그먼트 다운로드 IO 오류: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "세그먼트 다운로드 오류: ${e.message}")
            null
        }
    }
    
    /**
     * 다운로드 진행 상황 업데이트
     */
    private fun updateProgress(
        url: String,
        status: DownloadStatus,
        progress: Int = 0,
        totalSegments: Int = 0,
        downloadedSegments: Int = 0,
        error: String? = null
    ) {
        val currentProgress = downloadProgressMap[url] ?: DownloadProgress(url, status)
        
        val updatedProgress = currentProgress.copy(
            status = status,
            progress = progress,
            totalSegments = if (totalSegments > 0) totalSegments else currentProgress.totalSegments,
            downloadedSegments = downloadedSegments,
            error = error
        )
        
        downloadProgressMap[url] = updatedProgress
        progressCallback?.invoke(updatedProgress)
    }
    
    /**
     * 다운로드 진행 상황 조회
     */
    fun getDownloadProgress(url: String): DownloadProgress? {
        return downloadProgressMap[url]
    }
    
    /**
     * 모든 다운로드 진행 상황 조회
     */
    fun getAllDownloadProgress(): Map<String, DownloadProgress> {
        return downloadProgressMap.toMap()
    }
    
    /**
     * 다운로드 취소
     */
    fun cancelDownload(url: String) {
        downloadProgressMap.remove(url)
        // 실제 다운로드 취소 로직은 추후 구현
    }
}