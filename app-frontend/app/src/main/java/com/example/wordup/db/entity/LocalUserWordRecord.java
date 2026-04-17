package com.example.wordup.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 用户单词复习记录本地表 (用于离线进度追踪与云端同步)
 */
@Entity(tableName = "local_user_word_record")
public class LocalUserWordRecord {
    // 增加 autoGenerate = true，允许本地无网状态下自动生成主键
    @PrimaryKey(autoGenerate = true)
    public Long id;
    public Long user_id;
    public Long word_id;
    public Integer learn_status;
    public Integer current_stage;
    public Long next_review_time; // 存储时间戳
    public Integer sync_status;   // 0: 已同步, 1: 待同步 (仅用于本地标记)
}