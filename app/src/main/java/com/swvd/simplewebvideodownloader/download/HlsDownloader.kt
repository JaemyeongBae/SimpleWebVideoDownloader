package com.swvd.simplewebvideodownloader.download

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * HLS (M3U8) 다운로드 핸들러
 * HLS 스트리밍 비디오를 다운로드하는 기능을 담당
 */
class HlsDownloader(private val context: Context) {
    
    companion object {
        private const val TAG = "HlsDownloader"
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36"
        private const val CONNECT_TIMEOUT = 10000
        private const val READ_TIMEOUT = 30000
    }
    
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
     * HLS 스트림 다운로드 시작
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
                
                // 1. M3U8 매니페스트 다운로드
                val playlistContent = downloadPlaylist(m3u8Url)
                if (playlistContent.isNullOrEmpty()) {
                    updateProgress(m3u8Url, DownloadStatus.FAILED, error = "매니페스트 다운로드 실패")
                    onComplete(false, "매니페스트 다운로드 실패")
                    return@withContext
                }
                
                // 2. 세그먼트 URL 추출
                val segmentUrls = parsePlaylist(playlistContent, m3u8Url)
                if (segmentUrls.isEmpty()) {
                    updateProgress(m3u8Url, DownloadStatus.FAILED, error = "세그먼트 URL 추출 실패")
                    onComplete(false, "세그먼트 URL 추출 실패")
                    return@withContext
                }
                
                Log.d(TAG, "총 ${segmentUrls.size}개 세그먼트 발견")
                updateProgress(m3u8Url, DownloadStatus.DOWNLOADING, totalSegments = segmentUrls.size)
                
                // 3. 출력 파일 준비
                val outputFile = prepareOutputFile(filename)
                if (outputFile == null) {
                    updateProgress(m3u8Url, DownloadStatus.FAILED, error = "출력 파일 생성 실패")
                    onComplete(false, "출력 파일 생성 실패")
                    return@withContext
                }
                
                // 4. 세그먼트 다운로드 및 병합
                val success = downloadAndMergeSegments(m3u8Url, segmentUrls, outputFile)
                
                if (success) {
                    updateProgress(m3u8Url, DownloadStatus.COMPLETED, progress = 100)
                    onComplete(true, "다운로드 완료: ${outputFile.name}")
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
     * M3U8 매니페스트 다운로드
     */
    private suspend fun downloadPlaylist(url: String): String? {
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
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                Log.e(TAG, "매니페스트 다운로드 실패: HTTP $responseCode")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "매니페스트 다운로드 오류: ${e.message}")
            null
        }
    }
    
    /**
     * M3U8 플레이리스트 파싱하여 세그먼트 URL 추출
     */
    private fun parsePlaylist(content: String, baseUrl: String): List<String> {
        val segmentUrls = mutableListOf<String>()
        val baseUri = baseUrl.substringBeforeLast("/")
        
        content.lines().forEach { line ->
            val trimmedLine = line.trim()
            
            // 세그먼트 파일인지 확인 (일반적으로 .ts 파일)
            if (trimmedLine.isNotEmpty() && 
                !trimmedLine.startsWith("#") && 
                (trimmedLine.endsWith(".ts") || 
                 trimmedLine.endsWith(".m4s") || 
                 trimmedLine.contains(".ts?") ||
                 trimmedLine.contains(".m4s?"))) {
                
                val segmentUrl = if (trimmedLine.startsWith("http")) {
                    trimmedLine
                } else if (trimmedLine.startsWith("/")) {
                    // 절대 경로
                    val domain = baseUrl.substringBefore("/", baseUrl.substringAfter("://"))
                    val protocol = baseUrl.substringBefore("://")
                    "$protocol://$domain$trimmedLine"
                } else {
                    // 상대 경로
                    "$baseUri/$trimmedLine"
                }
                
                segmentUrls.add(segmentUrl)
            }
        }
        
        return segmentUrls
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
     * 세그먼트 다운로드 및 병합
     */
    private suspend fun downloadAndMergeSegments(
        m3u8Url: String,
        segmentUrls: List<String>,
        outputFile: File
    ): Boolean {
        return try {
            FileOutputStream(outputFile).use { outputStream ->
                segmentUrls.forEachIndexed { index, segmentUrl ->
                    try {
                        // 세그먼트 다운로드
                        val segmentData = downloadSegment(segmentUrl)
                        if (segmentData != null) {
                            outputStream.write(segmentData)
                            outputStream.flush()
                            
                            // 진행률 업데이트
                            val progress = ((index + 1) * 100) / segmentUrls.size
                            updateProgress(
                                m3u8Url, 
                                DownloadStatus.DOWNLOADING, 
                                progress = progress,
                                downloadedSegments = index + 1
                            )
                            
                            Log.d(TAG, "세그먼트 ${index + 1}/${segmentUrls.size} 다운로드 완료 ($progress%)")
                        } else {
                            Log.e(TAG, "세그먼트 다운로드 실패: $segmentUrl")
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