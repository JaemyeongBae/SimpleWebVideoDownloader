package com.swvd.simplewebvideodownloader.download

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.ContextCompat

/**
 * 파일 다운로드 처리 클래스
 * MP4 비디오 파일 다운로드 기능을 담당
 */
class DownloadHandler(private val context: Context) {
    
    /**
     * 저장소 권한 확인
     * Android 10 이하에서만 WRITE_EXTERNAL_STORAGE 권한 필요
     */
    fun checkStoragePermissions(): List<String> {
        val permissions = mutableListOf<String>()
        
        // Android 10 (API 29) 이하에서만 WRITE_EXTERNAL_STORAGE 권한 필요
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        
        return permissions
    }
    
    /**
     * 파일 다운로드 실행
     * @param url 다운로드할 파일 URL
     * @param filename 저장할 파일명
     */
    fun downloadFile(url: String, filename: String) {
        try {
            // 파일명에서 특수문자 제거
            val safeFilename = filename.replace(Regex("[^a-zA-Z0-9._-]"), "_")

            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("비디오 다운로드")
                .setDescription("$safeFilename 다운로드 중...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            // Android 10 이상에서는 Downloads 폴더에 저장
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, safeFilename)
            } else {
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, safeFilename)
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)

            Toast.makeText(context, "다운로드가 시작되었습니다\n파일: $safeFilename\nDownloads 폴더에 저장됩니다", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Toast.makeText(context, "다운로드 실패: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * URL 유효성 검사
     * @param url 검사할 URL
     * @return 유효한 URL인지 여부
     */
    fun isValidUrl(url: String): Boolean {
        val cleanUrl = url.trim()
        return cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")
    }
    
    /**
     * 파일명 생성
     * URL에서 적절한 파일명을 생성
     */
    fun generateFilename(url: String): String {
        return try {
            val uri = Uri.parse(url.trim())
            val pathSegment = uri.lastPathSegment
            when {
                pathSegment != null && pathSegment.contains(".mp4") -> pathSegment
                pathSegment != null -> "${pathSegment}.mp4"
                else -> "video_${System.currentTimeMillis()}.mp4"
            }
        } catch (e: Exception) {
            "video_${System.currentTimeMillis()}.mp4"
        }
    }
} 