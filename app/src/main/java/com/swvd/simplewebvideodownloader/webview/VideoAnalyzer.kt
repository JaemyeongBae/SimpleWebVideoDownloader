package com.swvd.simplewebvideodownloader.webview

import android.util.Log
import android.webkit.WebView
import com.swvd.simplewebvideodownloader.models.VideoInfo
import com.swvd.simplewebvideodownloader.models.VideoType

/**
 * 비디오 분석기 클래스
 * 다양한 비디오 형식(MP4, HLS/M3U8, DASH, WebM 등)을 감지하는 기능을 담당
 */
class VideoAnalyzer {
    
    /**
     * 향상된 비디오 감지 JavaScript 코드
     * 다양한 비디오 형식과 스트리밍 URL을 감지
     */
    private val videoDetectionScript = """
        (function() {
            var results = [];
            var uniqueUrls = new Set();
            
            // 지원하는 비디오 확장자 목록
            var videoExtensions = ['.mp4', '.m3u8', '.m3u', '.mpd', '.webm', '.mkv', '.avi', '.mov', '.flv'];
            
            try {
                // 1. 모든 video 태그 분석
                var videos = document.querySelectorAll('video');
                videos.forEach(function(video) {
                    // src 속성 확인
                    if (video.src) {
                        addVideoUrl(video.src, getVideoTitle(video));
                    }
                    
                    // currentSrc 확인
                    if (video.currentSrc) {
                        addVideoUrl(video.currentSrc, getVideoTitle(video));
                    }
                    
                    // data 속성들 확인
                    ['data-src', 'data-url', 'data-video', 'data-stream'].forEach(function(attr) {
                        var value = video.getAttribute(attr);
                        if (value) {
                            addVideoUrl(value, getVideoTitle(video));
                        }
                    });
                    
                    // source 태그들 확인
                    var sources = video.querySelectorAll('source');
                    sources.forEach(function(source) {
                        if (source.src) {
                            addVideoUrl(source.src, getVideoTitle(video));
                        }
                    });
                });
                
                // 2. 모든 source 태그 분석
                var sources = document.querySelectorAll('source');
                sources.forEach(function(source) {
                    if (source.src) {
                        addVideoUrl(source.src);
                    }
                });
                
                // 3. 링크 태그 분석
                var links = document.querySelectorAll('a[href]');
                links.forEach(function(link) {
                    if (link.href) {
                        addVideoUrl(link.href, link.textContent || link.title);
                    }
                });
                
                // 4. HTML 전체에서 정규식으로 URL 검색
                var html = document.documentElement.outerHTML;
                
                // 다양한 비디오 URL 패턴 검색
                videoExtensions.forEach(function(ext) {
                    var regex = new RegExp('https?:\\/\\/[^\\s"\'<>()]+\\' + ext + '[^\\s"\'<>()]*', 'gi');
                    var matches = html.match(regex);
                    if (matches) {
                        matches.forEach(function(match) {
                            var cleanUrl = match.replace(/['"<>()]+${'$'}/, '');
                            if (cleanUrl.length > 10) {
                                addVideoUrl(cleanUrl);
                            }
                        });
                    }
                });
                
                // 5. HLS 스트리밍 특별 검색
                // HLS 매니페스트는 종종 다른 방식으로 로드됨
                var hlsPatterns = [
                    /https?:\/\/[^\s"'<>()]+\.m3u8[^\s"'<>()]*/gi,
                    /https?:\/\/[^\s"'<>()]+\/playlist\.m3u8[^\s"'<>()]*/gi,
                    /https?:\/\/[^\s"'<>()]+\/index\.m3u8[^\s"'<>()]*/gi,
                    /https?:\/\/[^\s"'<>()]+\/master\.m3u8[^\s"'<>()]*/gi
                ];
                
                hlsPatterns.forEach(function(pattern) {
                    var matches = html.match(pattern);
                    if (matches) {
                        matches.forEach(function(match) {
                            var cleanUrl = match.replace(/['"<>()]+${'$'}/, '');
                            if (cleanUrl.length > 20) {
                                addVideoUrl(cleanUrl);
                            }
                        });
                    }
                });
                
                // 6. DASH 스트리밍 검색
                var dashPatterns = [
                    /https?:\/\/[^\s"'<>()]+\.mpd[^\s"'<>()]*/gi,
                    /https?:\/\/[^\s"'<>()]+\/manifest\.mpd[^\s"'<>()]*/gi
                ];
                
                dashPatterns.forEach(function(pattern) {
                    var matches = html.match(pattern);
                    if (matches) {
                        matches.forEach(function(match) {
                            var cleanUrl = match.replace(/['"<>()]+${'$'}/, '');
                            if (cleanUrl.length > 20) {
                                addVideoUrl(cleanUrl);
                            }
                        });
                    }
                });
                
                // 7. iframe 내 비디오 URL 검색
                var iframes = document.querySelectorAll('iframe');
                iframes.forEach(function(iframe) {
                    if (iframe.src) {
                        // YouTube, Vimeo 등의 embed URL도 확인
                        if (iframe.src.includes('youtube.com') || 
                            iframe.src.includes('vimeo.com') ||
                            iframe.src.includes('dailymotion.com')) {
                            addVideoUrl(iframe.src, 'Embedded Video');
                        }
                    }
                });
                
                // 8. JavaScript 변수에서 비디오 URL 검색
                var scripts = document.querySelectorAll('script');
                scripts.forEach(function(script) {
                    if (script.textContent) {
                        var scriptText = script.textContent;
                        videoExtensions.forEach(function(ext) {
                            var regex = new RegExp('https?:\\/\\/[^\\s"\'<>()]+\\' + ext + '[^\\s"\'<>()]*', 'gi');
                            var matches = scriptText.match(regex);
                            if (matches) {
                                matches.forEach(function(match) {
                                    var cleanUrl = match.replace(/['"<>()]+${'$'}/, '');
                                    if (cleanUrl.length > 10) {
                                        addVideoUrl(cleanUrl);
                                    }
                                });
                            }
                        });
                    }
                });
                
                // URL 추가 함수
                function addVideoUrl(url, title) {
                    if (!url || url.length < 10) return;
                    
                    // URL 정리
                    var cleanUrl = url.trim();
                    if (cleanUrl.startsWith('//')) {
                        cleanUrl = 'https:' + cleanUrl;
                    }
                    
                    // 비디오 URL인지 확인
                    var isVideo = videoExtensions.some(function(ext) {
                        return cleanUrl.toLowerCase().includes(ext);
                    });
                    
                    if (isVideo && !uniqueUrls.has(cleanUrl)) {
                        uniqueUrls.add(cleanUrl);
                        
                        // 비디오 타입 결정
                        var videoType = 'UNKNOWN';
                        if (cleanUrl.includes('.mp4')) videoType = 'MP4';
                        else if (cleanUrl.includes('.m3u8') || cleanUrl.includes('.m3u')) videoType = 'HLS';
                        else if (cleanUrl.includes('.mpd')) videoType = 'DASH';
                        else if (cleanUrl.includes('.webm')) videoType = 'WEBM';
                        else if (cleanUrl.includes('.mkv')) videoType = 'MKV';
                        else if (cleanUrl.includes('.avi')) videoType = 'AVI';
                        else if (cleanUrl.includes('.mov')) videoType = 'MOV';
                        else if (cleanUrl.includes('.flv')) videoType = 'FLV';
                        else if (cleanUrl.includes('youtube.com')) videoType = 'YOUTUBE';
                        else if (cleanUrl.includes('vimeo.com')) videoType = 'VIMEO';
                        
                        results.push({
                            url: cleanUrl,
                            type: videoType,
                            title: title || null
                        });
                    }
                }
                
                // 비디오 제목 추출 함수
                function getVideoTitle(videoElement) {
                    return videoElement.title || 
                           videoElement.getAttribute('data-title') ||
                           videoElement.getAttribute('aria-label') ||
                           null;
                }
                
                return JSON.stringify(results);
                
            } catch (e) {
                return JSON.stringify([{
                    url: 'JavaScript 오류: ' + e.message,
                    type: 'UNKNOWN',
                    title: null
                }]);
            }
        })();
    """.trimIndent()
    
