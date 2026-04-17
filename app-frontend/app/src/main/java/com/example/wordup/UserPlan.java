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
    // 新增：每日新词目标
    private Integer dailyNewTarget;
    // 新增：每日复习目标
    private Integer dailyReviewTarget;
    // 请确保您的 bookId 属性长这样：
    @com.google.gson.annotations.SerializedName(value = "bookId", alternate = {"book_id"})
    private Long bookId;

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

    public Integer getDailyNewTarget() {
        return dailyNewTarget;
    }

    public void setDailyNewTarget(Integer dailyNewTarget) {
        this.dailyNewTarget = dailyNewTarget;
    }

    public Integer getDailyReviewTarget() {
        return dailyReviewTarget;
    }

    public void setDailyReviewTarget(Integer dailyReviewTarget) {
        this.dailyReviewTarget = dailyReviewTarget;
    }
    // 在类的下方，加上配套的 Getter 和 Setter 方法：
    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }
}