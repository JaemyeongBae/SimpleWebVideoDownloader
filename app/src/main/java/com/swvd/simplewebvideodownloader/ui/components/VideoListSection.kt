package com.swvd.simplewebvideodownloader.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.swvd.simplewebvideodownloader.models.VideoInfo
import com.swvd.simplewebvideodownloader.models.VideoType

/**
 * 비디오 목록 섹션 컴포넌트
 * 감지된 비디오 목록을 표시하고 다운로드 기능을 제공
 */
@Composable
fun VideoListSection(
    modifier: Modifier = Modifier,
    hasAnalyzed: Boolean,
    videoList: List<VideoInfo>,
    mp4Links: List<String>, // 기존 호환성 유지
    downloadingUrls: Set<String>,
    videoSectionExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onDownloadVideo: (VideoInfo) -> Unit,
    onDownloadMp4: (String) -> Unit // 기존 호환성 유지
) {
    if (!hasAnalyzed) return

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 섹션 헤더 (클릭 가능)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpanded() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (videoList.isEmpty()) {
                        "🎯 감지된 비디오 (없음)"
                    } else {
                        "🎯 감지된 비디오 (${videoList.size}개)"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = if (videoSectionExpanded) "▲" else "▼",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // 접힌 상태에서 간단한 요약 표시
            if (!videoSectionExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                if (videoList.isEmpty()) {
                    Text(
                        text = "이 페이지에서 비디오를 찾을 수 없습니다",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    val downloadableCount = videoList.count { isDownloadable(it) }
                    val typesSummary = videoList.groupBy { it.type }.entries
                        .take(3)
                        .joinToString(", ") { "${it.key.icon}${it.value.size}" }
                    
                    Text(
                        text = "다운로드 가능: ${downloadableCount}개 | $typesSummary",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // 접힐 수 있는 목록 내용
            AnimatedVisibility(
                visible = videoSectionExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    if (videoList.isEmpty()) {
                        // 비디오가 없을 때 표시
                        Text(
                            text = "이 페이지에서 비디오를 찾을 수 없습니다.\n비디오가 iframe이나 스트리밍으로 구현되었을 수 있습니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        // 비디오 타입별 분류 표시
                        VideoTypesSummary(
                            videoList = videoList,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        // 비디오 목록
                        LazyColumn(
                            modifier = Modifier.height(300.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(videoList) { index, video ->
                                VideoItem(
                                    index = index,
                                    video = video,
                                    isDownloading = downloadingUrls.contains(video.url),
                                    onDownload = { onDownloadVideo(video) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 비디오 타입별 요약 표시
 */
@Composable
private fun VideoTypesSummary(
    videoList: List<VideoInfo>,
    modifier: Modifier = Modifier
) {
    val typeCounts = videoList.groupBy { it.type }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }

    if (typeCounts.isNotEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                typeCounts.take(4).forEach { (videoType, videoCount) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = videoType.icon,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "${videoCount}개",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = videoType.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * 개별 비디오 아이템
 */
@Composable
private fun VideoItem(
    index: Int,
    video: VideoInfo,
    isDownloading: Boolean,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 비디오 정보 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = video.type.icon,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = video.type.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // 다운로드 가능 여부 표시
                if (isDownloadable(video)) {
                    Text(
                        text = "⬇️",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 제목 표시 (있는 경우)
            if (!video.title.isNullOrBlank()) {
                Text(
                    text = "제목: ${video.title}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            // URL 표시
            Text(
                text = "${index + 1}. ${if (video.url.length > 50) "...${video.url.takeLast(50)}" else video.url}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // 다운로드 버튼 (다운로드 가능한 경우만)
            if (isDownloadable(video)) {
                Button(
                    onClick = onDownload,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isDownloading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (video.type) {
                            VideoType.HLS -> MaterialTheme.colorScheme.secondary
                            VideoType.MP4 -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("다운로드 중...")
                    } else {
                        Text("${video.type.icon} 다운로드")
                    }
                }
            } else {
                // 다운로드 불가능한 경우 안내
                Text(
                    text = when (video.type) {
                        VideoType.DASH -> "DASH 스트리밍은 지원 예정"
                        VideoType.YOUTUBE -> "YouTube는 별도 처리 필요"
                        VideoType.VIMEO -> "Vimeo는 별도 처리 필요"
                        else -> "다운로드 지원 안함"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

/**
 * 다운로드 가능 여부 확인
 */
private fun isDownloadable(video: VideoInfo): Boolean {
    return when (video.type) {
        VideoType.MP4,
        VideoType.WEBM,
        VideoType.MKV,
        VideoType.AVI,
        VideoType.MOV,
        VideoType.FLV,
        VideoType.HLS -> true
        else -> false
    }
}