    /**
     * WebView에서 비디오 분석 실행
     * @param webView 분석할 WebView
     * @param onResult 결과를 받을 콜백 함수
     */
    fun analyzeVideos(
        webView: WebView?,
        onResult: (List<VideoInfo>) -> Unit
    ) {
        if (webView == null) {
            Log.d("VideoAnalyzer", "WebView가 없음")
            onResult(emptyList())
            return
        }
        
        Log.d("VideoAnalyzer", "비디오 분석 시작: ${webView.url}")
        
        webView.evaluateJavascript(videoDetectionScript) { result ->
            try {
                val cleanResult = result?.replace("\\\"", "\"")?.removeSurrounding("\"") ?: "[]"
                Log.d("VideoAnalyzer", "비디오 검색 결과: $cleanResult")

                val videos = parseVideoResults(cleanResult)
                Log.d("VideoAnalyzer", "최종 비디오 ${videos.size}개 발견")
                onResult(videos)

            } catch (e: Exception) {
                Log.e("VideoAnalyzer", "비디오 분석 오류: ${e.message}")
                onResult(listOf(VideoInfo("분석 오류: ${e.message}", VideoType.UNKNOWN)))
            }
        }
    }
    
    /**
     * JavaScript 결과를 VideoInfo 리스트로 파싱
     */
    private fun parseVideoResults(jsonResult: String): List<VideoInfo> {
        return try {
            if (!jsonResult.startsWith("[")) {
                return emptyList()
            }
            
            // 간단한 JSON 파싱 (복잡한 라이브러리 없이)
            val videos = mutableListOf<VideoInfo>()
            val jsonArray = jsonResult.removeSurrounding("[", "]")
            
            if (jsonArray.isBlank()) {
                return emptyList()
            }
            
            // 각 비디오 객체 파싱
            var currentObject = ""
            var braceCount = 0
            var inString = false
            var escapeNext = false
            
            for (char in jsonArray) {
                when {
                    escapeNext -> {
                        currentObject += char
                        escapeNext = false
                    }
                    char == '\\' -> {
                        currentObject += char
                        escapeNext = true
                    }
                    char == '"' -> {
                        inString = !inString
                        currentObject += char
                    }
                    !inString && char == '{' -> {
                        braceCount++
                        currentObject += char
                    }
                    !inString && char == '}' -> {
                        braceCount--
                        currentObject += char
                        if (braceCount == 0) {
                            // 객체 완성
                            val video = parseVideoObject(currentObject)
                            if (video != null) {
                                videos.add(video)
                            }
                            currentObject = ""
                        }
                    }
                    braceCount > 0 -> {
                        currentObject += char
                    }
                }
            }
            
            videos.filter { it.url.isNotEmpty() && !it.url.contains("JavaScript 오류") }
        } catch (e: Exception) {
            Log.e("VideoAnalyzer", "JSON 파싱 오류: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * 개별 비디오 객체 파싱
     */
    private fun parseVideoObject(jsonObject: String): VideoInfo? {
        return try {
            // 간단한 JSON 객체 파싱
            val url = extractJsonValue(jsonObject, "url")
            val typeStr = extractJsonValue(jsonObject, "type")
            val title = extractJsonValue(jsonObject, "title")
            
            if (url.isBlank()) return null
            
            val type = try {
                VideoType.valueOf(typeStr)
            } catch (e: IllegalArgumentException) {
                VideoType.UNKNOWN
            }
            
            VideoInfo(
                url = url,
                type = type,
                title = title.takeIf { it.isNotBlank() }
            )
        } catch (e: Exception) {
            Log.e("VideoAnalyzer", "비디오 객체 파싱 오류: ${e.message}")
            null
        }
    }
    
    /**
     * JSON 문자열에서 특정 키의 값 추출
     */
    private fun extractJsonValue(json: String, key: String): String {
        return try {
            val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\""
            val regex = Regex(pattern)
            val match = regex.find(json)
            match?.groupValues?.get(1)?.replace("\\\"", "\"") ?: ""
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * 비디오 타입별 다운로드 가능 여부 확인
     */
    fun isDownloadable(video: VideoInfo): Boolean {
        return when (video.type) {
            VideoType.MP4, VideoType.WEBM, VideoType.MKV, 
            VideoType.AVI, VideoType.MOV, VideoType.FLV -> true
            VideoType.HLS -> true // HLS는 별도 처리 필요
            VideoType.DASH -> false // DASH는 복잡한 처리 필요
            VideoType.YOUTUBE, VideoType.VIMEO -> false // 별도 처리 필요
            VideoType.UNKNOWN -> video.url.contains("http") && 
                                 (video.url.contains(".mp4") || video.url.contains(".webm"))
        }
    }
}