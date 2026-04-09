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
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.FrameLayout;
import android.util.TypedValue;

public class EditProfileActivity extends AppCompatActivity {

    private ShapeableImageView ivEditAvatar;
    private TextView tvEditGender, tvEditSchool, tvEditGrade;
    private OkHttpClient client = new OkHttpClient(); // 发网络请求的客户端
    // 1. 定义左侧的教育阶段
    private final String[] stages = {"小学", "初中", "高中", "本科", "硕士", "其他"};

    // 2. 定义右侧对应的年级范围
    private String[] getGradesByStage(String stage) {
        switch (stage) {
            case "小学":
                return new String[]{"一年级", "二年级", "三年级", "四年级", "五年级", "六年级"};
            case "初中":
            case "高中":
                return new String[]{"一年级", "二年级", "三年级"};
            case "本科":
                return new String[]{"大一", "大二", "大三", "大四"};
            case "硕士":
                return new String[]{"研一", "研二", "研三"};
            default:
                return new String[]{"无"};
        }
    }

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

        // --- 1. 变量绑定---
        ImageView ivBack = findViewById(R.id.ivBack);
        ivEditAvatar = findViewById(R.id.ivEditAvatar);
        TextView tvEditNickname = findViewById(R.id.tvEditNickname);
        tvEditGender = findViewById(R.id.tvEditGender);
        tvEditSchool = findViewById(R.id.tvEditSchool);
        tvEditGrade = findViewById(R.id.tvEditGrade);

        LinearLayout itemEditAvatar = findViewById(R.id.itemEditAvatar);
        LinearLayout itemEditNickname = findViewById(R.id.itemEditNickname);
        LinearLayout itemEditGender = findViewById(R.id.itemEditGender);
        LinearLayout itemEditSchool = findViewById(R.id.itemEditSchool);
        LinearLayout itemEditGrade = findViewById(R.id.itemEditGrade);

        // --- 2. 基础逻辑（保留原有的） ---
        ivBack.setOnClickListener(v -> finish());
        SharedPreferences prefs = getSharedPreferences("MyAppConfig", Context.MODE_PRIVATE);
        tvEditNickname.setText(prefs.getString("nickname", "WordUp 用户"));
        tvEditGender.setText(prefs.getString("gender", "保密"));
        tvEditSchool.setText(prefs.getString("school", "未设置"));
        tvEditGrade.setText(prefs.getString("grade", "未设置"));

        String savedAvatarUrl = prefs.getString("avatar_url", "");
        if (!savedAvatarUrl.isEmpty()) {
            // 替换虚拟IP为统一配置的真实局域网IP，以确保物理设备正常加载图片
            savedAvatarUrl = savedAvatarUrl.replace("http://10.0.2.2:8080", NetworkConfig.BASE_URL);
            Glide.with(this).load(savedAvatarUrl).into(ivEditAvatar);
        }

        // --- 3. 设置点击事件 ---
        itemEditAvatar.setOnClickListener(v -> mGetContent.launch("image/*"));
        itemEditNickname.setOnClickListener(v -> showEditDialog("昵称", "nickname", tvEditNickname));
        itemEditGender.setOnClickListener(v -> showGenderPicker());
        itemEditSchool.setOnClickListener(v -> showEditDialog("学校", "school", tvEditSchool));
        // 修改年级的点击事件，改用我们新写的滑动选择器
        itemEditGrade.setOnClickListener(v -> showGradePicker());
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

        // 4. 发起网络请求，调用 NetworkConfig 中的统一接口常量
        Request request = new Request.Builder()
                .url(NetworkConfig.UPLOAD_AVATAR_URL)
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
                    String code = jsonObject.optString("code");

