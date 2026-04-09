package com.example.wordup;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI 专属网络请求工具类
 */
public class AiNetworkHelper {
    // 单线程池用于处理后台网络请求
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    // 用于将回调发送回主线程，方便直接更新 UI
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface GetSettingsCallback {
        void onSuccess(UserPlan plan);
        void onFailure(String errorMsg);
    }

    public interface UpdateSettingsCallback {
        void onSuccess();
        void onFailure(String errorMsg);
    }

    /**
     * 获取 AI 设置与用户学习计划数据
     */
    public static void getAiSettings(Long userId, GetSettingsCallback callback) {
        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(NetworkConfig.GET_AI_SETTINGS_URL + "?userId=" + userId);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject jsonObject = new JSONObject(response.toString());
                    int code = jsonObject.optInt("code");
                    if (code == 200) {
                        JSONObject data = jsonObject.optJSONObject("data");
                        if (data != null) {
                            UserPlan plan = new UserPlan();
                            plan.setUserId(data.optLong("userId"));
                            plan.setAntiSleepOn(data.optInt("antiSleepOn"));
                            plan.setAiSentenceOn(data.optInt("aiSentenceOn"));
                            plan.setEmotionRecogOn(data.optInt("emotionRecogOn"));

                            // 【修复核心】补充对 dailyTarget 的解析，设置默认值150以防异常
                            plan.setDailyTarget(data.optInt("dailyTarget", 150));

                            mainHandler.post(() -> callback.onSuccess(plan));
                            return;
                        }
                    }
                    String msg = jsonObject.optString("msg", "获取配置失败");
                    mainHandler.post(() -> callback.onFailure(msg));
                } else {
                    mainHandler.post(() -> callback.onFailure("网络请求失败，状态码：" + responseCode));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onFailure("请求异常：" + e.getMessage()));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    /**
     * 更新 AI 设置
     */
    public static void updateAiSettings(UserPlan plan, UpdateSettingsCallback callback) {
        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(NetworkConfig.UPDATE_AI_SETTINGS_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
                connection.setDoOutput(true);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                JSONObject requestBody = new JSONObject();
                requestBody.put("userId", plan.getUserId());
                requestBody.put("antiSleepOn", plan.getAntiSleepOn());
                requestBody.put("aiSentenceOn", plan.getAiSentenceOn());
                requestBody.put("emotionRecogOn", plan.getEmotionRecogOn());
                // 注：此处无需提交 dailyTarget，因为该接口在后端仅做 AI 设置的独立更新

                OutputStream os = connection.getOutputStream();
                os.write(requestBody.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject jsonObject = new JSONObject(response.toString());
                    int code = jsonObject.optInt("code");
                    if (code == 200) {
                        mainHandler.post(callback::onSuccess);
                    } else {
                        String msg = jsonObject.optString("msg", "更新配置失败");
                        mainHandler.post(() -> callback.onFailure(msg));
                    }
                } else {
                    mainHandler.post(() -> callback.onFailure("网络请求失败，状态码：" + responseCode));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onFailure("请求异常：" + e.getMessage()));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }
}