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

    // 全局变量：控件引用声明
    private ShapeableImageView ivAvatar;
    private TextView tvNickname;
    private TextView tvUsername;
    private TextView tvGrade;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 绑定 Fragment 布局文件
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // 1. 控件绑定与初始化
        tvNickname = view.findViewById(R.id.tvNickname);
        tvUsername = view.findViewById(R.id.tvUsername);
        tvGrade = view.findViewById(R.id.tvGrade);
        ivAvatar = view.findViewById(R.id.ivAvatar);

        // 2. 绑定头部区域点击事件，实现个人资料编辑页面跳转
        LinearLayout llProfileHeader = view.findViewById(R.id.llProfileHeader);
        if (llProfileHeader != null) {
            llProfileHeader.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), EditProfileActivity.class);
                startActivity(intent);
            });
        }

        // 3. 绑定新增的“设置AI”列表项点击事件，实现AI设置页面跳转
        LinearLayout itemAISettings = view.findViewById(R.id.itemAISettings);
        if (itemAISettings != null) {
            itemAISettings.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), AISettingsActivity.class);
                startActivity(intent);
            });
        }

        // 4. 配置退出登录按钮点击事件及状态清除逻辑
        AppCompatButton btnLogout = view.findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                if (getActivity() != null) {
                    // 清除本地存储中的认证 Token 及用户信息
                    SharedPreferences prefs = getActivity().getSharedPreferences("MyAppConfig", Context.MODE_PRIVATE);
                    prefs.edit()
                            .remove("user_token")
                            .remove("username")
                            .remove("nickname")
                            .remove("grade")
                            .remove("avatar_url")
                            .apply();

                    // 启动登录页面并结束当前 Activity 生命周期
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    startActivity(intent);
                    getActivity().finish();
                }
            });
        }

        return view;
    }

    // 生命周期回调：页面重新获取焦点时同步执行数据刷新操作
    @Override
    public void onResume() {
        super.onResume();

        if (getActivity() != null) {
            SharedPreferences prefs = getActivity().getSharedPreferences("MyAppConfig", Context.MODE_PRIVATE);

            // 1. 重新读取并渲染昵称数据
            String savedNickname = prefs.getString("nickname", "WordUp 用户");
            tvNickname.setText(savedNickname);

            // 2. 重新读取并渲染用户名（ID）数据
            String savedUsername = prefs.getString("username", "未知");
            tvUsername.setText("ID: " + savedUsername);

            // 3. 重新读取并渲染年级数据
            String savedGrade = prefs.getString("grade", "年级未设置");
            tvGrade.setText(savedGrade);

            // 4. 重新读取并加载用户头像图片资源
            String savedAvatarUrl = prefs.getString("avatar_url", "");
            if (!savedAvatarUrl.isEmpty()) {
                // 替换虚拟IP为统一配置的真实局域网IP，以确保物理设备正常加载图片
                savedAvatarUrl = savedAvatarUrl.replace("http://10.0.2.2:8080", NetworkConfig.BASE_URL);
                Glide.with(this).load(savedAvatarUrl).into(ivAvatar);
            }
        }
    }
}