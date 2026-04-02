package com.example.wordup;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

/**
 * AI设置页面的业务逻辑控制类。
 * 负责处理页面初始化、视图绑定及基础交互逻辑。
 */
public class AISettingsActivity extends AppCompatActivity {

    private ImageView ivBack;
    private SwitchCompat switchAntiDoze;
    private SwitchCompat switchAISentence;
    private SwitchCompat switchEmotion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_settings);

        initView();
        initListener();
    }

    /**
     * 初始化页面控件引用。
     */
    private void initView() {
        ivBack = findViewById(R.id.ivBack);
        switchAntiDoze = findViewById(R.id.switchAntiDoze);
        switchAISentence = findViewById(R.id.switchAISentence);
        switchEmotion = findViewById(R.id.switchEmotion);
    }

    /**
     * 绑定交互事件监听器。
     */
    private void initListener() {
        // 实现返回按钮逻辑，销毁当前界面并返回上一级
        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // 以下为Switch开关的状态监听占位，后续可在此处扩展业务逻辑
        switchAntiDoze.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // 防瞌睡功能状态变更逻辑
        });

        switchAISentence.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // AI造句功能状态变更逻辑
        });

        switchEmotion.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // 情绪识别功能状态变更逻辑
        });
    }
}