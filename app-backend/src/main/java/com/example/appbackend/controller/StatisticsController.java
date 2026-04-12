package com.example.appbackend.controller;

import com.example.appbackend.Result;
import com.example.appbackend.dto.AiStatsUpdateDTO;
import com.example.appbackend.service.StatisticsService;
import com.example.appbackend.vo.StatisticsDashboardVO;
import org.springframework.web.bind.annotation.*;

/**
 * 统计模块前端控制器
 */
@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    // 构造器注入 Service
    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    /**
     * 获取用户统计看板全量数据
     * @param userId 用户唯一标识
     * @return 统一封装的统计视图对象
     */
    @GetMapping("/dashboard")
    public Result<StatisticsDashboardVO> getDashboardStats(@RequestParam("userId") Long userId) {
        // 调用 Service 获取复杂聚合计算后的数据
        StatisticsDashboardVO dashboardData = statisticsService.getDashboardStats(userId);
        // 包装为标准的统一响应体返回
        return Result.success(dashboardData);
    }

    /**
     * 批量更新 AI 专注度与情绪相关数据
     * @param dto 包含本地暂存累加数据的对象
     * @return 统一封装的响应结果
     */
    @PostMapping("/updateAiStats")
    public Result<Void> updateAiStats(@RequestBody AiStatsUpdateDTO dto) {
        statisticsService.updateAiStats(dto);
        return Result.success(null);
    }
}