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

        // 绑定界面控件
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView btnRegister = findViewById(R.id.btnRegister);
        tvResult = findViewById(R.id.tvResult);

        // 设置登录按钮点击事件
        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString();
            String password = etPassword.getText().toString();
            tvResult.setText("正在连接服务器...");

            // 发起网络请求
            loginWithOkHttp(username, password);
        });

        // 设置注册跳转点击事件
        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void loginWithOkHttp(String username, String password) {
        // 组装表单数据
        RequestBody formBody = new FormBody.Builder()
                .add("username", username)
                .add("password", password)
                .build();

        // 组装请求指令
        Request request = new Request.Builder()
                .url("http://10.0.2.2:8080/login")
                .post(formBody)
                .build();

        // 异步发送请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> tvResult.setText("请求失败：" + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String resultText = response.body().string();

                try {
                    // 解析响应 JSON
                    JSONObject jsonObject = new JSONObject(resultText);
                    int code = jsonObject.getInt("code");

                    if (code == 200) {
                        // 提取 data 字段（现为 JSON 对象格式）
                        JSONObject dataObj = jsonObject.getJSONObject("data");

                        // 分别提取 token 与 avatar_url
                        String token = dataObj.getString("token");
                        // 使用 optString 防止由于键不存在抛出异常
                        String avatarUrl = dataObj.optString("avatar_url", "");

                        // 持久化存储用户配置信息，包含刚同步下发的头像 URL
                        LoginActivity.this.getSharedPreferences("MyAppConfig", Context.MODE_PRIVATE)
                                .edit()
                                .putString("user_token", token)
                                .putString("username", username)
                                .putString("avatar_url", avatarUrl)
                                .apply();

                        // 执行页面跳转
                        runOnUiThread(() -> {
                            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                            startActivity(intent);
                            // 跳转后销毁登录页
                            finish();
                        });
                    } else {
                        // 提取并显示错误提示
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