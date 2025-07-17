package com.swvd.simplewebvideodownloader.download

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * DownloadHandler 단위 테스트
 * 다운로드 핸들러의 핵심 기능을 검증
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class DownloadHandlerTest {

    private lateinit var downloadHandler: DownloadHandler
    private val mockContext: Context = mockk()

    @Before
    fun setUp() {
        mockkStatic(ContextCompat::class)
        downloadHandler = DownloadHandler(mockContext)
    }

    @After
    fun tearDown() {
        unmockkStatic(ContextCompat::class)
    }

    @Test
    fun `URL 유효성 검사 - 올바른 HTTP URL`() {
        // Given
        val validHttpUrl = "http://example.com/video.mp4"
        
        // When
        val result = downloadHandler.isValidUrl(validHttpUrl)
        
        // Then
        assertTrue("HTTP URL은 유효해야 함", result)
    }

    @Test
    fun `URL 유효성 검사 - 올바른 HTTPS URL`() {
        // Given
        val validHttpsUrl = "https://example.com/video.mp4"
        
        // When
        val result = downloadHandler.isValidUrl(validHttpsUrl)
        
        // Then
        assertTrue("HTTPS URL은 유효해야 함", result)
    }

    @Test
    fun `URL 유효성 검사 - 잘못된 URL`() {
        // Given
        val invalidUrls = listOf(
            "ftp://example.com/video.mp4",
            "example.com/video.mp4",
            "//example.com/video.mp4",
            "",
            "   ",
            "invalid-url"
        )
        
        // When & Then
        invalidUrls.forEach { url ->
            val result = downloadHandler.isValidUrl(url)
            assertFalse("잘못된 URL은 무효해야 함: $url", result)
        }
    }

    @Test
    fun `파일명 생성 - 정상적인 MP4 URL`() {
        // Given
        val urlWithMp4 = "https://example.com/videos/sample.mp4"
        
        // When
        val filename = downloadHandler.generateFilename(urlWithMp4)
        
        // Then
        assertEquals("sample.mp4", filename)
    }

    @Test
    fun `파일명 생성 - MP4 확장자 없는 URL`() {
        // Given
        val urlWithoutMp4 = "https://example.com/videos/sample"
        
        // When
        val filename = downloadHandler.generateFilename(urlWithoutMp4)
        
        // Then
        assertEquals("sample.mp4", filename)
    }

    @Test
    fun `파일명 생성 - 경로 없는 URL`() {
        // Given
        val urlWithoutPath = "https://example.com"
        
        // When
        val filename = downloadHandler.generateFilename(urlWithoutPath)
        
        // Then
        assertTrue("타임스탬프가 포함된 파일명 생성", filename.startsWith("video_"))
        assertTrue("MP4 확장자 포함", filename.endsWith(".mp4"))
    }

    @Test
    fun `파일명 생성 - 잘못된 URL`() {
        // Given
        val invalidUrl = "invalid-url"
        
        // When
        val filename = downloadHandler.generateFilename(invalidUrl)
        
        // Then
        assertTrue("잘못된 URL도 안전한 파일명 생성", filename.startsWith("video_"))
        assertTrue("MP4 확장자 포함", filename.endsWith(".mp4"))
    }

    @Test
    fun `권한 확인 - Android 10 이하에서 권한 없음`() {
        // Given
        every { 
            ContextCompat.checkSelfPermission(mockContext, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) 
        } returns PackageManager.PERMISSION_DENIED
        
        // When
        val permissions = downloadHandler.checkStoragePermissions()
        
        // Then
        assertTrue("WRITE_EXTERNAL_STORAGE 권한이 필요함", 
            permissions.contains(android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
    }

    @Test
    fun `권한 확인 - Android 10 이하에서 권한 있음`() {
        // Given
        every { 
            ContextCompat.checkSelfPermission(mockContext, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) 
        } returns PackageManager.PERMISSION_GRANTED
        
        // When
        val permissions = downloadHandler.checkStoragePermissions()
        
        // Then
        assertTrue("권한이 있으면 빈 리스트 반환", permissions.isEmpty())
    }

    @Test
    fun `파일명 생성 - 공백 문자 포함 URL`() {
        // Given
        val urlWithSpaces = "  https://example.com/video file.mp4  "
        
        // When
        val filename = downloadHandler.generateFilename(urlWithSpaces)
        
        // Then
        assertEquals("video file.mp4", filename)
    }

    @Test
    fun `파일명 생성 - 특수 문자 포함 URL`() {
        // Given
        val urlWithSpecialChars = "https://example.com/video%20file.mp4"
        
        // When
        val filename = downloadHandler.generateFilename(urlWithSpecialChars)
        
        // Then
        assertEquals("video%20file.mp4", filename)
    }
}