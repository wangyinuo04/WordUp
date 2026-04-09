package com.example.wordup.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wordup.ui.components.*
import com.example.wordup.ui.theme.Primary
import com.example.wordup.ui.theme.Surface

@Composable
fun StatisticsScreen(userId: Long, viewModel: StatisticsViewModel = viewModel()) {
    // 首次进入页面时，触发网络请求获取数据
    LaunchedEffect(userId) {
        viewModel.loadStatisticsData(userId)
    }

    Scaffold(containerColor = Surface) { innerPadding ->
        if (viewModel.isLoading) {
            // 加载时的居中动画指示器
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else {
            // 数据加载完成，使用 ViewModel 中的真实状态渲染图表
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                CoreOverviewCard(state = viewModel.overviewState)
                MemoryFunnelCard(state = viewModel.funnelState)
                AiEmotionCard(state = viewModel.aiEmotionState)
                HardWordsCard(state = viewModel.hardWordsState)
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}