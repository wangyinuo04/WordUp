package com.example.wordup.ui.model
/**
 * 词云单词实体，包含权重以决定显示层级
 */
data class WordCloudItem(
    val word: String,
    val weight: Int // 权重 1-5，数值越大错误次数越多，字体越大
)

/**
 * 难词与热力图状态模型
 */
data class HardWordsState(
    val hardWords: List<WordCloudItem> = listOf(
        WordCloudItem("persistence", 5),
        WordCloudItem("meticulous", 3),
        WordCloudItem("ambiguous", 4),
        WordCloudItem("fluctuate", 2),
        WordCloudItem("resilient", 5),
        WordCloudItem("diligent", 1),
        WordCloudItem("analytical", 4)
    ),
    // 热力图活跃度层级 (0: 无复习, 1-4: 活跃度递增)，模拟近3个月(约36周/格)的数据
    val heatmapLevels: List<Int> = listOf(
        4, 2, 0, 4, 3, 4, 1, 0, 4, 4, 2, 4,
        3, 4, 0, 4, 1, 3, 0, 4, 2, 4, 4, 3,
        0, 2, 4, 1, 4, 3, 4, 0, 2, 4, 3, 0
    )
)