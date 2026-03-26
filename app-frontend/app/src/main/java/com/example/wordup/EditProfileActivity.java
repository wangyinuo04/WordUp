package com.example.wordup;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
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
        LinearLayout itemEditNickname = findViewById(R.id.itemEditNickname);

        SharedPreferences prefs = getSharedPreferences("MyAppConfig", Context.MODE_PRIVATE);

        /* 1. 读取并显示本地存储的昵称 */
        String savedNickname = prefs.getString("nickname", "WordUp 用户");
        tvEditNickname.setText(savedNickname);

        // 2. 🌟 检查小本子上有没有存过的头像 URL，如果有，用 Glide 自动加载并缓存！
        String savedAvatarUrl = prefs.getString("avatar_url", "");
        if (!savedAvatarUrl.isEmpty()) {
            Glide.with(this).load(savedAvatarUrl).into(ivEditAvatar);
        }

        // 点击头像，唤起相册
        itemEditAvatar.setOnClickListener(v -> mGetContent.launch("image/*"));

        /* 绑定昵称修改点击事件 */
        itemEditNickname.setOnClickListener(v -> showEditNicknameDialog(tvEditNickname));
    }

    /* 显示修改昵称对话框 */
    private void showEditNicknameDialog(TextView tvEditNickname) {
        final EditText editText = new EditText(this);
        editText.setText(tvEditNickname.getText().toString());
        editText.setSelection(editText.getText().length());

        new AlertDialog.Builder(this)
                .setTitle("修改昵称")
                .setView(editText)
                .setPositiveButton("确定", (dialog, which) -> {
                    String newNickname = editText.getText().toString().trim();
                    if (!newNickname.isEmpty()) {
                        updateNicknameLocal(newNickname, tvEditNickname);
                    } else {
                        Toast.makeText(EditProfileActivity.this, "昵称不能为空", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /* 更新本地昵称数据并刷新界面 */
    private void updateNicknameLocal(String newNickname, TextView tvEditNickname) {
        SharedPreferences prefs = getSharedPreferences("MyAppConfig", Context.MODE_PRIVATE);
        prefs.edit().putString("nickname", newNickname).apply();
        tvEditNickname.setText(newNickname);
        Toast.makeText(this, "昵称修改成功（暂存本地）", Toast.LENGTH_SHORT).show();
    }

    private void uploadAvatarToServer(Uri uri) {

        // 1. 将系统的 Uri 转化成真实的 File
        File tempFile = uriToFile(uri);
        if (tempFile == null) return;

        // 2. 读取当前账号名
        SharedPreferences prefs = getSharedPreferences("MyAppConfig", Context.MODE_PRIVATE);
        String username = prefs.getString("username", "");

        // 3. 组装 MultipartBody
        RequestBody fileBody = RequestBody.create(tempFile, MediaType.parse("image/*"));
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", tempFile.getName(), fileBody)
                .addFormDataPart("username", username)
                .build();

        // 4. 发起网络请求
        Request request = new Request.Builder()
                .url("http://10.0.2.2:8080/uploadAvatar")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(EditProfileActivity.this, "网络请求失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    final String resultText = response.body() != null ? response.body().string() : "";

                    // 将后端返回的 JSON 字符串解析为 JSONObject
                    org.json.JSONObject jsonObject = new org.json.JSONObject(resultText);
                    int code = jsonObject.getInt("code");

                    runOnUiThread(() -> {
                        if (code == 200) {
                            try {
                                // 提取真实的图片 URL
                                String avatarUrl = jsonObject.getString("data");

                                // 将正确的 URL 保存到本地缓存
                                prefs.edit().putString("avatar_url", avatarUrl).apply();

                                // 使用 Glide 加载网络图片到当前的 ImageView
                                Glide.with(EditProfileActivity.this).load(avatarUrl).into(ivEditAvatar);

                                Toast.makeText(EditProfileActivity.this, "头像更新成功", Toast.LENGTH_SHORT).show();
                            } catch (org.json.JSONException e) {
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                String msg = jsonObject.getString("msg");
                                Toast.makeText(EditProfileActivity.this, "上传失败: " + msg, Toast.LENGTH_SHORT).show();
                            } catch (org.json.JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(EditProfileActivity.this, "数据解析异常", Toast.LENGTH_SHORT).show());
                } finally {
                    if (response.body() != null) {
                        response.body().close();
                    }
                }
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