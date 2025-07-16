package com.swvd.simplewebvideodownloader.hls

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.net.URLConnection

/**
 * M3U8 플레이리스트 파서
 * 마스터 플레이리스트와 미디어 플레이리스트를 파싱하여 
 * HLS 스트림의 구조를 분석하는 클래스
 * 
 * 주요 기능:
 * - 마스터 플레이리스트 파싱 (다양한 화질 옵션)
 * - 미디어 플레이리스트 파싱 (세그먼트 목록)
 * - URL 해석 및 상대 경로 처리
 * - 네트워크 오류 처리
 */
class M3u8Parser {
    
    companion object {
        private const val TAG = "M3u8Parser"
        private const val CONNECTION_TIMEOUT = 10000 // 10초
        private const val READ_TIMEOUT = 15000 // 15초
    }
    
    /**
     * M3U8 플레이리스트 파싱
     * @param url M3U8 플레이리스트 URL
     * @return 파싱된 플레이리스트 정보
     */
    suspend fun parseM3u8(url: String): M3u8Playlist = withContext(Dispatchers.IO) {
        Log.d(TAG, "M3U8 파싱 시작: $url")
        
        try {
            val content = downloadM3u8Content(url)
            val lines = content.lines().filter { it.isNotBlank() }
            
            Log.d(TAG, "M3U8 파일 크기: ${content.length} bytes, 라인 수: ${lines.size}")
            
            return@withContext if (isMasterPlaylist(lines)) {
                Log.i(TAG, "마스터 플레이리스트 감지")
                parseMasterPlaylist(url, lines)
            } else {
                Log.i(TAG, "미디어 플레이리스트 감지")
                parseMediaPlaylist(url, lines)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "M3U8 파싱 실패: ${e.message}")
            throw M3u8ParseException("M3U8 파싱 실패: ${e.message}", e)
        }
    }
    
    /**
     * M3U8 콘텐츠 다운로드
     * @param url M3U8 URL
     * @return 다운로드된 텍스트 콘텐츠
     */
    private fun downloadM3u8Content(url: String): String {
        return try {
            val connection = URL(url).openConnection() as URLConnection
            connection.connectTimeout = CONNECTION_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            
            // HLS 요청에 적합한 헤더 설정
            connection.setRequestProperty("User-Agent", 
                "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
            connection.setRequestProperty("Accept", 
                "application/x-mpegURL, application/vnd.apple.mpegurl, */*")
            
            connection.getInputStream().use { inputStream ->
                inputStream.bufferedReader().use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "M3U8 콘텐츠 다운로드 실패: $url - ${e.message}")
            throw e
        }
    }
    
    /**
     * 마스터 플레이리스트 여부 확인
     * @param lines M3U8 파일의 모든 라인
     * @return 마스터 플레이리스트 여부
     */
    private fun isMasterPlaylist(lines: List<String>): Boolean {
        return lines.any { line ->
            line.startsWith("#EXT-X-STREAM-INF") || 
            line.startsWith("#EXT-X-I-FRAME-STREAM-INF") ||
            line.startsWith("#EXT-X-MEDIA")
        }
    }
    
    /**
     * 마스터 플레이리스트 파싱
     * @param baseUrl 기본 URL
     * @param lines M3U8 파일 라인들
     * @return 파싱된 마스터 플레이리스트
     */
    private fun parseMasterPlaylist(baseUrl: String, lines: List<String>): M3u8Playlist {
        val variants = mutableListOf<HlsVariant>()
        
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            
            if (line.startsWith("#EXT-X-STREAM-INF")) {
                val attributes = parseAttributes(line)
                val bandwidth = attributes["BANDWIDTH"]?.toIntOrNull() ?: 0
                val resolution = attributes["RESOLUTION"] ?: "unknown"
                val codecs = attributes["CODECS"]
                
                // 다음 라인이 URL
                if (i + 1 < lines.size && !lines[i + 1].startsWith("#")) {
                    val variantUrl = resolveUrl(baseUrl, lines[i + 1])
                    variants.add(HlsVariant(variantUrl, bandwidth, resolution, codecs))
                    Log.d(TAG, "변형 추가: $resolution ($bandwidth bps) - $variantUrl")
                }
            }
            i++
        }
        
        // 대역폭 기준으로 정렬 (높은 화질부터)
        variants.sortByDescending { it.bandwidth }
        
        Log.i(TAG, "마스터 플레이리스트 파싱 완료: ${variants.size}개 변형")
        
        return M3u8Playlist(
            url = baseUrl,
            isMaster = true,
            variants = variants,
            totalDuration = null
        )
    }
    
