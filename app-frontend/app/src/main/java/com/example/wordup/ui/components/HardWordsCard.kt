package com.example.wordup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wordup.ui.model.HardWordsState
import com.example.wordup.ui.model.WordCloudItem
import com.example.wordup.ui.theme.*
import com.example.wordup.ui.util.editorialShadow

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun HardWordsCard(state: HardWordsState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .editorialShadow()
            .clip(RoundedCornerShape(32.dp))
            .background(SurfaceContainerLowest)
            .padding(28.dp)
    ) {
        Text(
            text = "难词与错点图谱",
            style = AppTypography.headlineMedium,
            color = Primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // 1. 难词词云区
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceContainerLow)
                .padding(vertical = 32.dp, horizontal = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            maxItemsInEachRow = 3
        ) {
            state.hardWords.forEach { item ->
                WordCloudText(item)
                Spacer(modifier = Modifier.width(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 2. 学习热力图区
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "学习热力图",
                style = AppTypography.titleLarge,
                color = Primary
            )
            Text(
                text = "LAST 3 MONTHS",
                style = AppTypography.labelMedium.copy(fontSize = 10.sp),
                color = Outline
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        HeatmapGrid(levels = state.heatmapLevels)
    }
}

@Composable
private fun WordCloudText(item: WordCloudItem) {
    // 根据单词权重计算对应的字号与颜色体系
    val fontSize = when (item.weight) {
        5 -> 28.sp
        4 -> 24.sp
        3 -> 20.sp
        2 -> 18.sp
        else -> 16.sp
    }

    val color = when (item.weight) {
        5 -> Secondary
        4 -> PrimaryContainer
        3 -> Tertiary
        2 -> Outline
        else -> OutlineVariant
    }

    val fontWeight = when (item.weight) {
        5 -> FontWeight.ExtraBold
        4 -> FontWeight.Bold
        3 -> FontWeight.SemiBold
        2 -> FontWeight.Medium
        else -> FontWeight.Normal
    }

    Text(
        text = item.word,
        style = AppTypography.headlineMedium.copy(fontSize = fontSize, fontWeight = fontWeight),
        color = color
    )
}

@Composable
private fun HeatmapGrid(levels: List<Int>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLow)
            .padding(16.dp)
    ) {
        // 渲染 3 行 12 列的热力图矩阵
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (row in 0 until 3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (col in 0 until 12) {
                        val index = row * 12 + col
                        val level = levels.getOrElse(index) { 0 }
                        HeatmapBlock(level = level)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 热力图图例指示器
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Less", style = AppTypography.labelMedium.copy(fontSize = 10.sp), color = Outline)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                HeatmapBlock(level = 0, size = 10.dp)
                HeatmapBlock(level = 1, size = 10.dp)
                HeatmapBlock(level = 3, size = 10.dp)
                HeatmapBlock(level = 4, size = 10.dp)
            }
            Text(text = "More", style = AppTypography.labelMedium.copy(fontSize = 10.sp), color = Outline)
        }
    }
}

@Composable
private fun HeatmapBlock(level: Int, size: androidx.compose.ui.unit.Dp = 22.dp) {
    val blockColor = when (level) {
        0 -> SurfaceContainerHighest
        1 -> Secondary.copy(alpha = 0.3f)
        2 -> Secondary.copy(alpha = 0.5f)
        3 -> Secondary.copy(alpha = 0.8f)
        else -> Secondary
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(4.dp))
            .background(blockColor)
    )
}