package com.swvd.simplewebvideodownloader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.swvd.simplewebvideodownloader.models.Tab
import com.swvd.simplewebvideodownloader.ui.components.TabOverviewScreen

/**
 * 전체화면 UI 컴포넌트
 * 웹뷰를 전체화면으로 표시할 때 사용하는 UI
 */
@Composable
fun FullscreenUI(
    tabs: List<Tab>,
    currentTabIndex: Int,
    currentUrl: String,
    urlText: String,
    onUrlTextChange: (String) -> Unit,
    onLoadUrl: (String) -> Unit,
    showTabOverview: Boolean,
    onShowTabOverview: (Boolean) -> Unit,
    onAddNewTab: () -> Unit,
    canGoBack: Boolean,
    canGoForward: Boolean,
    isAnalyzing: Boolean,
    mp4Links: List<String>,
    downloadingUrls: Set<String>,
    urlHistory: List<String>,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    onRefresh: () -> Unit,
    onShowMp4List: () -> Unit,
    onSwitchTab: (Int) -> Unit,
    onCloseTab: (Int) -> Unit,
    onDownloadVideo: (String) -> Unit,
    onExitFullscreen: () -> Unit,
    webViewContent: @Composable () -> Unit
) {
    var isEditingUrl by remember { mutableStateOf(false) }
    var editUrlText by remember { mutableStateOf("") }
    var showHistoryDropdown by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showTabOverview) {
            // 탭 오버뷰 화면
            TabOverviewScreen(
                tabs = tabs,
                currentTabIndex = currentTabIndex,
                onTabSelected = { index ->
                    onSwitchTab(index)
                    onShowTabOverview(false)
                },
                onTabClosed = { index ->
                    onCloseTab(index)
                    if (tabs.size == 1) {
                        onShowTabOverview(false)
                    }
                },
                onAddNewTab = {
                    onAddNewTab()
                    onShowTabOverview(false)
                },
                onBackToWebView = { onShowTabOverview(false) }
            )
        } else {
            // 웹뷰 화면
            Column(modifier = Modifier.fillMaxSize()) {
                // 상단 바
                FullscreenTopBar(
                    currentUrl = currentUrl,
                    isEditingUrl = isEditingUrl,
                    editUrlText = editUrlText,
                    tabs = tabs,
                    urlHistory = urlHistory,
                    showHistoryDropdown = showHistoryDropdown,
                    onEditUrlTextChange = { editUrlText = it },
                    onStartEditingUrl = { 
                        editUrlText = currentUrl
                        isEditingUrl = true 
                    },
                    onStopEditingUrl = { isEditingUrl = false },
                    onShowHistoryDropdown = { showHistoryDropdown = it },
                    onLoadUrl = onLoadUrl,
                    onAddNewTab = onAddNewTab,
                    onShowTabOverview = { onShowTabOverview(true) },
                    onExitFullscreen = onExitFullscreen
                )

                // 웹뷰 콘텐츠
                Box(modifier = Modifier.weight(1f)) {
                    webViewContent()
                }

                // 하단 네비게이션 바
                FullscreenBottomBar(
                    canGoBack = canGoBack,
                    canGoForward = canGoForward,
                    isAnalyzing = isAnalyzing,
                    urlHistory = urlHistory,
                    onGoBack = onGoBack,
                    onGoForward = onGoForward,
                    onRefresh = onRefresh,
                    onShowMp4List = onShowMp4List,
                    onShowHistory = { showHistoryDropdown = !showHistoryDropdown }
                )
            }
        }
    }
}

/**
 * 전체화면 상단 바
 */
