package com.example.wordup;

import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
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
     * 学习进度获取回调接口
     */
    public interface FetchProgressCallback {
        void onSuccess(int learnedCount, int totalCount, double progressPercentage);
        void onFailure(String errorMsg);
    }

    /**
     * 发送网络请求以更新用户的每日学习目标
     *
     * @param userId      目标用户 ID
     * @param dailyTarget 计算后的每日待背总目标数
     * @param newCount    每日新词目标数
     * @param reviewCount 每日复习目标数
     * @param callback    异步回调接口
     */
    public static void updateDailyTarget(long userId, int dailyTarget, int newCount, int reviewCount, UpdatePlanCallback callback) {
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
                // 追加拆分后的独立目标配额
                jsonParam.put("dailyNewTarget", newCount);
                jsonParam.put("dailyReviewTarget", reviewCount);

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

    /**
     * 【重构】发送 GET 请求以获取用户的真实词书学习进度
     * 移除前端传递 bookId，改为仅通过 userId 触发后端溯源
     *
     * @param userId   目标用户 ID
     * @param callback 异步回调接口
     */
    public static void getStudyProgress(long userId, FetchProgressCallback callback) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                // 拼接 GET 请求参数，不再携带 bookId
                String urlString = NetworkConfig.GET_STUDY_PROGRESS_URL + "?userId=" + userId;
                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream is = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    int code = jsonResponse.optInt("code");
                    if (code == 200) {
                        JSONObject data = jsonResponse.optJSONObject("data");
                        if (data != null) {
                            int learnedCount = data.optInt("learnedCount", 0);
                            int totalCount = data.optInt("totalCount", 0);
                            double progressPercentage = data.optDouble("progressPercentage", 0.0);
                            new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(learnedCount, totalCount, progressPercentage));
                        } else {
                            new Handler(Looper.getMainLooper()).post(() -> callback.onFailure("返回数据结构异常"));
                        }
                    } else {
                        String msg = jsonResponse.optString("msg", "获取进度失败");
                        new Handler(Looper.getMainLooper()).post(() -> callback.onFailure(msg));
                    }
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