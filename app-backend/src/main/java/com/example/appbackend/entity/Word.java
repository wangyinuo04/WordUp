package com.example.appbackend.entity;

import java.time.LocalDateTime;

/**
 * 单词字典表实体类
 */
public class Word {
    private Long id;
    private Long bookId;
    private String spelling;
    private String phonetic;
    private String translation;
    private Integer difficulty;
    private String commonMeaning;
    private String csMeaning;
    private String enExample;
    private String cnExample;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    public String getSpelling() { return spelling; }
    public void setSpelling(String spelling) { this.spelling = spelling; }
    public String getPhonetic() { return phonetic; }
    public void setPhonetic(String phonetic) { this.phonetic = phonetic; }
    public String getTranslation() { return translation; }
    public void setTranslation(String translation) { this.translation = translation; }
    public Integer getDifficulty() { return difficulty; }
    public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }
    public String getCommonMeaning() { return commonMeaning; }
    public void setCommonMeaning(String commonMeaning) { this.commonMeaning = commonMeaning; }
    public String getCsMeaning() { return csMeaning; }
    public void setCsMeaning(String csMeaning) { this.csMeaning = csMeaning; }
    public String getEnExample() { return enExample; }
    public void setEnExample(String enExample) { this.enExample = enExample; }
    public String getCnExample() { return cnExample; }
    public void setCnExample(String cnExample) { this.cnExample = cnExample; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}