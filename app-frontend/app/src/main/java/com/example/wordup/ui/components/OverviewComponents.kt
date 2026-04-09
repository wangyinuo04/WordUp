package com.example.wordup.ui.components
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wordup.ui.theme.*
import com.example.wordup.ui.model.LearningOverviewState
import com.example.wordup.ui.util.editorialShadow

@Composable
fun ProgressRing(progress: Float, modifier: Modifier = Modifier) {
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Canvas(modifier = Modifier.size(96.dp)) {
            drawCircle(
                color = SurfaceContainerHigh,
                style = Stroke(width = 8.dp.toPx())
            )
            drawArc(
                color = Secondary,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(progress * 100).toInt()}%",
                style = AppTypography.headlineMedium.copy(fontSize = 20.sp, color = Secondary)
            )
            Text(
                text = "今日目标",
                style = AppTypography.labelMedium.copy(fontSize = 8.sp, color = Outline)
            )
        }
    }
}

@Composable
fun CoreOverviewCard(state: LearningOverviewState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .editorialShadow()
            .clip(RoundedCornerShape(32.dp))
            .background(SurfaceContainerLowest)
    ) {
        // 背景装饰圆环：已修正对齐方式，确保与进度条中心轴线和谐
        Box(
            modifier = Modifier
                .size(140.dp)
                .align(Alignment.TopEnd)
                .offset(x = 20.dp, y = (-20).dp)
                .background(Secondary.copy(alpha = 0.05f), CircleShape)
        )

        Column(modifier = Modifier.padding(28.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "核心学习概览",
                        style = AppTypography.headlineMedium,
                        color = Primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(TertiaryFixedDim)
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WorkspacePremium,
                            contentDescription = null,
                            tint = Tertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "连续打卡 ${state.streakDays} 天",
                            style = AppTypography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Tertiary
                        )
                    }
                }
                ProgressRing(progress = state.todayProgress)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MetricItem("新学", state.newWordsCount.toString(), Modifier.weight(1f))
                MetricItem("复习", state.reviewWordsCount.toString(), Modifier.weight(1f))
                MetricItem("时长", "${state.studyMinutes}m", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceContainerLow)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = AppTypography.displayLarge.copy(fontSize = 32.sp),
            color = Primary
        )
        Text(
            text = label,
            style = AppTypography.labelMedium,
            color = Outline
        )
    }
}