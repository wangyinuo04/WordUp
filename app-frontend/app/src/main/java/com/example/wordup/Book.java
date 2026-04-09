package com.example.wordup;

/**
 * 词书实体类
 */
public class Book {
    private Long id; // 新增字段，用于后端绑定
    private String bookName;
    private int totalWords;
    private int progress; // 进度百分比

    public Book(Long id, String bookName, int totalWords, int progress) {
        this.id = id;
        this.bookName = bookName;
        this.totalWords = totalWords;
        this.progress = progress;
    }

    // Getter 方法
    public Long getId() { return id; }
    public String getBookName() { return bookName; }
    public int getTotalWords() { return totalWords; }
    public int getProgress() { return progress; }
}