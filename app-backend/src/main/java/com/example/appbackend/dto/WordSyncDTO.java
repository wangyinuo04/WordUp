package com.example.appbackend.dto;

public class WordSyncDTO {
    private Long wordId;
    private Integer learnStatus;
    private Integer currentStage;
    private Long nextReviewTime; // 客户端传来的时间戳

    public Long getWordId() { return wordId; }
    public void setWordId(Long wordId) { this.wordId = wordId; }
    public Integer getLearnStatus() { return learnStatus; }
    public void setLearnStatus(Integer learnStatus) { this.learnStatus = learnStatus; }
    public Integer getCurrentStage() { return currentStage; }
    public void setCurrentStage(Integer currentStage) { this.currentStage = currentStage; }
    public Long getNextReviewTime() { return nextReviewTime; }
    public void setNextReviewTime(Long nextReviewTime) { this.nextReviewTime = nextReviewTime; }
}