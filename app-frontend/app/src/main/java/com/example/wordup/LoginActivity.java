package com.example.wordup;


import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.Context;

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
    private OkHttpClient client = new OkHttpClient(); // 实例化 OkHttp

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 绑定界面上的控件
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView btnRegister = findViewById(R.id.btnRegister);
        tvResult = findViewById(R.id.tvResult);

        // 给按钮设置点击事件
        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString();
            String password = etPassword.getText().toString();
            tvResult.setText("正在连接服务器...");

            // 发起网络请求
            loginWithOkHttp(username, password);
        });

        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

    }

    private void loginWithOkHttp(String username, String password) {
        // 1. 组装要发送的表单数据 (和你刚才在 Apifox 里填的一样)
        RequestBody formBody = new FormBody.Builder()
                .add("username", username)
                .add("password", password)
                .build();

        // 2. 组装请求指令 ⚠️ 极其关键：模拟器访问电脑本机，必须用 10.0.2.2
        Request request = new Request.Builder()
                .url("http://10.0.2.2:8080/login")
                .post(formBody)
                .build();

        // 3. 派小弟（异步线程）去发送请求，千万别把主线程卡死
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 失败了（比如没网、后端没开）
                runOnUiThread(() -> tvResult.setText("请求失败：" + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String resultText = response.body().string();

                // 1. 判断是不是真的拿到了 Token
                if (resultText.contains("Token")) {
                    // 提取出纯净的 Token 字符串
                    String token = resultText.substring(resultText.indexOf("：") + 1).trim();

                    // 2. 核心操作：加了 LoginActivity.this 和 Context.MODE_PRIVATE，彻底消灭飘红
                    LoginActivity.this.getSharedPreferences("MyAppConfig", Context.MODE_PRIVATE)
                            .edit()
                            .putString("user_token", token)
                            .putString("username", username)
                            .apply();

                    // 3. 激动人心的跳转！上下文正确修改为 LoginActivity.this
                    runOnUiThread(() -> {
                        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                        startActivity(intent);

                        // 跳转后销毁登录页
                        finish();
                    });
                } else {
                    // 如果账号密码错误，就只在界面上提示，不跳转
                    runOnUiThread(() -> tvResult.setText(resultText));
                }
            }
        });




    }

}