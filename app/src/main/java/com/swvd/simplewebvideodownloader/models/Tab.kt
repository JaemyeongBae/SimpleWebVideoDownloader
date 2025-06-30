package com.swvd.simplewebvideodownloader.models

import java.util.UUID

/**
 * 탭 데이터 모델
 * 브라우저 탭의 정보를 저장하는 데이터 클래스
 */
data class Tab(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "새 탭",
    var url: String = "https://www.google.com"
) 