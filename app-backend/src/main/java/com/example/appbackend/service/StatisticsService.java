package com.example.appbackend.service;

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
}