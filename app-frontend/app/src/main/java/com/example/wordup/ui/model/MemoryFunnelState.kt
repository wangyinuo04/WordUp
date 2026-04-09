package com.example.wordup.ui.model
/**
 * 艾宾浩斯记忆漏斗状态模型
 */
data class MemoryFunnelState(
    val phase1Count: Int = 520, // 阶段1：初次学习
    val phase2Count: Int = 340, // 阶段2：1天后复习
    val phase3Count: Int = 210, // 阶段3：2天后复习
    val phase4Count: Int = 150, // 阶段4：4天后复习
    val phase5Count: Int = 98,  // 阶段5：7天以上（牢固掌握）
    val longTermRetentionRate: Float = 0.18f // 长期记忆转化率
)