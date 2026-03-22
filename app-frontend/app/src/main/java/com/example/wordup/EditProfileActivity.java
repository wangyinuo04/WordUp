package com.example.wordup;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EditProfileActivity extends AppCompatActivity {

    private ShapeableImageView ivEditAvatar;
    private OkHttpClient client = new OkHttpClient(); // 发网络请求的客户端

    // 🌟 相册选择器
    private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    // 1. 本地先瞬间预览，让用户感觉很顺滑
                    ivEditAvatar.setImageURI(uri);
                    // 2. 偷偷在后台把图片发给 Spring Boot
                    uploadAvatarToServer(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        ImageView ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> finish());

        TextView tvEditNickname = findViewById(R.id.tvEditNickname);
        ivEditAvatar = findViewById(R.id.ivEditAvatar);
        LinearLayout itemEditAvatar = findViewById(R.id.itemEditAvatar);

        // 1. 读取账号名
        SharedPreferences prefs = getSharedPreferences("MyAppConfig", Context.MODE_PRIVATE);
        String savedUsername = prefs.getString("username", "WordUp 用户");
        tvEditNickname.setText(savedUsername);

        // 2. 🌟 检查小本子上有没有存过的头像 URL，如果有，用 Glide 自动加载并缓存！
        String savedAvatarUrl = prefs.getString("avatar_url", "");
        if (!savedAvatarUrl.isEmpty()) {
            Glide.with(this).load(savedAvatarUrl).into(ivEditAvatar);
        }

        // 点击头像，唤起相册
        itemEditAvatar.setOnClickListener(v -> mGetContent.launch("image/*"));
    }

    // ==========================================
    // 🚨 核心战役：上传图片给后端的硬核代码 🚨
    // ==========================================
    private void uploadAvatarToServer(Uri uri) {

        // 1. 极其关键的辅助操作：把系统的 Uri 转化成真实的 File
        File tempFile = uriToFile(uri);
        if (tempFile == null) return;

        // 2. 拿出你的当前账号名
        SharedPreferences prefs = getSharedPreferences("MyAppConfig", Context.MODE_PRIVATE);
        String username = prefs.getString("username", "");

        // 3. 组装 MultipartBody (也就是构建包含文件和文字的复杂表单)
        RequestBody fileBody = RequestBody.create(tempFile, MediaType.parse("image/*"));
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM) // 表单类型
                .addFormDataPart("file", tempFile.getName(), fileBody) // 塞入图片文件
                .addFormDataPart("username", username)                 // 塞入用户名
                .build();

        // 4. 发射给咱们刚才写的 Spring Boot 接口
        Request request = new Request.Builder()
                .url("http://10.0.2.2:8080/uploadAvatar")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(EditProfileActivity.this, "上传失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String resultText = response.body().string();

                runOnUiThread(() -> {
                    // 如果后端返回的是 http 开头的网址，说明成功了！
                    if (resultText.startsWith("http")) {
                        // 把这段专属的图片网址，记在小本子上！
                        prefs.edit().putString("avatar_url", resultText).apply();
                        Toast.makeText(EditProfileActivity.this, "头像更新成功", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(EditProfileActivity.this, resultText, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    // 神级辅助方法：将系统相册的 Uri 拷贝到咱们 App 专属的缓存目录，变成真实的 File 文件
    private File uriToFile(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            File tempFile = new File(getCacheDir(), "temp_avatar.jpg");
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();
            return tempFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}