                    runOnUiThread(() -> {
                        if ("200".equals(code)) {
                            try {
                                // 提取真实的图片 URL
                                String avatarUrl = jsonObject.getString("data");

                                // 拦截并替换虚拟IP为统一配置的真实局域网IP
                                avatarUrl = avatarUrl.replace("http://10.0.2.2:8080", NetworkConfig.BASE_URL);

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

    /**
     * 通用的修改弹窗
     */
    private void showEditDialog(String title, String key, final TextView target) {
        // 1. 创建一个容器，让布局有边距
        FrameLayout container = new FrameLayout(this);
        EditText editText = new EditText(this);

        // 美化输入框：去掉下划线，换成淡色背景
        editText.setText(target.getText().toString());
        editText.setSelection(editText.getText().length()); // 光标移到末尾
        editText.setPadding(40, 30, 40, 30);
        editText.setBackgroundResource(R.drawable.bg_rounded_soft); // 复用队友的淡色背景
        editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);

        // 设置容器边距
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(60, 20, 60, 20); // 左右留白，上下微调
        container.addView(editText, params);

        // 2. 构建弹窗
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("修改" + title)
                .setView(container)
                .setPositiveButton("确定", (d, which) -> {
                    String newValue = editText.getText().toString().trim();
                    if (!newValue.isEmpty()) {
                        target.setText(newValue);
                        sendUpdateToServer(key, newValue);
                    }
                })
                .setNegativeButton("取消", null)
                .create();

        dialog.show();

        // 3. 注入圆角灵魂与按钮美化
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_rounded_soft_pop_up);
            dialog.getWindow().setDimAmount(0.45f);
        }

        // 按钮颜色统一
        int goldColor = getResources().getColor(R.color.wordup_gold_button);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(goldColor);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.wordup_text_secondary));
    }

    /**
     * 性别选择弹窗
     */
    private void showGenderPicker() {
        String[] options = {"男", "女"};

        // 1. 构建弹窗对象（使用 .create() 暂时不显示）
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("选择性别")
                .setItems(options, (d, which) -> {
                    String selected = options[which];
                    tvEditGender.setText(selected);
                    sendUpdateToServer("gender", selected); // 同步发包给后端
                })
                .create();

        // 2. 正式显示
        dialog.show();

        // 3. 注入圆角灵魂
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_rounded_soft_pop_up);
            // 保持遮罩亮度一致
            dialog.getWindow().setDimAmount(0.45f);
        }
    }

    /**
     * 年级选择弹窗
     */
    private void showGradePicker() {
        // 1. 创建容器和布局参数
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setGravity(Gravity.CENTER);
        container.setPadding(20, 40, 20, 40);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);

        // 2. 初始化左侧：阶段选择器
        final NumberPicker pickerStage = new NumberPicker(this);
        pickerStage.setMinValue(0);
        pickerStage.setMaxValue(stages.length - 1);
        pickerStage.setDisplayedValues(stages);
        pickerStage.setWrapSelectorWheel(false);

        // 3. 初始化右侧：年级选择器
        final NumberPicker pickerGrade = new NumberPicker(this);
        String[] initialGrades = getGradesByStage(stages[0]);
        pickerGrade.setMinValue(0);
        pickerGrade.setMaxValue(initialGrades.length - 1);
        pickerGrade.setDisplayedValues(initialGrades);

        // 4. 联动逻辑
        pickerStage.setOnValueChangedListener((picker, oldVal, newVal) -> {
            String[] newGrades = getGradesByStage(stages[newVal]);
            pickerGrade.setDisplayedValues(null);
            pickerGrade.setMinValue(0);
            pickerGrade.setMaxValue(newGrades.length - 1);
            pickerGrade.setDisplayedValues(newGrades);
        });

        // 5. 将选择器放入容器
        container.addView(pickerStage, params);
        container.addView(pickerGrade, params);

        // 6. 构建弹窗对象
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("选择年级")
                .setView(container)
                .setPositiveButton("确定", (d, which) -> {
                    // 这里是发包逻辑，确保能读取到上面定义的 pickerStage 和 pickerGrade
                    String stagePart = stages[pickerStage.getValue()];
                    String gradePart = pickerGrade.getDisplayedValues()[pickerGrade.getValue()];
                    String result = stagePart + " " + gradePart;

                    tvEditGrade.setText(result);
                    sendUpdateToServer("grade", result); // 发包给后端
                })
                .setNegativeButton("取消", null)
                .create(); // 先创建

        // 7. 显示并整容
        dialog.show();

        if (dialog.getWindow() != null) {
            // 使用你命名的那个明晰的圆角背景
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_rounded_soft_pop_up);
            dialog.getWindow().setDimAmount(0.45f);
        }

        // 8. 按钮颜色对齐
        int themeColor = getResources().getColor(R.color.wordup_gold_button);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(themeColor);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.wordup_text_secondary));
    }

    /**
     * 统一发包给后端
     */
    private void sendUpdateToServer(String key, String value) {
        // 1. 先声明并获取 username (这一步必须在最前面)
        SharedPreferences prefs = getSharedPreferences("MyAppConfig", Context.MODE_PRIVATE);
        String username = prefs.getString("username", "");

        // 2. 现在再打印，就不会报红了
        android.util.Log.d("NETWORK_DEBUG", "正在发送请求，用户名是: [" + username + "]");
        android.util.Log.d("NETWORK_DEBUG", "请求地址: " + NetworkConfig.UPDATE_PROFILE_URL);

        // 3. 组装 JSON
        String json = "{\"username\":\"" + username + "\", \"" + key + "\":\"" + value + "\"}";
        // 2. 使用 OkHttp 发送请求
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));

        // --- 核心修改点：使用 NetworkConfig 中定义的常量 ---
        Request request = new Request.Builder()
                .url(NetworkConfig.UPDATE_PROFILE_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    // 打印日志方便调试
                    android.util.Log.e("NETWORK_DEBUG", "请求失败: " + NetworkConfig.UPDATE_PROFILE_URL, e);
                    Toast.makeText(EditProfileActivity.this, "连接失败：" + e.toString(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    // 3. 后端返回成功后，同步到本地 SharedPreferences 缓存
                    prefs.edit().putString(key, value).apply();

                    // 可选：在 UI 线程弹个简单的成功提示
                    runOnUiThread(() -> {
                        // Toast.makeText(EditProfileActivity.this, "保存成功", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    // 如果后端返回了 404 或 500 等错误
                    runOnUiThread(() -> {
                        Toast.makeText(EditProfileActivity.this, "后端拒绝请求，错误码：" + response.code(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
}