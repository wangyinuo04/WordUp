package com.example.wordup.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.wordup.db.entity.LocalUserWordRecord;
import com.example.wordup.db.entity.LocalWord;
import com.example.wordup.db.entity.LocalWordBook;

import java.util.List;

/**
 * 本地数据库访问对象接口
 */
@Dao
public interface LocalDataDao {

    // --- 词库操作 ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertWords(List<LocalWord> words);

    @Query("SELECT * FROM local_word WHERE book_id = :bookId")
    List<LocalWord> getWordsByBookId(Long bookId);

    @Query("DELETE FROM local_word")
    void clearAllWords();

    // --- 词书操作 ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertBooks(List<LocalWordBook> books);

    @Query("SELECT * FROM local_word_book")
    List<LocalWordBook> getAllBooks();

    // --- 复习记录操作 ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRecords(List<LocalUserWordRecord> records);

    @Update
    void updateRecord(LocalUserWordRecord record);

    @Query("SELECT * FROM local_user_word_record WHERE user_id = :userId AND sync_status = 1")
    List<LocalUserWordRecord> getPendingSyncRecords(Long userId);

    @Query("UPDATE local_user_word_record SET sync_status = 0 WHERE id IN (:recordIds)")
    void markRecordsAsSynced(List<Long> recordIds);

    // ================== [本次新增] ==================
    @Query("SELECT * FROM local_user_word_record WHERE user_id = :userId")
    List<LocalUserWordRecord> getUserRecords(Long userId);

    @Query("SELECT * FROM local_user_word_record WHERE user_id = :userId AND word_id = :wordId")
    LocalUserWordRecord getUserRecord(Long userId, Long wordId);
}