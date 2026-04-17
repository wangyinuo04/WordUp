package com.example.wordup.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;

/**
 * 词书本地缓存表
 */
@Entity(tableName = "local_word_book")
public class LocalWordBook {
    @PrimaryKey
    public Long id;

    @SerializedName("bookName")
    public String book_name;

    @SerializedName("totalWords")
    public Integer total_words;
}