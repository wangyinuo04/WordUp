package com.example.wordup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.wordup.db.sync.DataSyncManager;
import com.example.wordup.db.sync.SyncCallback;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername;
    private EditText etPassword;
    private TextView tvResult;
    private OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView btnRegister = findViewById(R.id.btnRegister);
        tvResult = findViewById(R.id.tvResult);

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString();
            String password = etPassword.getText().toString();
            tvResult.setText("正在连接服务器...");
            loginWithOkHttp(username, password);
        });

        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void loginWithOkHttp(String username, String password) {
        RequestBody formBody = new FormBody.Builder()
                .add("username", username)
                .add("password", password)
                .build();

        Request request = new Request.Builder()
                .url(NetworkConfig.BASE_URL + "/login")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> tvResult.setText("请求失败：" + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String resultText = response.body().string();
                try {
                    JSONObject jsonObject = new JSONObject(resultText);
                    int code = jsonObject.getInt("code");

                    if (code == 200) {
                        JSONObject dataObj = jsonObject.getJSONObject("data");
                        String token = dataObj.getString("token");
                        long userId = dataObj.getLong("userId");
                        String avatarUrl = dataObj.optString("avatar_url", "");

                        LoginActivity.this.getSharedPreferences("MyAppConfig", Context.MODE_PRIVATE)
                                .edit()
                                .putString("user_token", token)
                                .putString("username", username)
                                .putLong("userId", userId)
                                .putString("avatar_url", avatarUrl)
                                .apply();

                        runOnUiThread(() -> {
                            tvResult.setText("正在智能获取您的专属学习计划...");
                            tvResult.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                        });

                        // 【核心重构：暴力绕过 AiNetworkHelper，使用原生 OkHttp 获取真实书号】
                        Request planReq = new Request.Builder()
                                .url(NetworkConfig.GET_AI_SETTINGS_URL + "?userId=" + userId)
                                .get()
                                .build();

                        client.newCall(planReq).enqueue(new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                executeFallbackSync(userId);
                            }

                            @Override
                            public void onResponse(Call call, Response planResponse) throws IOException {
                                long targetBookId = 1L; // 默认保底为 1 号词书
                                try {
                                    String planRes = planResponse.body().string();
                                    JSONObject planJson = new JSONObject(planRes);
                                    if (planJson.getInt("code") == 200) {
                                        JSONObject planData = planJson.getJSONObject("data");
                                        // 暴力破解 JSON 字典，兼容两种命名法
                                        targetBookId = planData.optLong("bookId", planData.optLong("book_id", 1L));
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                final long finalBookId = targetBookId;

                                // 立即修正本地 SharedPreferences
                                getSharedPreferences("MyAppConfig", Context.MODE_PRIVATE)
                                        .edit()
                                        .putLong("current_book_id", finalBookId)
                                        .apply();

                                runOnUiThread(() -> tvResult.setText("正在为您部署专属离线词库..."));

                                // 精准下载该用户的词书及历史进度
                                DataSyncManager.getInstance(LoginActivity.this).fetchCloudDataToLocal(userId, finalBookId, new SyncCallback() {
                                    @Override
                                    public void onSuccess() {
                                        runOnUiThread(() -> {
                                            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                                            startActivity(intent);
                                            finish();
                                        });
                                    }

                                    @Override
                                    public void onFailure(String errorMessage) {
                                        showSyncError(errorMessage);
                                    }
                                });
                            }
                        });

                    } else {
                        String msg = jsonObject.getString("msg");
                        runOnUiThread(() -> tvResult.setText(msg));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> tvResult.setText("数据解析异常：" + resultText));
                }
            }
        });
    }

    private void executeFallbackSync(long userId) {
        long fallbackBookId = getSharedPreferences("MyAppConfig", Context.MODE_PRIVATE).getLong("current_book_id", 1L);
        DataSyncManager.getInstance(LoginActivity.this).fetchCloudDataToLocal(userId, fallbackBookId, new SyncCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                    startActivity(intent);
                    finish();
                });
            }
            @Override
            public void onFailure(String errorMessage) {
                showSyncError(errorMessage);
            }
        });
    }

    private void showSyncError(String errorMessage) {
        runOnUiThread(() -> {
            tvResult.setText("离线同步失败: " + errorMessage + "\n应用可能缺失本地数据。");
            tvResult.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            tvResult.postDelayed(() -> {
                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            }, 2000);
        });
    }
}