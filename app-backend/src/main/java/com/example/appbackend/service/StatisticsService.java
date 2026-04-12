package com.example.appbackend.service;

import com.example.appbackend.dto.AiStatsUpdateDTO;
import com.example.appbackend.vo.StatisticsDashboardVO;

/**
 * 统计模块业务逻辑接口
 */
public interface StatisticsService {

    /**
     * 获取指定用户的全量统计看板数据
     * @param userId 用户ID
     * @return 组装完毕的视图对象
     */
    StatisticsDashboardVO getDashboardStats(Long userId);

    /**
     * 更新用户当日的 AI 专注度与情绪统计数据
     * @param dto 包含累加数据的传输对象
     */
    void updateAiStats(AiStatsUpdateDTO dto);
}