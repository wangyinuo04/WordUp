package com.example.appbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日学习数据聚合表实体类
 */
@Data
@TableName("user_daily_stats")
public class UserDailyStats {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate recordDate;
    private Integer learnedCount;
    private Integer reviewedCount;
    private Integer studyMinutes;
    private Integer sleepyCount;
    private Integer unfocusedCount;
    private Integer happyMinutes;
    private Integer negativeMinutes;
    private Integer aiHardWords;
    private Boolean hardPushed;
    private Boolean easyPushed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}