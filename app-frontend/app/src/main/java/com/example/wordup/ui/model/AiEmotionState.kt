package com.example.wordup.ui.model
/**
 * AI 脑力与情绪专注度状态模型
 */
data class AiEmotionState(
    // 雷达图五个维度的分数 (0.0f - 1.0f)，依次为：专注力、积极性、抗压力、恒心、效率
    val radarScores: List<Float> = listOf(0.85f, 0.70f, 0.60f, 0.90f, 0.75f),

    // 近7日情绪积极度/学习效率趋势 (0.0f - 1.0f)
    val weeklyTrend: List<Float> = listOf(0.4f, 0.6f, 0.55f, 0.8f, 0.45f, 0.7f, 0.9f),

    // 异常与干预统计
    val sleepyCount: Int = 2,
    val unfocusedCount: Int = 5,
    val aiHardWordsPushed: Int = 18
)