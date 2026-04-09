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
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long wordId;
    private Integer learnStatus;
    private Integer errorCount;
    private Boolean isHardMarked;
    private Integer currentStage;
    private LocalDate lastBatchDate;
    private LocalDateTime nextReviewTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}