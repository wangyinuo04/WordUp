package com.example.appbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户单词记忆记录表实体类
 */
@Data
@TableName("user_word_record")
public class UserWordRecord {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 关联单词ID
     */
    private Long wordId;

    /**
     * 总体状态: 0未学(新词), 1学习中(复习词), 2已掌握
     */
    private Integer learnStatus;

    /**
     * 微观调度：是否为临时旧词 (true:是, 享有批次内最高推送优先级)
     */
    private Boolean isTempOld;

    /**
     * 微观调度：临时旧词连续点击认识的次数 (满3次归零并解除 isTempOld)
     */
    private Integer consecutiveKnownCount;

    /**
     * 历史背错总次数(用于判断顽固难词)
     */
    private Integer errorCount;

    /**
     * 是否被系统或用户标记为难词: false否, true是
     */
    private Boolean isHardMarked;

    /**
     * 宏观调度：当前复习阶段(1-5，仅决定复习跨度天数)
     */
    private Integer currentStage;

    /**
     * 最近一次分配批次的日期
     */
    private LocalDate lastBatchDate;

    /**
     * 宏观调度：下次复习时间 (仅决定该单词在哪一天进入复习候选池)
     */
    private LocalDateTime nextReviewTime;

    /**
     * 首次学习创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 最后一次更新时间
     */
    private LocalDateTime updatedAt;
}