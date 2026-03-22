package com.example.wordup;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;

public class ProfileFragment extends Fragment {

    // 全局变量：让整个类都能认识这两个控件
    private ShapeableImageView ivAvatar;
    private TextView tvUsername;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 绑定 Fragment 布局文件
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // 1. 极其纯粹的绑定：只找控件，不在这里读取数据了！
        tvUsername = view.findViewById(R.id.tvUsername);
        ivAvatar = view.findViewById(R.id.ivAvatar);

        // 2. 绑定点击区域
        LinearLayout llProfileHeader = view.findViewById(R.id.llProfileHeader);

        // 设置点击事件，跳转到 EditProfileActivity
        llProfileHeader.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditProfileActivity.class);
            startActivity(intent);
        });

        AppCompatButton btnLogout = view.findViewById(R.id.btnLogout);

        // 3. 配置退出登录按钮点击事件
        btnLogout.setOnClickListener(v -> {
            if (getActivity() != null) {
                // 清除本地存储中的认证 Token 和 username、avatar_url
                SharedPreferences prefs = getActivity().getSharedPreferences("MyAppConfig", Context.MODE_PRIVATE);
                prefs.edit().remove("user_token").remove("username").remove("avatar_url").apply();

                // 启动登录页面并结束当前 Activity 生命周期
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                startActivity(intent);
                getActivity().finish();
            }
        });

        return view;
    }

    // 🌟 生命周期的魔法：每次从别的页面返回（重新露出本页面）时，自动执行这里！
    @Override
    public void onResume() {
        super.onResume();

        if (getActivity() != null) {
            SharedPreferences prefs = getActivity().getSharedPreferences("MyAppConfig", Context.MODE_PRIVATE);

            // 1. 每次回来都刷新一下名字
            String savedUsername = prefs.getString("username", "WordUp 用户");
            tvUsername.setText(savedUsername);

            // 2. 每次回来都刷新一下头像（由于后端的 URL 每次都是新 UUID，所以绝对不会有缓存延迟）
            String savedAvatarUrl = prefs.getString("avatar_url", "");
            if (!savedAvatarUrl.isEmpty()) {
                Glide.with(this).load(savedAvatarUrl).into(ivAvatar);
            }
        }
    }
}