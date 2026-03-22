package com.example.wordup;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Pair;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // 绑定视图组件
        ImageView ivSplashLogo = findViewById(R.id.ivSplashLogo);
        TextView tvSplashName = findViewById(R.id.tvSplashName);

        // 加载并启动复合动画
        Animation splashAnim = AnimationUtils.loadAnimation(this, R.anim.anim_splash_up);
        ivSplashLogo.startAnimation(splashAnim);
        tvSplashName.startAnimation(splashAnim);

        // 延迟执行路由与转场逻辑
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            checkLoginStatusAndRoute(ivSplashLogo);
        }, 2000);
    }

    private void checkLoginStatusAndRoute(View sharedLogoView) {
        SharedPreferences prefs = getSharedPreferences("MyAppConfig", Context.MODE_PRIVATE);
        String token = prefs.getString("user_token", null);

        if (token != null && !token.isEmpty()) {
            // 存在凭证，常规跳转至主界面
            Intent intent = new Intent(SplashActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        } else {
            // 凭证缺失，执行携带共享元素的跳转至登录界面
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            // 封装共享元素视图及其对应的 transitionName
            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(
                    SplashActivity.this,
                    Pair.create(sharedLogoView, "logo_transition")
            );
            startActivity(intent, options.toBundle());

            // 注意：使用共享元素动画时，延迟结束当前 Activity 可保证转场渲染完整
            new Handler(Looper.getMainLooper()).postDelayed(this::finish, 1000);
        }
    }
}