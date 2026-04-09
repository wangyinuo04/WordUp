package com.example.wordup.ui.screens

import androidx.compose.ui.platform.ComposeView
import com.example.wordup.ui.theme.ArchivistTheme

fun setupStatisticsComposeView(composeView: ComposeView, currentUserId: Long) {
    composeView.setContent {
        ArchivistTheme {
            // 传递登录用户的 ID
            StatisticsScreen(userId = currentUserId)
        }
    }
}