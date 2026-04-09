package com.example.wordup;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

/**
 * AI设置页面的业务逻辑控制类。
 * 已修复：将硬编码 ID 替换为从 SharedPreferences 动态加载。
 */
public class AISettingsActivity extends AppCompatActivity {

    private ImageView ivBack;
    private SwitchCompat switchAntiDoze;
    private SwitchCompat switchAISentence;
    private SwitchCompat switchEmotion;

    // 动态获取当前登录用户的 ID，初始设为 -1 代表非法
    private long currentUserId;

    private boolean isProgrammaticChange = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_settings);

        // 从本地缓存加载当前用户的真实 ID
        currentUserId = getSharedPreferences("MyAppConfig", Context.MODE_PRIVATE).getLong("userId", -1L);

        if (currentUserId == -1L) {
            Toast.makeText(this, "用户信息异常，请重新登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initView();
        initListener();
        fetchCurrentSettings();
    }

    private void initView() {
        ivBack = findViewById(R.id.ivBack);
        switchAntiDoze = findViewById(R.id.switchAntiDoze);
        switchAISentence = findViewById(R.id.switchAISentence);
        switchEmotion = findViewById(R.id.switchEmotion);
    }

    private void fetchCurrentSettings() {
        // 使用动态加载的 currentUserId 发起请求
        AiNetworkHelper.getAiSettings(currentUserId, new AiNetworkHelper.GetSettingsCallback() {
            @Override
            public void onSuccess(UserPlan plan) {
                isProgrammaticChange = true;
                switchAntiDoze.setChecked(plan.getAntiSleepOn() != null && plan.getAntiSleepOn() == 1);
                switchAISentence.setChecked(plan.getAiSentenceOn() != null && plan.getAiSentenceOn() == 1);
                switchEmotion.setChecked(plan.getEmotionRecogOn() != null && plan.getEmotionRecogOn() == 1);
                isProgrammaticChange = false;
            }

            @Override
            public void onFailure(String errorMsg) {
                Toast.makeText(AISettingsActivity.this, "配置加载失败：" + errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initListener() {
        ivBack.setOnClickListener(v -> finish());

        CompoundButton.OnCheckedChangeListener switchListener = (buttonView, isChecked) -> {
            if (!isProgrammaticChange) {
                syncSettingsToBackend();
            }
        };

        switchAntiDoze.setOnCheckedChangeListener(switchListener);
        switchAISentence.setOnCheckedChangeListener(switchListener);
        switchEmotion.setOnCheckedChangeListener(switchListener);
    }

    private void syncSettingsToBackend() {
        UserPlan currentPlan = new UserPlan();
        // 绑定实际的 currentUserId，确保后端隔离更新
        currentPlan.setUserId(currentUserId);
        currentPlan.setAntiSleepOn(switchAntiDoze.isChecked() ? 1 : 0);
        currentPlan.setAiSentenceOn(switchAISentence.isChecked() ? 1 : 0);
        currentPlan.setEmotionRecogOn(switchEmotion.isChecked() ? 1 : 0);

        AiNetworkHelper.updateAiSettings(currentPlan, new AiNetworkHelper.UpdateSettingsCallback() {
            @Override
            public void onSuccess() {
                // 状态同步成功
            }

            @Override
            public void onFailure(String errorMsg) {
                Toast.makeText(AISettingsActivity.this, "配置同步失败，请检查网络", Toast.LENGTH_SHORT).show();
            }
        });
    }
}