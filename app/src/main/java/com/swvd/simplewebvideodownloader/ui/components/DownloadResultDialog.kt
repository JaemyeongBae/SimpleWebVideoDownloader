package com.swvd.simplewebvideodownloader.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * 다운로드 결과 다이얼로그
 * 다운로드 성공/실패 메시지를 표시
 */
@Composable
fun DownloadResultDialog(
    show: Boolean,
    message: String?,
    onDismiss: () -> Unit
) {
    if (show) {
        Dialog(onDismissRequest = onDismiss) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val messageText = message ?: "알 수 없는 오류"
                    
                    // 아이콘
                    Text(
                        text = if (messageText.contains("성공") || messageText.contains("완료")) "✅" else "❌",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 제목
                    Text(
                        text = if (messageText.contains("성공") || messageText.contains("완료")) 
                            "다운로드 완료" else "다운로드 실패",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 메시지
                    Text(
                        text = messageText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // 확인 버튼
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("확인")
                    }
                }
            }
        }
    }
}