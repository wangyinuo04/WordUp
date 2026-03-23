package com.example.appbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("user_daily_stats") // 告诉 MyBatis-Plus 对应哪张数据库表
public class UserDailyStats {

    @TableId(type = IdType.AUTO) // 主键且自增
    private Long id;

    // --- 核心关联字段 ---
    private Integer userId; // 关联 User 表的 id (注意类型保持为 Integer)

    private LocalDate recordDate; // 记录日期 (精确到天，例如 2024-03-16)

    // --- 统计数据字段 ---
    private Integer learnedCount; // 当日新学单词数
    private Integer reviewedCount; // 当日复习单词数
    private Integer studyMinutes; // 当日学习时长(分钟)

    // --- 审计时间字段 ---
    private LocalDateTime createdAt; // 创建时间
    private LocalDateTime updatedAt; // 更新时间
}