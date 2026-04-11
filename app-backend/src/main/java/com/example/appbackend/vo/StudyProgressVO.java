package com.example.appbackend.vo;

import lombok.Data;

/**
 * 学习进度视图对象
 * 用于封装指定用户在特定词书下的学习进度统计信息
 */
@Data
public class StudyProgressVO {
    /**
     * 词书ID
     */
    private Long bookId;

    /**
     * 该词书中已存在于用户学习记录中的单词数量
     */
    private Integer learnedCount;

    /**
     * 该词书的总单词数量
     */
    private Integer totalCount;

    /**
     * 当前学习进度百分比数值 (例如: 45.5 表示 45.5%)
     */
    private Double progressPercentage;
}