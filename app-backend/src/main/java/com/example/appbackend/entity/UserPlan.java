package com.example.appbackend.entity;

import java.time.LocalDateTime;

/**
 * 用户学习计划与设置表实体类
 */
public class UserPlan {
    private Long id;
    private Long userId;
    private Long bookId;
    private Integer dailyTarget;
    private Integer antiSleepOn;
    private Integer aiSentenceOn;
    private Integer emotionRecogOn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public Integer getDailyTarget() {
        return dailyTarget;
    }

    public void setDailyTarget(Integer dailyTarget) {
        this.dailyTarget = dailyTarget;
    }

    public Integer getAntiSleepOn() {
        return antiSleepOn;
    }

    public void setAntiSleepOn(Integer antiSleepOn) {
        this.antiSleepOn = antiSleepOn;
    }

    public Integer getAiSentenceOn() {
        return aiSentenceOn;
    }

    public void setAiSentenceOn(Integer aiSentenceOn) {
        this.aiSentenceOn = aiSentenceOn;
    }

    public Integer getEmotionRecogOn() {
        return emotionRecogOn;
    }

    public void setEmotionRecogOn(Integer emotionRecogOn) {
        this.emotionRecogOn = emotionRecogOn;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}