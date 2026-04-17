package com.example.wordup.db.sync;

import android.content.Context;
import android.util.Log;

import com.example.wordup.NetworkConfig;
import com.example.wordup.db.AppDatabase;
import com.example.wordup.db.dao.LocalDataDao;
import com.example.wordup.db.entity.LocalUserWordRecord;
import com.example.wordup.db.entity.LocalWord;
import com.example.wordup.db.entity.LocalWordBook;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 分布式数据库端云同步管理器 (全量闭环版)
 * 职责：负责词书、词库的下行同步，以及历史进度的双向端云同步
 */
public class DataSyncManager {

    private static volatile DataSyncManager instance;
    private final LocalDataDao localDataDao;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final ExecutorService executorService;

    private static final String TAG = "DataSyncManager";

    private DataSyncManager(Context context) {
        this.localDataDao = AppDatabase.getInstance(context).localDataDao();
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
        this.executorService = Executors.newFixedThreadPool(3);
    }

    public static DataSyncManager getInstance(Context context) {
        if (instance == null) {
            synchronized (DataSyncManager.class) {
                if (instance == null) {
                    instance = new DataSyncManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    /**
     * 全量拉取云端词书、词库及用户历史进度，并持久化至本地 SQLite
     * [新增参数 userId 用于拉取专属进度]
     */
    public void fetchCloudDataToLocal(Long userId, Long bookId, SyncCallback callback) {
        executorService.execute(() -> {
            try {
                // 1. 下行同步：获取所有词书列表
                Request bookRequest = new Request.Builder()
                        .url(NetworkConfig.BASE_URL + "/api/book/list")
                        .get()
                        .build();

                try (Response response = httpClient.newCall(bookRequest).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String body = response.body().string();
                        CloudResult<List<LocalWordBook>> bookResult = gson.fromJson(body, new TypeToken<CloudResult<List<LocalWordBook>>>(){}.getType());
                        if (bookResult != null && bookResult.data != null) {
                            localDataDao.insertBooks(bookResult.data);
                        }
                    }
                }

                // 2. 下行同步：拉取全量单词字典
                Request wordsRequest = new Request.Builder()
                        .url(NetworkConfig.BASE_URL + "/api/learning/all?bookId=" + bookId)
                        .get()
                        .build();

                try (Response response = httpClient.newCall(wordsRequest).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String body = response.body().string();
                        CloudResult<List<LocalWord>> wordsResult = gson.fromJson(body, new TypeToken<CloudResult<List<LocalWord>>>(){}.getType());
                        if (wordsResult != null && wordsResult.data != null) {
                            localDataDao.clearAllWords();
                            localDataDao.insertWords(wordsResult.data);
                        }
                    }
                }

                // 3. [新增] 下行同步：拉取用户在该词书下的所有历史复习记录
                Request recordsRequest = new Request.Builder()
                        .url(NetworkConfig.BASE_URL + "/api/learning/records?userId=" + userId + "&bookId=" + bookId)
                        .get()
                        .build();

                try (Response response = httpClient.newCall(recordsRequest).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String body = response.body().string();
                        CloudResult<List<CloudRecord>> recordsResult = gson.fromJson(body, new TypeToken<CloudResult<List<CloudRecord>>>(){}.getType());
                        if (recordsResult != null && recordsResult.data != null) {
                            List<LocalUserWordRecord> localRecords = new ArrayList<>();
                            // 兼容 Spring Boot LocalDateTime 的标准 ISO 反序列化格式
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());

                            for (CloudRecord cr : recordsResult.data) {
                                LocalUserWordRecord lr = new LocalUserWordRecord();
                                lr.user_id = cr.userId;
                                lr.word_id = cr.wordId;
                                lr.learn_status = cr.learnStatus;
                                lr.current_stage = cr.currentStage;
                                lr.sync_status = 0; // 从云端拉取的数据，本地标记为已同步

                                if (cr.nextReviewTime != null) {
                                    try {
                                        Date date = sdf.parse(cr.nextReviewTime);
                                        lr.next_review_time = date != null ? date.getTime() : System.currentTimeMillis();
                                    } catch (Exception e) {
                                        try {
                                            lr.next_review_time = Long.parseLong(cr.nextReviewTime);
                                        } catch (Exception ex) {
                                            lr.next_review_time = System.currentTimeMillis();
                                        }
                                    }
                                } else {
                                    lr.next_review_time = System.currentTimeMillis();
                                }
                                localRecords.add(lr);
                            }
                            localDataDao.insertRecords(localRecords);
                        }
                    }
                }

                if (callback != null) callback.onSuccess();

            } catch (Exception e) {
                Log.e(TAG, "全量数据下行同步异常: " + e.getMessage());
                if (callback != null) callback.onFailure(e.getMessage());
            }
        });
    }

    /**
     * 上行同步：批量上传本地离线产生的待同步进度至云端
     */
    public void pushLocalProgressToCloud(Long userId) {
        executorService.execute(() -> {
            try {
                List<LocalUserWordRecord> pendingRecords = localDataDao.getPendingSyncRecords(userId);
                if (pendingRecords == null || pendingRecords.isEmpty()) return;

                List<SyncPayload> payloads = new ArrayList<>();
                for (LocalUserWordRecord record : pendingRecords) {
                    SyncPayload payload = new SyncPayload();
                    payload.wordId = record.word_id;
                    payload.learnStatus = record.learn_status;
                    payload.currentStage = record.current_stage;
                    payload.nextReviewTime = record.next_review_time;
                    payloads.add(payload);
                }

                String jsonPayload = gson.toJson(payloads);
                RequestBody body = RequestBody.create(jsonPayload, MediaType.parse("application/json; charset=utf-8"));

                Request pushRequest = new Request.Builder()
                        .url(NetworkConfig.BASE_URL + "/api/learning/batch-sync?userId=" + userId)
                        .post(body)
                        .build();

                try (Response response = httpClient.newCall(pushRequest).execute()) {
                    if (response.isSuccessful()) {
                        List<Long> syncedIds = new ArrayList<>();
                        for (LocalUserWordRecord record : pendingRecords) syncedIds.add(record.id);
                        localDataDao.markRecordsAsSynced(syncedIds);
                        Log.i(TAG, "本地进度增量同步至云端完成，条数: " + syncedIds.size());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "本地进度上行同步异常: " + e.getMessage());
            }
        });
    }

    // 后端通用 Result 结构映射
    private static class CloudResult<T> {
        public int code;
        public String message;
        public T data;
    }

    // 下行进度接收映射实体
    private static class CloudRecord {
        public Long userId;
        public Long wordId;
        public Integer learnStatus;
        public Integer currentStage;
        public String nextReviewTime;
    }

    // 上行进度发送映射实体
    private static class SyncPayload {
        public Long wordId;
        public Integer learnStatus;
        public Integer currentStage;
        public Long nextReviewTime;
    }
}