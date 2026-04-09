package com.example.wordup;

public class NetworkConfig {
    /**
     * 10.0.2.2 是安卓模拟器访问电脑主机的标准地址。
     * 如果连不上，请临时改为你电脑的真 IP (例如 "http://192.168.198.168:8080")。
     * 提交给队友前记得改回 10.0.2.2。
     */
    public static final String BASE_URL = "http://192.168.198.168:8080";

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

    // ==========================================
    // 新增：背单词核心调度算法接口
    // ==========================================
    public static final String GET_WORD_BATCH_URL = BASE_URL + "/api/learning/batch";
    public static final String SUBMIT_WORD_ACTION_URL = BASE_URL + "/api/learning/action";
}