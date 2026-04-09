package com.example.wordup;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * 背单词核心模块独立的网络请求工具类
 */
public class WordLearningNetworkHelper {

    // 回调接口新增 offset 参数
    public interface GetBatchCallback {
        void onSuccess(List<WordLearningVO> wordList, int offset);
        void onFailure(String errorMsg);
    }

    public interface SubmitActionCallback {
        void onSuccess();
        void onFailure(String errorMsg);
    }

    public static void getWordBatch(long userId, long bookId, int batchSize, boolean isReview, GetBatchCallback callback) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                String urlString = NetworkConfig.GET_WORD_BATCH_URL +
                        "?userId=" + userId +
                        "&bookId=" + bookId +
                        "&batchSize=" + batchSize +
                        "&isReview=" + isReview;

                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    if (jsonResponse.getInt("code") == 200) {
                        // 核心修改：解析嵌套的复合数据结构
                        JSONObject dataObj = jsonResponse.getJSONObject("data");
                        JSONArray dataArray = dataObj.getJSONArray("words");
                        int offset = dataObj.getInt("offset");

                        List<WordLearningVO> wordList = new ArrayList<>();
                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject obj = dataArray.getJSONObject(i);
                            WordLearningVO vo = new WordLearningVO();
                            vo.setWordId(obj.getLong("wordId"));
                            vo.setSpelling(obj.getString("spelling"));
                            vo.setPhonetic(obj.isNull("phonetic") ? "" : obj.getString("phonetic"));
                            vo.setTranslation(obj.getString("translation"));
                            vo.setDifficulty(obj.optInt("difficulty", 1));
                            vo.setLearnStatus(obj.getInt("learnStatus"));
                            vo.setIsTempOld(obj.getBoolean("isTempOld"));
                            vo.setConsecutiveKnownCount(obj.getInt("consecutiveKnownCount"));
                            vo.setCurrentStage(obj.getInt("currentStage"));
                            wordList.add(vo);
                        }
                        new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(wordList, offset));
                    } else {
                        String msg = jsonResponse.optString("msg", "未知错误");
                        new Handler(Looper.getMainLooper()).post(() -> callback.onFailure(msg));
                    }
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onFailure("接口响应异常，状态码: " + responseCode));
                }
            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> callback.onFailure("网络请求失败: " + e.getMessage()));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    public static void submitWordAction(long userId, long wordId, boolean isKnown, SubmitActionCallback callback) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(NetworkConfig.SUBMIT_WORD_ACTION_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
                connection.setDoOutput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("userId", userId);
                jsonParam.put("wordId", wordId);
                jsonParam.put("isKnown", isKnown);

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
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> callback.onFailure("网络请求失败: " + e.getMessage()));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }
}