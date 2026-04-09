package com.example.appbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.appbackend.entity.User;
import com.example.appbackend.entity.UserDailyStats;
import com.example.appbackend.entity.UserPlan;
import com.example.appbackend.mapper.StatisticsMapper;
import com.example.appbackend.mapper.UserDailyStatsMapper;
import com.example.appbackend.mapper.UserMapper;
import com.example.appbackend.mapper.UserPlanMapper;
import com.example.appbackend.service.StatisticsService;
import com.example.appbackend.vo.StatisticsDashboardVO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private final StatisticsMapper statisticsMapper;
    private final UserDailyStatsMapper userDailyStatsMapper;
    private final UserPlanMapper userPlanMapper;
    private final UserMapper userMapper; // 新增：用于查询真实的 User 表

    public StatisticsServiceImpl(StatisticsMapper statisticsMapper,
                                 UserDailyStatsMapper userDailyStatsMapper,
                                 UserPlanMapper userPlanMapper,
                                 UserMapper userMapper) {
        this.statisticsMapper = statisticsMapper;
        this.userDailyStatsMapper = userDailyStatsMapper;
        this.userPlanMapper = userPlanMapper;
        this.userMapper = userMapper; // 新增
    }

    @Override
    public StatisticsDashboardVO getDashboardStats(Long userId) {
        StatisticsDashboardVO dashboard = new StatisticsDashboardVO();

        // 依次装载四个核心板块的数据
        dashboard.setOverview(buildOverview(userId));
        dashboard.setFunnel(buildMemoryFunnel(userId));
        dashboard.setAiEmotion(buildAiEmotion(userId));
        dashboard.setHardWordsMap(buildHardWordsMap(userId));

        return dashboard;
    }

    private StatisticsDashboardVO.LearningOverviewVO buildOverview(Long userId) {
        StatisticsDashboardVO.LearningOverviewVO overview = new StatisticsDashboardVO.LearningOverviewVO();

        // 获取今日学习数据
        LambdaQueryWrapper<UserDailyStats> statsWrapper = new LambdaQueryWrapper<>();
        statsWrapper.eq(UserDailyStats::getUserId, userId)
                .eq(UserDailyStats::getRecordDate, LocalDate.now());
        UserDailyStats todayStats = userDailyStatsMapper.selectOne(statsWrapper);

        // 调用自定义的 UserPlanMapper.selectByUserId 方法
        UserPlan userPlan = userPlanMapper.selectByUserId(userId);

        if (todayStats != null) {
            overview.setNewWordsCount(todayStats.getLearnedCount() != null ? todayStats.getLearnedCount() : 0);
            overview.setReviewWordsCount(todayStats.getReviewedCount() != null ? todayStats.getReviewedCount() : 0);
            overview.setStudyMinutes(todayStats.getStudyMinutes() != null ? todayStats.getStudyMinutes() : 0);

            int target = (userPlan != null && userPlan.getDailyTarget() != null) ? userPlan.getDailyTarget() : 150;
            int completed = overview.getNewWordsCount() + overview.getReviewWordsCount();
            overview.setTodayProgress(target > 0 ? Math.min((float) completed / target, 1.0f) : 0f);
        } else {
            overview.setNewWordsCount(0);
            overview.setReviewWordsCount(0);
            overview.setStudyMinutes(0);
            overview.setTodayProgress(0f);
        }

        // 【修正点1】查询真实的用户表，获取真实的连续打卡天数
        User user = userMapper.selectById(userId);
        overview.setStreakDays(user != null && user.getStreakDays() != null ? user.getStreakDays() : 0);

        return overview;
    }

    private StatisticsDashboardVO.MemoryFunnelVO buildMemoryFunnel(Long userId) {
        StatisticsDashboardVO.MemoryFunnelVO funnel = new StatisticsDashboardVO.MemoryFunnelVO();
        List<Map<String, Object>> stageCounts = statisticsMapper.getMemoryFunnelStageCounts(userId);

        int[] counts = new int[6];
        int totalWords = 0;

        for (Map<String, Object> map : stageCounts) {
            int stage = ((Number) map.get("stage")).intValue();
            int count = ((Number) map.get("wordCount")).intValue();
            if (stage >= 1 && stage <= 5) {
                counts[stage] = count;
                totalWords += count;
            }
        }

        funnel.setPhase1Count(counts[1]);
        funnel.setPhase2Count(counts[2]);
        funnel.setPhase3Count(counts[3]);
        funnel.setPhase4Count(counts[4]);
        funnel.setPhase5Count(counts[5]);

        // 计算长期记忆转化率 (Phase 5 占比)
        funnel.setLongTermRetentionRate(totalWords > 0 ? (float) counts[5] / totalWords : 0f);

        return funnel;
    }

    private StatisticsDashboardVO.AiEmotionVO buildAiEmotion(Long userId) {
        StatisticsDashboardVO.AiEmotionVO emotion = new StatisticsDashboardVO.AiEmotionVO();

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6);

        // 获取近7日的学习状态
        LambdaQueryWrapper<UserDailyStats> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserDailyStats::getUserId, userId)
                .between(UserDailyStats::getRecordDate, startDate, endDate)
                .orderByAsc(UserDailyStats::getRecordDate);

        List<UserDailyStats> weeklyStats = userDailyStatsMapper.selectList(queryWrapper);
        List<Float> weeklyTrend = new ArrayList<>();

        // 【修正点2】如果过去7天完全没有任何学习记录，雷达图直接全部归零
        if (weeklyStats == null || weeklyStats.isEmpty()) {
            emotion.setWeeklyTrend(java.util.Arrays.asList(0f, 0f, 0f, 0f, 0f, 0f, 0f));
            emotion.setSleepyCount(0);
            emotion.setUnfocusedCount(0);
            emotion.setAiHardWordsPushed(0);
            emotion.setRadarScores(java.util.Arrays.asList(0f, 0f, 0f, 0f, 0f)); // 全部归零
            return emotion;
        }

        int totalSleepy = 0;
        int totalUnfocused = 0;
        int totalAiHard = 0;

        Map<LocalDate, UserDailyStats> statsMap = weeklyStats.stream()
                .collect(Collectors.toMap(UserDailyStats::getRecordDate, s -> s));

        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);
            UserDailyStats stat = statsMap.get(date);
            if (stat != null) {
                int happy = stat.getHappyMinutes() != null ? stat.getHappyMinutes() : 0;
                int negative = stat.getNegativeMinutes() != null ? stat.getNegativeMinutes() : 0;
                int totalEmotion = happy + negative;
                // 计算单日情绪积极占比
                weeklyTrend.add(totalEmotion > 0 ? (float) happy / totalEmotion : 0f);

                totalSleepy += stat.getSleepyCount() != null ? stat.getSleepyCount() : 0;
                totalUnfocused += stat.getUnfocusedCount() != null ? stat.getUnfocusedCount() : 0;
                totalAiHard += stat.getAiHardWords() != null ? stat.getAiHardWords() : 0;
            } else {
                weeklyTrend.add(0f);
            }
        }

        emotion.setWeeklyTrend(weeklyTrend);
        emotion.setSleepyCount(totalSleepy);
        emotion.setUnfocusedCount(totalUnfocused);
        emotion.setAiHardWordsPushed(totalAiHard);

        // 【修正点3】动态计算雷达图分数
        List<Float> radarScores = new ArrayList<>();
        radarScores.add(Math.max(0f, 1.0f - (totalSleepy + totalUnfocused) * 0.05f)); // 专注力：无异常记录即为满分
        float avgPositivity = (float) weeklyTrend.stream().mapToDouble(Float::doubleValue).average().orElse(0.0);
        radarScores.add(avgPositivity); // 积极性
        radarScores.add(Math.min(1.0f, totalAiHard * 0.02f)); // 抗压力
        radarScores.add(weeklyStats.size() / 7.0f); // 恒心：改为根据近7天有学习记录的天数比例计算
        radarScores.add(0.8f); // 效率：既然有记录，给一个及格基准分

        emotion.setRadarScores(radarScores);
        return emotion;
    }

    private StatisticsDashboardVO.HardWordsMapVO buildHardWordsMap(Long userId) {
        StatisticsDashboardVO.HardWordsMapVO hardWordsMap = new StatisticsDashboardVO.HardWordsMapVO();

        // 1. 提取错词权重并构建词云
        List<Map<String, Object>> topWordsMap = statisticsMapper.getTopHardWords(userId, 7);
        List<StatisticsDashboardVO.HardWordsMapVO.WordCloudItem> cloudItems = new ArrayList<>();
        for (Map<String, Object> map : topWordsMap) {
            StatisticsDashboardVO.HardWordsMapVO.WordCloudItem item = new StatisticsDashboardVO.HardWordsMapVO.WordCloudItem();
            item.setWord((String) map.get("word"));

            // 限制 UI 权重范围为 1-5
            int rawError = ((Number) map.get("weight")).intValue();
            item.setWeight(Math.min(5, Math.max(1, rawError)));
            cloudItems.add(item);
        }
        hardWordsMap.setHardWords(cloudItems);

        // 2. 构建近 36 天热力图层级 (为适配 Compose 视图 3x12 网格)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(35);
        List<Map<String, Object>> heatmapStats = statisticsMapper.getHeatmapStats(userId, startDate);

        Map<LocalDate, Integer> dateReviewMap = new HashMap<>();
        for (Map<String, Object> map : heatmapStats) {
            Object dateObj = map.get("recordDate");
            // 兼容 java.sql.Date 向 LocalDate 的转换
            LocalDate date = dateObj instanceof java.sql.Date ? ((java.sql.Date) dateObj).toLocalDate() : (LocalDate) dateObj;
            int reviewCount = ((Number) map.get("reviewedCount")).intValue();
            dateReviewMap.put(date, reviewCount);
        }

        List<Integer> heatmapLevels = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            LocalDate date = startDate.plusDays(i);
            int count = dateReviewMap.getOrDefault(date, 0);

            // 基于复习量分配热力图活跃层级 0-4
            int level = 0;
            if (count > 0 && count <= 20) level = 1;
            else if (count > 20 && count <= 50) level = 2;
            else if (count > 50 && count <= 80) level = 3;
            else if (count > 80) level = 4;

            heatmapLevels.add(level);
        }
        hardWordsMap.setHeatmapLevels(heatmapLevels);

        return hardWordsMap;
    }
}