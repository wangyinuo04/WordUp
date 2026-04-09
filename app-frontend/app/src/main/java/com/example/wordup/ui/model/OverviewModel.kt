package com.example.wordup.ui.model
/**
 * 学习概览数据状态实体
 */
data class LearningOverviewState(
    val streakDays: Int = 45,
    val todayProgress: Float = 0.85f,
    val newWordsCount: Int = 12,
    val reviewWordsCount: Int = 85,
    val studyMinutes: Int = 42
)