package com.example.wordup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wordup.ui.model.MemoryFunnelState
import com.example.wordup.ui.theme.*
import com.example.wordup.ui.util.editorialShadow

/**
 * 艾宾浩斯记忆漏斗数据卡片 (Module 2)
 */
@Composable
fun MemoryFunnelCard(state: MemoryFunnelState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .editorialShadow()
            .clip(RoundedCornerShape(32.dp))
            .background(SurfaceContainerLowest)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "艾宾浩斯记忆漏斗",
            style = AppTypography.headlineMedium,
            color = Primary,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 24.dp)
        )

        // 漏斗图层级构建
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FunnelLayer(
                label = "Phase 1 (Initial)",
                value = state.phase1Count.toString(),
                widthFraction = 1.0f,
                backgroundColor = PrimaryContainer,
                textColor = Primary,
                isTop = true
            )
            FunnelLayer(
                label = "Phase 2",
                value = state.phase2Count.toString(),
                widthFraction = 0.85f,
                backgroundColor = Primary,
                textColor = Color.White
            )
            FunnelLayer(
                label = "Phase 3",
                value = state.phase3Count.toString(),
                widthFraction = 0.70f,
                backgroundColor = Secondary,
                textColor = Color.White
            )
            FunnelLayer(
                label = "Phase 4",
                value = state.phase4Count.toString(),
                widthFraction = 0.55f,
                backgroundColor = SecondaryContainer,
                textColor = Secondary
            )
            FunnelLayer(
                label = "Phase 5",
                value = state.phase5Count.toString(),
                widthFraction = 0.40f,
                backgroundColor = TertiaryFixedDim,
                textColor = Tertiary,
                isBottom = true
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 底部留存率分析文本
        Text(
            text = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = Secondary,
                        fontSize = 16.sp
                    )
                ) {
                    append("${(state.longTermRetentionRate * 100).toInt()}% ")
                }
                append("的词汇已进入长期记忆库")
            },
            style = AppTypography.bodyMedium,
            color = Outline
        )
    }
}

/**
 * 漏斗单层结构定义
 */
@Composable
private fun FunnelLayer(
    label: String,
    value: String,
    widthFraction: Float,
    backgroundColor: Color,
    textColor: Color,
    isTop: Boolean = false,
    isBottom: Boolean = false
) {
    // 动态计算圆角：仅顶部与底部层级保留外侧圆角，形成整体拼合感
    val shape = RoundedCornerShape(
        topStart = if (isTop) 16.dp else 0.dp,
        topEnd = if (isTop) 16.dp else 0.dp,
        bottomStart = if (isBottom) 16.dp else 0.dp,
        bottomEnd = if (isBottom) 16.dp else 0.dp
    )

    Row(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(56.dp)
            .clip(shape)
            .background(backgroundColor)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = AppTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = textColor
        )
        Text(
            text = value,
            style = AppTypography.titleLarge.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
            color = textColor
        )
    }
}