    /**
     * 미디어 플레이리스트 파싱
     * @param baseUrl 기본 URL
     * @param lines M3U8 파일 라인들
     * @return 파싱된 미디어 플레이리스트
     */
    private fun parseMediaPlaylist(baseUrl: String, lines: List<String>): M3u8Playlist {
        val segments = mutableListOf<HlsSegment>()
        var duration = 0.0
        var sequence = 0
        var totalDuration = 0.0
        
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            
            when {
                line.startsWith("#EXTINF:") -> {
                    // 세그먼트 길이 정보
                    val durationStr = line.substringAfter(":").substringBefore(",")
                    duration = durationStr.toDoubleOrNull() ?: 0.0
                    totalDuration += duration
                }
                
                line.startsWith("#EXT-X-MEDIA-SEQUENCE:") -> {
                    // 시작 시퀀스 번호
                    sequence = line.substringAfter(":").toIntOrNull() ?: 0
                }
                
                line.endsWith(".ts") || line.endsWith(".m4s") -> {
                    // 세그먼트 파일
                    val segmentUrl = resolveUrl(baseUrl, line)
                    segments.add(HlsSegment(segmentUrl, duration.toFloat(), sequence++))
                    Log.v(TAG, "세그먼트 추가: $segmentUrl (${duration}초)")
                }
            }
            i++
        }
        
        Log.i(TAG, "미디어 플레이리스트 파싱 완료: ${segments.size}개 세그먼트, 총 ${totalDuration.toInt()}초")
        
        return M3u8Playlist(
            url = baseUrl,
            isMaster = false,
            segments = segments,
            totalDuration = totalDuration.toLong()
        )
    }
    
    /**
     * 속성 문자열 파싱
     * @param line 속성이 포함된 라인
     * @return 파싱된 속성 맵
     */
    private fun parseAttributes(line: String): Map<String, String> {
        val attributes = mutableMapOf<String, String>()
        
        try {
            // #EXT-X-STREAM-INF: 이후의 속성들 추출
            val attributeString = line.substringAfter(":")
            
            // 정규식으로 KEY=VALUE 패턴 찾기 (따옴표 고려)
            val regex = """(\w+)=(\"([^\"]*)\"|([^,]*))""".toRegex()
            
            regex.findAll(attributeString).forEach { match ->
                val key = match.groupValues[1]
                val value = match.groupValues[3].ifEmpty { match.groupValues[4] }
                attributes[key] = value
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "속성 파싱 오류: $line - ${e.message}")
        }
        
        return attributes
    }
    
    /**
     * 상대 URL을 절대 URL로 변환
     * @param baseUrl 기본 URL
     * @param path 상대 또는 절대 경로
     * @return 해석된 절대 URL
     */
    private fun resolveUrl(baseUrl: String, path: String): String {
        return try {
            when {
                // 이미 절대 URL인 경우
                path.startsWith("http://") || path.startsWith("https://") -> path
                
                // 루트 상대 경로인 경우 (/path)
                path.startsWith("/") -> {
                    val baseUrlObj = URL(baseUrl)
                    "${baseUrlObj.protocol}://${baseUrlObj.host}${if (baseUrlObj.port != -1) ":${baseUrlObj.port}" else ""}$path"
                }
                
                // 상대 경로인 경우 (path)
                else -> {
                    val baseUrlWithoutFile = baseUrl.substringBeforeLast("/")
                    "$baseUrlWithoutFile/$path"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "URL 해석 실패: $baseUrl + $path - ${e.message}")
            path // 실패시 원본 반환
        }
    }
}

/**
 * M3U8 플레이리스트 데이터
 * @param url 플레이리스트 URL
 * @param isMaster 마스터 플레이리스트 여부
 * @param variants 다양한 화질 옵션 (마스터 플레이리스트용)
 * @param segments 세그먼트 목록 (미디어 플레이리스트용)
 * @param totalDuration 총 재생 시간 (초)
 */
data class M3u8Playlist(
    val url: String,
    val isMaster: Boolean,
    val variants: List<HlsVariant> = emptyList(),
    val segments: List<HlsSegment> = emptyList(),
    val totalDuration: Long? = null
) {
    /**
     * 가장 높은 화질의 변형 URL 반환
     */
    val bestQualityUrl: String?
        get() = variants.maxByOrNull { it.bandwidth }?.url
    
    /**
     * 사용자 친화적인 설명 반환
     */
    val description: String
        get() = when {
            isMaster && variants.isNotEmpty() -> 
                "${variants.size}개 화질 (최고: ${variants.maxByOrNull { it.bandwidth }?.resolution})"
            !isMaster && segments.isNotEmpty() -> 
                "${segments.size}개 세그먼트" + if (totalDuration != null) " (${totalDuration}초)" else ""
            else -> "빈 플레이리스트"
        }
}

/**
 * HLS 세그먼트
 * @param url 세그먼트 파일 URL
 * @param duration 세그먼트 재생 시간 (초)
 * @param sequence 시퀀스 번호
 */
data class HlsSegment(
    val url: String,
    val duration: Float,
    val sequence: Int
)

/**
 * M3U8 파싱 예외
 */
class M3u8ParseException(message: String, cause: Throwable? = null) : Exception(message, cause)