@Composable
private fun FullscreenTopBar(
    currentUrl: String,
    isEditingUrl: Boolean,
    editUrlText: String,
    tabs: List<Tab>,
    urlHistory: List<String>,
    showHistoryDropdown: Boolean,
    onEditUrlTextChange: (String) -> Unit,
    onStartEditingUrl: () -> Unit,
    onStopEditingUrl: () -> Unit,
    onShowHistoryDropdown: (Boolean) -> Unit,
    onLoadUrl: (String) -> Unit,
    onAddNewTab: () -> Unit,
    onShowTabOverview: () -> Unit,
    onExitFullscreen: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top)),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 좌측: URL 표시/편집
            if (isEditingUrl) {
                OutlinedTextField(
                    value = editUrlText,
                    onValueChange = onEditUrlTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("URL 입력") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            if (editUrlText.isNotBlank()) {
                                val url = if (!editUrlText.startsWith("http")) {
                                    "https://$editUrlText"
                                } else {
                                    editUrlText
                                }
                                onLoadUrl(url)
                            }
                            onStopEditingUrl()
                        }
                    ),
                    trailingIcon = {
                        Row {
                            IconButton(
                                onClick = {
                                    if (editUrlText.isNotBlank()) {
                                        val url = if (!editUrlText.startsWith("http")) {
                                            "https://$editUrlText"
                                        } else {
                                            editUrlText
                                        }
                                        onLoadUrl(url)
                                    }
                                    onStopEditingUrl()
                                }
                            ) {
                                Icon(Icons.Default.Check, "확인")
                            }
                            IconButton(onClick = onStopEditingUrl) {
                                Icon(Icons.Default.Close, "취소")
                            }
                            IconButton(onClick = { onShowHistoryDropdown(!showHistoryDropdown) }) {
                                Icon(Icons.Default.Info, "기록")
                            }
                        }
                    },
                    singleLine = true
                )
            } else {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onStartEditingUrl() },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = currentUrl.takeIf { it.isNotEmpty() } ?: "URL을 입력하세요",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 우측: 새 탭 추가, 탭 개수, 전체화면 종료 (3개 버튼)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. 새 탭 추가 버튼
                IconButton(onClick = onAddNewTab) {
                    Icon(
                        Icons.Default.Add, 
                        contentDescription = "새 탭",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // 2. 탭 개수 (탭 오버뷰로 이동)
                Surface(
                    modifier = Modifier.clickable { onShowTabOverview() },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = tabs.size.toString(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                
                // 3. 전체화면 종료 버튼
                IconButton(onClick = onExitFullscreen) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "전체화면 종료",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // URL 히스토리 드롭다운
        if (showHistoryDropdown && urlHistory.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp)
            ) {
                items(urlHistory.reversed()) { url ->
                    Text(
                        text = url,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onLoadUrl(url)
                                onShowHistoryDropdown(false)
                            }
                            .padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * 전체화면 하단 네비게이션 바
 */
@Composable
private fun FullscreenBottomBar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    isAnalyzing: Boolean,
    urlHistory: List<String>,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    onRefresh: () -> Unit,
    onShowMp4List: () -> Unit,
    onShowHistory: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom)),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. 뒤로가기
            IconButton(
                onClick = onGoBack,
                enabled = canGoBack
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    "뒤로가기",
                    tint = if (canGoBack) 
                        MaterialTheme.colorScheme.onSurface 
                    else 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }

            // 2. 새로고침 및 MP4 감지
            IconButton(
                onClick = onRefresh
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(Icons.Default.Refresh, "새로고침")
                }
            }

            // 3. MP4 목록 보기
            IconButton(
                onClick = onShowMp4List
            ) {
                Icon(Icons.Default.PlayArrow, "MP4 목록")
            }

            // 4. 최근 방문한 페이지 목록
            IconButton(
                onClick = onShowHistory,
                enabled = urlHistory.isNotEmpty()
            ) {
                Icon(
                    Icons.Default.Info,
                    "방문 기록",
                    tint = if (urlHistory.isNotEmpty()) 
                        MaterialTheme.colorScheme.onSurface 
                    else 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }

            // 5. 앞으로가기
            IconButton(
                onClick = onGoForward,
                enabled = canGoForward
            ) {
                Icon(
                    Icons.Default.ArrowForward,
                    "앞으로가기",
                    tint = if (canGoForward) 
                        MaterialTheme.colorScheme.onSurface 
                    else 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }
    }
} 