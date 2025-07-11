package com.swvd.simplewebvideodownloader.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.swvd.simplewebvideodownloader.models.Tab

/**
 * 탭 오버뷰 화면 컴포넌트
 * 전체화면에서 탭 목록을 표시하는 화면
 */
@Composable
fun TabOverviewScreen(
    tabs: List<Tab>,
    currentTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    onTabClosed: (Int) -> Unit,
    onAddNewTab: () -> Unit,
    onBackToWebView: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 상단 헤더
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "탭 관리",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Row {
                // 새 탭 버튼
                IconButton(onClick = onAddNewTab) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "새 탭"
                    )
                }
                
                // 닫기 버튼
                IconButton(onClick = onBackToWebView) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기"
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 탭 목록
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(tabs) { index, tab ->
                TabOverviewItem(
                    tab = tab,
                    isSelected = index == currentTabIndex,
                    onSelect = { onTabSelected(index) },
                    onClose = if (tabs.size > 1) { 
                        { onTabClosed(index) } 
                    } else null
                )
            }
        }
    }
}

/**
 * 탭 오버뷰 개별 아이템
 */
@Composable
private fun TabOverviewItem(
    tab: Tab,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onClose: (() -> Unit)? = null
) {
    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = tab.title.ifEmpty { "새 탭" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (tab.url.isNotEmpty()) {
                    Text(
                        text = tab.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // 닫기 버튼
            if (onClose != null) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "탭 닫기"
                    )
                }
            }
        }
    }
}