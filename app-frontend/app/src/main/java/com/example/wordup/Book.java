package com.example.wordup;

public class Book {
    private String bookName;
    private int totalWords;
    private int progress; // 进度百分比

    public Book(String bookName, int totalWords, int progress) {
        this.bookName = bookName;
        this.totalWords = totalWords;
        this.progress = progress;
    }

    // Getter 方法
    public String getBookName() { return bookName; }
    public int getTotalWords() { return totalWords; }
    public int getProgress() { return progress; }
}
