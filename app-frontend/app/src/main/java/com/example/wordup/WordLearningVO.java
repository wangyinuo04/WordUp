package com.example.wordup;

/**
 * 单词学习视图对象 (映射后端返回的 JSON 数据)
 */
public class WordLearningVO {
    private Long wordId;
    private String spelling;
    private String phonetic;
    private String translation;

    // 从数据库直出的单词难度
    private Integer difficulty;

    private Long recordId;
    private Integer learnStatus;
    private Boolean isTempOld;
    private Integer consecutiveKnownCount;
    private Integer currentStage;

    public Long getWordId() { return wordId; }
    public void setWordId(Long wordId) { this.wordId = wordId; }

    public String getSpelling() { return spelling; }
    public void setSpelling(String spelling) { this.spelling = spelling; }

    public String getPhonetic() { return phonetic; }
    public void setPhonetic(String phonetic) { this.phonetic = phonetic; }

    public String getTranslation() { return translation; }
    public void setTranslation(String translation) { this.translation = translation; }

    public Integer getDifficulty() { return difficulty; }
    public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }

    public Integer getLearnStatus() { return learnStatus; }
    public void setLearnStatus(Integer learnStatus) { this.learnStatus = learnStatus; }

    public Boolean getIsTempOld() { return isTempOld; }
    public void setIsTempOld(Boolean tempOld) { isTempOld = tempOld; }

    public Integer getConsecutiveKnownCount() { return consecutiveKnownCount; }
    public void setConsecutiveKnownCount(Integer consecutiveKnownCount) { this.consecutiveKnownCount = consecutiveKnownCount; }

    public Integer getCurrentStage() { return currentStage; }
    public void setCurrentStage(Integer currentStage) { this.currentStage = currentStage; }
}