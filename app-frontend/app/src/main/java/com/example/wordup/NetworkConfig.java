package com.example.wordup;

public class NetworkConfig {
    /**
     * 🌐 云端生产环境（Railway 部署）
     * 如果需要在本地模拟器测试，可以临时改为 "http://10.0.2.2:8080"
     * 但打包前务必改回 Railway 地址。
     */
    public static final String BASE_URL = "https://wordup-production.up.railway.app";

    // 拼接具体的接口路径
    public static final String UPDATE_PROFILE_URL = BASE_URL + "/updateProfile";
    public static final String UPLOAD_AVATAR_URL = BASE_URL + "/uploadAvatar";

    // AI 设置相关接口
    public static final String GET_AI_SETTINGS_URL = BASE_URL + "/api/plan/ai-settings";
    public static final String UPDATE_AI_SETTINGS_URL = BASE_URL + "/api/plan/ai-settings/update";

    // 词书相关接口
    public static final String GET_BOOK_LIST_URL = BASE_URL + "/api/book/list";
    public static final String GET_CURRENT_BOOK_URL = BASE_URL + "/api/book/current";
    public static final String UPDATE_BOOK_URL = BASE_URL + "/api/plan/book/update";

    // 学习计划相关接口
    public static final String UPDATE_DAILY_TARGET_URL = BASE_URL + "/api/plan/daily-target/update";

    // 新增：获取词书学习进度接口
    public static final String GET_STUDY_PROGRESS_URL = BASE_URL + "/api/plan/progress";

    // ==========================================
    // 新增：背单词核心调度算法接口
    // ==========================================
    public static final String GET_WORD_BATCH_URL = BASE_URL + "/api/learning/batch";
    public static final String SUBMIT_WORD_ACTION_URL = BASE_URL + "/api/learning/action";

    // ==========================================
    // 新增：AI 专注度与情绪数据聚合上报接口
    // ==========================================
    public static final String UPDATE_AI_STATS_URL = BASE_URL + "/api/statistics/updateAiStats";
}