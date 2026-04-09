package com.example.appbackend.vo;

import lombok.Data;
import java.util.List;

/**
 * 统计页面全局聚合视图对象
 */
@Data
public class StatisticsDashboardVO {
    private LearningOverviewVO overview;
    private MemoryFunnelVO funnel;
    private AiEmotionVO aiEmotion;
    private HardWordsMapVO hardWordsMap;

    @Data
    public static class LearningOverviewVO {
        private Integer streakDays;
        private Float todayProgress;
        private Integer newWordsCount;
        private Integer reviewWordsCount;
        private Integer studyMinutes;
    }

    @Data
    public static class MemoryFunnelVO {
        private Integer phase1Count;
        private Integer phase2Count;
        private Integer phase3Count;
        private Integer phase4Count;
        private Integer phase5Count;
        private Float longTermRetentionRate;
    }

    @Data
    public static class AiEmotionVO {
        private List<Float> radarScores; // 维度分数 [专注力, 积极性, 抗压力, 恒心, 效率]
        private List<Float> weeklyTrend; // 近七日积极情绪趋势
        private Integer sleepyCount;
        private Integer unfocusedCount;
        private Integer aiHardWordsPushed;
    }

    @Data
    public static class HardWordsMapVO {
        private List<WordCloudItem> hardWords;
        private List<Integer> heatmapLevels; // 热力图层级列表

        @Data
        public static class WordCloudItem {
            private String word;
            private Integer weight; // 1-5 权重
        }
    }
}