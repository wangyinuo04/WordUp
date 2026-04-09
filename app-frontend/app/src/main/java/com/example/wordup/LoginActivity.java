package com.example.wordup;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.Context;

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
                .url(NetworkConfig.BASE_URL + "/login") // 使用统一配置的 URL
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
                        // 提取下发的关键字段：userId
                        long userId = dataObj.getLong("userId");
                        String avatarUrl = dataObj.optString("avatar_url", "");

                        // 将 userId 与其他配置一同持久化存储
                        LoginActivity.this.getSharedPreferences("MyAppConfig", Context.MODE_PRIVATE)
                                .edit()
                                .putString("user_token", token)
                                .putString("username", username)
                                .putLong("userId", userId) // 存储 userId
                                .putString("avatar_url", avatarUrl)
                                .apply();

                        runOnUiThread(() -> {
                            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                            startActivity(intent);
                            finish();
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
}