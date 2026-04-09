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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 词书专属网络请求工具类
 */
public class BookNetworkHelper {
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface GetBookListCallback {
        void onSuccess(List<Book> bookList);
        void onFailure(String errorMsg);
    }

    public interface GetCurrentBookCallback {
        void onSuccess(String bookName);
        void onFailure(String errorMsg);
    }

    public interface UpdateBookCallback {
        void onSuccess();
        void onFailure(String errorMsg);
    }

    /**
     * 获取词书列表
     */
    public static void getBookList(GetBookListCallback callback) {
        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(NetworkConfig.GET_BOOK_LIST_URL);
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
                        List<Book> bookList = new ArrayList<>();
                        JSONArray dataArray = jsonObject.optJSONArray("data");
                        if (dataArray != null) {
                            for (int i = 0; i < dataArray.length(); i++) {
                                JSONObject bookObj = dataArray.getJSONObject(i);
                                Long id = bookObj.optLong("id");
                                String name = bookObj.optString("bookName");
                                int totalWords = bookObj.optInt("totalWords");
                                // 进度可在此处扩展，当前默认为0
                                bookList.add(new Book(id, name, totalWords, 0));
                            }
                        }
                        mainHandler.post(() -> callback.onSuccess(bookList));
                    } else {
                        String msg = jsonObject.optString("msg", "获取列表失败");
                        mainHandler.post(() -> callback.onFailure(msg));
                    }
                } else {
                    mainHandler.post(() -> callback.onFailure("网络请求失败，状态码：" + responseCode));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onFailure("请求异常：" + e.getMessage()));
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    /**
     * 获取当前学习词书名称
     */
    public static void getCurrentBook(Long userId, GetCurrentBookCallback callback) {
        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(NetworkConfig.GET_CURRENT_BOOK_URL + "?userId=" + userId);
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
                        String bookName = jsonObject.optString("data");
                        mainHandler.post(() -> callback.onSuccess(bookName));
                    } else {
                        String msg = jsonObject.optString("msg", "获取当前词书失败");
                        mainHandler.post(() -> callback.onFailure(msg));
                    }
                } else {
                    mainHandler.post(() -> callback.onFailure("网络请求失败，状态码：" + responseCode));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onFailure("请求异常：" + e.getMessage()));
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    /**
     * 更新用户当前词书
     */
    public static void updateCurrentBook(Long userId, Long bookId, UpdateBookCallback callback) {
        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(NetworkConfig.UPDATE_BOOK_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
                connection.setDoOutput(true);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                JSONObject requestBody = new JSONObject();
                requestBody.put("userId", userId);
                requestBody.put("bookId", bookId);

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
                        String msg = jsonObject.optString("msg", "更新失败");
                        mainHandler.post(() -> callback.onFailure(msg));
                    }
                } else {
                    mainHandler.post(() -> callback.onFailure("网络请求失败，状态码：" + responseCode));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onFailure("请求异常：" + e.getMessage()));
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }
}