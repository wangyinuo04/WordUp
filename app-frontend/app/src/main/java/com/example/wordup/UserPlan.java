package com.example.wordup;

/**
 * 用户学习计划与 AI 设置前端实体类
 */
public class UserPlan {
    private Long userId;
    private Integer dailyTarget;
    private Integer antiSleepOn;
    private Integer aiSentenceOn;
    private Integer emotionRecogOn;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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
}