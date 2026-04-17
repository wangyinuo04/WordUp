package com.example.wordup.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;

/**
 * 单词本地缓存表
 */
@Entity(tableName = "local_word")
public class LocalWord {
    @PrimaryKey
    public Long id;

    @SerializedName("bookId")
    public Long book_id;

    public String spelling;
    public String phonetic;
    public String translation;
    public Integer difficulty;

    @SerializedName("commonMeaning")
    public String common_meaning;

    @SerializedName("csMeaning")
    public String cs_meaning;

    @SerializedName("enExample")
    public String en_example;

    @SerializedName("cnExample")
    public String cn_example;
}