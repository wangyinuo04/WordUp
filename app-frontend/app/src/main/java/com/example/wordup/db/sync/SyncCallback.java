package com.example.wordup.db.sync;

/**
 * 数据同步状态回调接口
 */
public interface SyncCallback {
    /**
     * 同步成功时触发
     */
    void onSuccess();

    /**
     * 同步失败时触发
     *
     * @param errorMessage 异常原因描述
     */
    void onFailure(String errorMessage);
}