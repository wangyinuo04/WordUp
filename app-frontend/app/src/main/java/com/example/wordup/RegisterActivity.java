package com.example.wordup;


import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText etRegUsername;
    private EditText etRegPassword;
    private EditText etRegConfirmPassword;
    private TextView tvRegResult;
    private OkHttpClient client = new OkHttpClient();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 1. 绑定控件
        etRegUsername = findViewById(R.id.etRegUsername);
        etRegPassword = findViewById(R.id.etRegPassword);
        etRegConfirmPassword = findViewById(R.id.etRegConfirmPassword);
        Button btnSubmitRegister = findViewById(R.id.btnSubmitRegister);
        tvRegResult = findViewById(R.id.tvRegResult);
        // 2. 点击注册按钮的逻辑
        btnSubmitRegister.setOnClickListener(v -> {
            String username = etRegUsername.getText().toString().trim();
            String password = etRegPassword.getText().toString().trim();
            String confirmPassword = etRegConfirmPassword.getText().toString().trim();

            // 基础防呆校验
            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                tvRegResult.setText("所有内容都必须填写！");
                return;
            }
            // 核心：校验两次密码是否一致
            if (!password.equals(confirmPassword)) {
                tvRegResult.setText("两次输入的密码不一致，请重新输入！");
                return;
            }

            tvRegResult.setText("正在提交注册信息...");
            // 发送网络请求给后端
            submitRegistration(username, password);
        });
    }

    private void submitRegistration(String username, String password) {
        RequestBody formBody = new FormBody.Builder()
                .add("username", username)
                .add("password", password) // 注意：发给后端只需要发一次密码就行
                .build();

        Request request = new Request.Builder()
                .url("http://192.168.198.168:8080/register")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> tvRegResult.setText("网络请求失败：" + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String resultText = response.body().string();

                runOnUiThread(() -> {
                    // 如果注册成功了（判断后端返回的话里有没有"成功"俩字）
                    if (resultText.contains("成功")) {
                        // 弹出一个系统小黑条提示
                        Toast.makeText(RegisterActivity.this, "注册成功，快去登录吧！", Toast.LENGTH_SHORT).show();
                        // 注册成功后，直接关掉当前注册页面，就会自动回到上一层（登录页）
                        finish();
                    } else {
                        // 如果失败（比如名字被占用了），就把原因显示在界面上
                        tvRegResult.setText(resultText);
                    }
                });
            }
        });
    }
}