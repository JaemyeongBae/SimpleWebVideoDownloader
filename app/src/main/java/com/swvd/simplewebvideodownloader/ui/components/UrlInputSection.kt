package com.swvd.simplewebvideodownloader.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * URL 입력 섹션 컴포넌트
 * URL 입력 필드와 관련 버튼들을 포함
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrlInputSection(
    modifier: Modifier = Modifier,
    urlText: String,
    currentUrl: String,
    isAnalyzing: Boolean,
    urlSectionExpanded: Boolean,
    onUrlTextChange: (String) -> Unit,
    onLoadUrl: () -> Unit,
    onReset: () -> Unit,
    onRefresh: () -> Unit,
    onToggleExpanded: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // 섹션 헤더 (클릭 가능)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpanded() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "웹 페이지 URL 입력",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (urlSectionExpanded) "▲" else "▼",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // 로드된 URL이 있고 섹션이 접혀있을 때 간단히 표시
            if (!urlSectionExpanded && currentUrl.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentUrl.let { url ->
                        if (url.length > 40) "...${url.takeLast(37)}" else url
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 접힐 수 있는 내용
            AnimatedVisibility(
                visible = urlSectionExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(6.dp))

                    // URL 입력 필드
                    OutlinedTextField(
                        value = urlText,
                        onValueChange = onUrlTextChange,
                        label = { Text("URL을 입력하세요") },
                        placeholder = { Text("https://example.com") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = { onLoadUrl() }
                        ),
                        trailingIcon = {
                            TextButton(
                                onClick = {
                                    clipboardManager.getText()?.text?.let { clipText ->
                                        if (clipText.contains(".")) {
                                            onUrlTextChange(clipText)
                                        }
                                    }
                                }
                            ) {
                                Text("붙여넣기")
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 버튼들
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 첫 번째 줄: 페이지 로드, 초기화
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onLoadUrl,
                                modifier = Modifier.weight(1f),
                                enabled = urlText.isNotBlank() && !isAnalyzing
                            ) {
                                if (isAnalyzing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("분석 중...")
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("페이지 로드")
                                }
                            }

                            Button(
                                onClick = onReset,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("초기화")
                            }
                        }

                        // 두 번째 줄: 새로고침 (현재 URL이 있을 때만 표시)
                        if (currentUrl.isNotEmpty()) {
                            Button(
                                onClick = onRefresh,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isAnalyzing,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary
                                )
                            ) {
                                if (isAnalyzing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onTertiary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("새로고침 중...")
                                } else {
                                    Text("🔄 새로고침 & 비디오 감지")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}