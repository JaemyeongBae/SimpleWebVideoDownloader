package com.swvd.simplewebvideodownloader.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
 * 탭바 컴포넌트
 * 여러 탭을 관리하고 표시
 */
@Composable
fun TabBar(
    tabs: List<Tab>,
    currentTabIndex: Int,
    onNewTab: () -> Unit,
    onCloseTab: (Int) -> Unit,
    onSwitchTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 탭 목록
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(tabs) { index, tab ->
                    TabItem(
                        tab = tab,
                        isSelected = index == currentTabIndex,
                        onSelect = { onSwitchTab(index) },
                        onClose = if (tabs.size > 1) { 
                            { onCloseTab(index) } 
                        } else null
                    )
                }
            }
            
            // 새 탭 추가 버튼
            FilledTonalIconButton(
                onClick = onNewTab,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "새 탭",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * 개별 탭 아이템
 */
@Composable
private fun TabItem(
    tab: Tab,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onClose: (() -> Unit)? = null
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Card(
        onClick = onSelect,
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        modifier = Modifier.widthIn(max = 150.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 탭 제목
            Text(
                text = tab.title.ifEmpty { "새 탭" },
                style = MaterialTheme.typography.bodySmall,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            // 닫기 버튼 (여러 탭이 있을 때만)
            if (onClose != null) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "탭 닫기",
                        tint = contentColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}