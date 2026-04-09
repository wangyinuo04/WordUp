package com.example.appbackend.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 单词学习视图对象 (联合 word 表与 user_word_record 表的数据)
 */
@Data
public class WordLearningVO {
    // 基础单词信息 (来自 word 表)
    private Long wordId;
    private String spelling;
    private String phonetic;
    private String translation;
    private Integer difficulty; // 单词固定难度属性

    // 记忆记录信息 (来自 user_word_record 表，若为全新词则这部分部分字段为初始值)
    private Long recordId;
    private Integer learnStatus;
    private Boolean isTempOld;
    private Integer consecutiveKnownCount;
    private Integer currentStage;
    private LocalDateTime nextReviewTime;
}