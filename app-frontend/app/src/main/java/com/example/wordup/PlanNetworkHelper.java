package com.example.wordup;

import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 学习计划模块独立的网络请求工具类
 */
public class PlanNetworkHelper {

    public interface UpdatePlanCallback {
        void onSuccess();
        void onFailure(String errorMsg);
    }

    /**
     * 发送网络请求以更新用户的每日学习目标
     *
     * @param userId      目标用户 ID
     * @param dailyTarget 计算后的每日待背总目标数
     * @param callback    异步回调接口
     */
    public static void updateDailyTarget(long userId, int dailyTarget, UpdatePlanCallback callback) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(NetworkConfig.UPDATE_DAILY_TARGET_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
                connection.setDoOutput(true);

                // 封装请求体参数
                JSONObject jsonParam = new JSONObject();
                jsonParam.put("userId", userId);
                jsonParam.put("dailyTarget", dailyTarget);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonParam.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    new Handler(Looper.getMainLooper()).post(callback::onSuccess);
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onFailure("接口响应异常，状态码: " + responseCode));
                }
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onFailure("网络请求失败: " + e.getMessage()));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }
}