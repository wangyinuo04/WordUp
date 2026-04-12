package com.example.appbackend.dto;

import lombok.Data;

/**
 * AI 专注度与情绪数据更新传输对象
 */
@Data
public class AiStatsUpdateDTO {
    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 瞌睡触发次数 (前端本次周期内累加值)
     */
    private Integer sleepyCount;

    /**
     * 偏离镜头次数 (前端本次周期内累加值)
     */
    private Integer unfocusedCount;

    /**
     * AI情绪调度推送的难词数 (前端本次周期内累加值)
     */
    private Integer aiHardWords;
}