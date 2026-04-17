package com.example.wordup.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.wordup.db.dao.LocalDataDao;
import com.example.wordup.db.entity.LocalUserWordRecord;
import com.example.wordup.db.entity.LocalWord;
import com.example.wordup.db.entity.LocalWordBook;

/**
 * SQLite 数据库全局实例管理类
 */
@Database(entities = {LocalWord.class, LocalWordBook.class, LocalUserWordRecord.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract LocalDataDao localDataDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "wordup_local_database")
                            .fallbackToDestructiveMigration() // 允许破坏性迁移，简化测试期表结构变更
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}