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

        etRegUsername = findViewById(R.id.etRegUsername);
        etRegPassword = findViewById(R.id.etRegPassword);
        etRegConfirmPassword = findViewById(R.id.etRegConfirmPassword);
        Button btnSubmitRegister = findViewById(R.id.btnSubmitRegister);
        tvRegResult = findViewById(R.id.tvRegResult);

        btnSubmitRegister.setOnClickListener(v -> {
            String username = etRegUsername.getText().toString().trim();
            String password = etRegPassword.getText().toString().trim();
            String confirmPassword = etRegConfirmPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                tvRegResult.setText("所有内容都必须填写！");
                return;
            }
            if (!password.equals(confirmPassword)) {
                tvRegResult.setText("两次输入的密码不一致，请重新输入！");
                return;
            }

            tvRegResult.setText("正在提交注册信息...");
            submitRegistration(username, password);
        });
    }

    private void submitRegistration(String username, String password) {
        RequestBody formBody = new FormBody.Builder()
                .add("username", username)
                .add("password", password)
                .build();

        // ✅ 修改为统一的 NetworkConfig 常量
        // 原代码：.url("http://192.168.198.168:8080/register")
        Request request = new Request.Builder()
                .url(NetworkConfig.BASE_URL + "/register")
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
                    if (resultText.contains("成功")) {
                        Toast.makeText(RegisterActivity.this, "注册成功，快去登录吧！", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        tvRegResult.setText(resultText);
                    }
                });
            }
        });
    }
}