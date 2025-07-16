package com.swvd.simplewebvideodownloader.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 네비게이션 하단 바 컴포넌트
 * WebView 네비게이션 버튼들을 제공
 */
@Composable
fun NavigationBottomBar(
    modifier: Modifier = Modifier,
    canGoBack: Boolean,
    canGoForward: Boolean,
    isAnalyzing: Boolean,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    onRefresh: () -> Unit,
    onFullscreen: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom))
            .padding(horizontal = 16.dp, vertical = 20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 뒤로가기 버튼
            NavigationButton(
                onClick = onGoBack,
                enabled = canGoBack,
                icon = {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "뒤로가기",
                        modifier = Modifier.size(20.dp)
                    )
                },
                containerColor = if (canGoBack) 
                    MaterialTheme.colorScheme.secondaryContainer 
                else 
                    MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (canGoBack) 
                    MaterialTheme.colorScheme.onSecondaryContainer 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )

            // 새로고침 버튼
            NavigationButton(
                onClick = onRefresh,
                enabled = !isAnalyzing,
                icon = {
                    if (isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "새로고침",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            )

            // 앞으로가기 버튼
            NavigationButton(
                onClick = onGoForward,
                enabled = canGoForward,
                icon = {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "앞으로가기",
                        modifier = Modifier.size(20.dp)
                    )
                },
                containerColor = if (canGoForward) 
                    MaterialTheme.colorScheme.secondaryContainer 
                else 
                    MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (canGoForward) 
                    MaterialTheme.colorScheme.onSecondaryContainer 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )

            // 전체화면 버튼
            NavigationButton(
                onClick = onFullscreen,
                enabled = true,
                icon = {
                    Text(
                        text = "⛶",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/**
 * 개별 네비게이션 버튼
 */
@Composable
private fun NavigationButton(
    onClick: () -> Unit,
    enabled: Boolean,
    icon: @Composable () -> Unit,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(44.dp),
        containerColor = containerColor,
        contentColor = contentColor
    ) {
        icon()
    }
}

/**
 * 전체화면용 네비게이션 바
 */
@Composable
fun FullscreenNavigationBar(
    modifier: Modifier = Modifier,
    canGoBack: Boolean,
    canGoForward: Boolean,
    isAnalyzing: Boolean,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    onRefresh: () -> Unit,
    onExitFullscreen: () -> Unit,
    onShowMp4List: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 뒤로가기
            IconButton(
                onClick = onGoBack,
                enabled = canGoBack
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = if (canGoBack) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }

            // 앞으로가기
            IconButton(
                onClick = onGoForward,
                enabled = canGoForward
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "앞으로가기",
                    tint = if (canGoForward) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }

            // 새로고침
            IconButton(
                onClick = onRefresh,
                enabled = !isAnalyzing
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "새로고침",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 비디오 목록
            IconButton(onClick = onShowMp4List) {
                Text(
                    text = "🎬",
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            // 전체화면 종료
            IconButton(onClick = onExitFullscreen) {
                Text(
                    text = "⤹",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * 컴팩트 네비게이션 바 (공간이 부족할 때)
 */
@Composable
fun CompactNavigationBar(
    modifier: Modifier = Modifier,
    canGoBack: Boolean,
    canGoForward: Boolean,
    isAnalyzing: Boolean,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    onRefresh: () -> Unit,
    onFullscreen: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 뒤로/앞으로 결합 버튼
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilledTonalIconButton(
                onClick = onGoBack,
                enabled = canGoBack,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "뒤로가기",
                    modifier = Modifier.size(16.dp)
                )
            }

            FilledTonalIconButton(
                onClick = onGoForward,
                enabled = canGoForward,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "앞으로가기",
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // 새로고침
        FilledTonalIconButton(
            onClick = onRefresh,
            enabled = !isAnalyzing,
            modifier = Modifier.size(36.dp)
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "새로고침",
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // 전체화면
        FilledIconButton(
            onClick = onFullscreen,
            modifier = Modifier.size(36.dp)
        ) {
            Text(
                text = "⛶",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}