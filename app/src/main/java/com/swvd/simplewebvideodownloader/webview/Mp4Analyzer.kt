package com.swvd.simplewebvideodownloader.webview

import android.util.Log
import android.webkit.WebView

/**
 * MP4 비디오 분석 클래스
 * 웹페이지에서 MP4 비디오 링크를 감지하는 기능을 담당
 */
class Mp4Analyzer {
    
    /**
     * JavaScript 코드로 페이지에서 MP4 링크 추출
     * 다양한 방법으로 MP4 URL을 찾아서 반환
     */
    private val mp4DetectionScript = """
        (function() {
            var results = [];
            var uniqueUrls = new Set();
            
            try {
                // 1. 모든 video 태그의 src 확인
                var videos = document.querySelectorAll('video');
                videos.forEach(function(v) {
                    if (v.src && v.src.includes('.mp4')) {
                        uniqueUrls.add(v.src);
                    }
                    if (v.currentSrc && v.currentSrc.includes('.mp4')) {
                        uniqueUrls.add(v.currentSrc);
                    }
                });
                
                // 2. 모든 source 태그의 src 확인
                var sources = document.querySelectorAll('source');
                sources.forEach(function(s) {
                    if (s.src && s.src.includes('.mp4')) {
                        uniqueUrls.add(s.src);
                    }
                });
                
                // 3. 모든 a 태그의 href 확인 (링크)
                var links = document.querySelectorAll('a[href]');
                links.forEach(function(a) {
                    if (a.href && a.href.includes('.mp4')) {
                        uniqueUrls.add(a.href);
                    }
                });
                
                // 4. 페이지 HTML에서 MP4 URL 정규식 검색
                var html = document.documentElement.outerHTML;
                var mp4Regex = /https?:\/\/[^\s"'<>()]+\.mp4[^\s"'<>()]*/gi;
                var matches = html.match(mp4Regex);
                if (matches) {
                    matches.forEach(function(match) {
                        // URL 정리
                        var cleanUrl = match.replace(/['"<>()]+${'$'}/, '');
                        if (cleanUrl.length > 20) { // 너무 짧은 URL 제외
                            uniqueUrls.add(cleanUrl);
                        }
                    });
                }
                
                // 5. 모든 img 태그의 data 속성 확인 (때로는 비디오 썸네일이 data 속성에 있음)
                var imgs = document.querySelectorAll('img[data-src], img[data-url]');
                imgs.forEach(function(img) {
                    var dataSrc = img.getAttribute('data-src') || img.getAttribute('data-url');
                    if (dataSrc && dataSrc.includes('.mp4')) {
                        uniqueUrls.add(dataSrc);
                    }
                });
                
                // 6. 모든 div의 data 속성 확인
                var divs = document.querySelectorAll('div[data-video], div[data-src], div[data-url]');
                divs.forEach(function(div) {
                    var dataVideo = div.getAttribute('data-video') || div.getAttribute('data-src') || div.getAttribute('data-url');
                    if (dataVideo && dataVideo.includes('.mp4')) {
                        uniqueUrls.add(dataVideo);
                    }
                });
                
                // 결과 정리
                uniqueUrls.forEach(function(url) {
                    results.push(url);
                });
                
                return JSON.stringify(results);
                
            } catch (e) {
                return JSON.stringify(['JavaScript 오류: ' + e.message]);
            }
        })();
    """.trimIndent()
    
    /**
     * WebView에서 MP4 링크 분석 실행
     * @param webView 분석할 WebView
     * @param onResult 결과를 받을 콜백 함수
     */
    fun analyzePageForMp4(
        webView: WebView?,
        onResult: (List<String>) -> Unit
    ) {
        if (webView == null) {
            Log.d("Mp4Analyzer", "WebView가 없음")
            onResult(emptyList())
            return
        }
        
        Log.d("Mp4Analyzer", "MP4 감지 시작: ${webView.url}")
        
        webView.evaluateJavascript(mp4DetectionScript) { result ->
            try {
                val cleanResult = result?.replace("\\\"", "\"")?.removeSurrounding("\"") ?: "[]"
                Log.d("Mp4Analyzer", "MP4 검색 결과: $cleanResult")

                val videoLinks = if (cleanResult.startsWith("[")) {
                    cleanResult.removeSurrounding("[", "]")
                        .split(",")
                        .map { it.trim().removeSurrounding("\"") }
                        .filter { it.isNotEmpty() && it.contains(".mp4") && !it.contains("JavaScript 오류") }
                } else {
                    emptyList()
                }

                Log.d("Mp4Analyzer", "최종 MP4 링크 ${videoLinks.size}개 발견")
                onResult(videoLinks)

            } catch (e: Exception) {
                Log.e("Mp4Analyzer", "MP4 분석 오류: ${e.message}")
                onResult(listOf("분석 오류: ${e.message}"))
            }
        }
    }
} 