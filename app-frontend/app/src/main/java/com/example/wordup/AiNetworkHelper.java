package com.example.wordup;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
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
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static final String LLM_API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
    // TODO: 请在此处填入您申请的真实 API Key
    private static final String LLM_API_KEY = "247f45d80d174a7e96842bd3558174bc.MfEWzj3GSu0vJHd9";

    // 终极优化版提示词：赋予教研专家人设，强制要求语义逻辑通顺、富有画面感，拒绝生硬拼凑
    private static final String SYSTEM_PROMPT = "你是一个专业的高级英语教研专家，专门用于生成高质量的英语学习例句。请使用用户提供的【所有】单词，构建一个语法正确、逻辑通顺、符合日常真实交流语境的英文句子，并提供准确流畅的中文释义。\n" +
            "【核心生成原则】\n" +
            "1. 拒绝生硬拼凑！句子必须具有真实的意义和强烈的画面感，方便用户进行单词的关联联想记忆。如果给定的单词看似不相关，请发挥创造力，将它们巧妙地融入同一个自然的语境中。\n" +
            "2. 必须包含用户输入的每一个核心单词。\n" +
            "【强制输出规范】\n" +
            "1. 必须且只能在一行内输出全部内容，绝对禁止使用任何换行符（\\n）。\n" +
            "2. 格式必须严格遵循：<英文句子> <中文释义>\n" +
            "3. 绝对禁止输出任何多余的解释、问候语或标点符号之外的字符。\n" +
            "【正确输出示例】\n" +
            "用户输入：please, how, only\n" +
            "输出结果：Please tell me how to get the only ticket left. 请告诉我如何获得剩下唯一的一张票。";

    public interface GetSettingsCallback {
        void onSuccess(UserPlan plan);
        void onFailure(String errorMsg);
    }

    public interface UpdateSettingsCallback {
        void onSuccess();
        void onFailure(String errorMsg);
    }

    public interface GenerateSentenceCallback {
        void onSuccess(String sentence);
        void onFailure(String errorMsg);
    }

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
                            plan.setDailyTarget(data.optInt("dailyTarget", 150));

                            // 解析新增的每日新词与复习配额字段，并进行空值安全校验
                            if (!data.isNull("dailyNewTarget")) {
                                plan.setDailyNewTarget(data.optInt("dailyNewTarget"));
                            }
                            if (!data.isNull("dailyReviewTarget")) {
                                plan.setDailyReviewTarget(data.optInt("dailyReviewTarget"));
                            }

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

    public static void generateSentenceWithAI(String words, GenerateSentenceCallback callback) {
        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(LLM_API_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
                connection.setRequestProperty("Authorization", "Bearer " + LLM_API_KEY);
                connection.setDoOutput(true);
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(30000);

                JSONObject requestBody = new JSONObject();
                requestBody.put("model", "glm-4-flash");
                // 【核心修改】将温度值提升至 0.5，释放大模型的创造力，使其能生成符合真实语境的自然句子
                requestBody.put("temperature", 0.5);

                JSONArray messages = new JSONArray();

                JSONObject systemMessage = new JSONObject();
                systemMessage.put("role", "system");
                systemMessage.put("content", SYSTEM_PROMPT);
                messages.put(systemMessage);

                JSONObject userMessage = new JSONObject();
                userMessage.put("role", "user");
                userMessage.put("content", words);
                messages.put(userMessage);

                requestBody.put("messages", messages);

                OutputStream os = connection.getOutputStream();
                os.write(requestBody.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONArray choices = jsonResponse.optJSONArray("choices");
                    if (choices != null && choices.length() > 0) {
                        JSONObject firstChoice = choices.optJSONObject(0);
                        JSONObject message = firstChoice.optJSONObject("message");
                        if (message != null) {
                            String resultContent = message.optString("content", "").replace("\n", "").trim();
                            mainHandler.post(() -> callback.onSuccess(resultContent));
                            return;
                        }
                    }
                    mainHandler.post(() -> callback.onFailure("模型返回数据解析失败"));
                } else {
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "UTF-8"));
                    StringBuilder errorResponse = new StringBuilder();
                    String errorLine;
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                    errorReader.close();
                    Log.e("AiNetworkHelper", "大模型请求失败: " + errorResponse.toString());
                    mainHandler.post(() -> callback.onFailure("请求失败，错误码：" + responseCode));
                }
            } catch (Exception e) {
                Log.e("AiNetworkHelper", "大模型请求异常", e);
                mainHandler.post(() -> callback.onFailure("请求发生异常：" + e.getMessage()));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }
}