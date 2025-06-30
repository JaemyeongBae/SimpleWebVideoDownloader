package com.swvd.simplewebvideodownloader.utils

import android.app.Activity
import android.view.View
import android.view.WindowInsetsController
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/**
 * 전체화면 모드 관리 유틸리티
 * 몰입형 전체화면 모드 설정을 담당
 */
object FullscreenManager {
    
    /**
     * 전체화면 몰입형 모드 설정/해제
     * @param activity 대상 액티비티
     * @param enable 전체화면 모드 활성화 여부
     */
    fun setFullscreenMode(activity: Activity, enable: Boolean) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (enable) {
                // Android 11+ 에서 몰입형 모드
                activity.window.insetsController?.let { controller ->
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                // 몰입형 모드 해제
                activity.window.insetsController?.show(WindowInsetsCompat.Type.systemBars())
            }
        } else {
            @Suppress("DEPRECATION")
            if (enable) {
                // Android 10 이하에서 몰입형 모드
                activity.window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
            } else {
                // 몰입형 모드 해제
                activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }
    
    /**
     * Edge-to-Edge 모드 활성화
     * @param activity 대상 액티비티
     */
    fun enableEdgeToEdge(activity: Activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
    }
} 