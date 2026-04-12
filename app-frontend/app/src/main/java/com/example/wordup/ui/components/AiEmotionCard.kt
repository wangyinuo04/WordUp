package com.example.wordup.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wordup.ui.model.AiEmotionState
import com.example.wordup.ui.theme.*
import com.example.wordup.ui.util.editorialShadow
import kotlin.math.cos
import kotlin.math.sin

/**
 * AI 脑力与情绪专注度卡片 (Module 3)
 */
@Composable
fun AiEmotionCard(state: AiEmotionState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .editorialShadow()
            .clip(RoundedCornerShape(32.dp))
            .background(SurfaceContainerLowest)
            .padding(28.dp)
    ) {
        Text(
            text = "AI 脑力与情绪专注度",
            style = AppTypography.headlineMedium,
            color = Primary,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // 1. 五维雷达图
        RadarChart(
            scores = state.radarScores,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 2. 近七日趋势图 (改造为展示每日专注度)
        Text(
            text = "近7日专注度走势",
            style = AppTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Outline,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )
        WeeklyTrendChart(trendData = state.weeklyTrend)

        Spacer(modifier = Modifier.height(28.dp))

        // 3. 异常状态微型卡片
        MicroStatusCard(
            icon = Icons.Rounded.NightsStay,
            label = "瞌睡记录",
            value = "${state.sleepyCount}次",
            backgroundColor = SurfaceContainerLow,
            contentColor = Primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        MicroStatusCard(
            icon = Icons.Rounded.VisibilityOff,
            label = "专注偏离",
            value = "${state.unfocusedCount}次",
            backgroundColor = SurfaceContainerLow,
            contentColor = Primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        MicroStatusCard(
            icon = Icons.Rounded.Psychology,
            label = "情绪调度推送难词",
            value = "${state.aiHardWordsPushed}个",
            backgroundColor = TertiaryFixedDim,
            contentColor = Tertiary,
            isHighlighted = true
        )
    }
}

@Composable
private fun RadarChart(scores: List<Float>, modifier: Modifier = Modifier) {
    val labels = listOf("专注力", "积极性", "抗压力", "恒心", "效率")

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = center
            val radius = size.minDimension / 2.5f
            val angles = List(5) { it * (2 * Math.PI / 5) - Math.PI / 2 }

            // 绘制底图同心多边形
            for (step in 1..3) {
                val stepRadius = radius * (step / 3f)
                val path = Path().apply {
                    angles.forEachIndexed { index, angle ->
                        val x = center.x + stepRadius * cos(angle).toFloat()
                        val y = center.y + stepRadius * sin(angle).toFloat()
                        if (index == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
                drawPath(
                    path = path,
                    color = OutlineVariant.copy(alpha = 0.3f),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // 绘制数据多边形
            val dataPath = Path().apply {
                angles.forEachIndexed { index, angle ->
                    val score = scores.getOrElse(index) { 0f }
                    val x = center.x + radius * score * cos(angle).toFloat()
                    val y = center.y + radius * score * sin(angle).toFloat()
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(
                path = dataPath,
                color = Secondary.copy(alpha = 0.2f)
            )
            drawPath(
                path = dataPath,
                color = Secondary,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // 绘制维度文本标签
        val radiusOffset = 110.dp
        val angles = List(5) { it * (2 * Math.PI / 5) - Math.PI / 2 }
        labels.forEachIndexed { index, label ->
            val angle = angles[index]
            Box(
                modifier = Modifier.offset(
                    x = radiusOffset * cos(angle).toFloat(),
                    y = radiusOffset * sin(angle).toFloat()
                )
            ) {
                Text(
                    text = label,
                    style = AppTypography.labelMedium.copy(fontSize = 10.sp),
                    color = Outline
                )
            }
        }
    }
}

@Composable
private fun WeeklyTrendChart(trendData: List<Float>) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            trendData.forEach { fraction ->
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .fillMaxHeight(fraction)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(Secondary.copy(alpha = 0.3f))
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            days.forEach { day ->
                Text(
                    text = day,
                    style = AppTypography.labelMedium.copy(fontSize = 10.sp),
                    color = Outline
                )
            }
        }
    }
}

@Composable
private fun MicroStatusCard(
    icon: ImageVector,
    label: String,
    value: String,
    backgroundColor: Color,
    contentColor: Color,
    isHighlighted: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isHighlighted) contentColor else Outline,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = AppTypography.bodyMedium.copy(
                    fontWeight = if (isHighlighted) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (isHighlighted) contentColor else OnSurface
            )
        }
        Text(
            text = value,
            style = AppTypography.headlineMedium.copy(fontSize = 16.sp),
            color = contentColor
        )
    }
}