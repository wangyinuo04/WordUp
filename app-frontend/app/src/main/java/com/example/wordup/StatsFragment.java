package com.example.wordup;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.compose.ui.platform.ComposeView;
import androidx.fragment.app.Fragment;
import com.example.wordup.ui.screens.StatsComposeBridgeKt;

public class StatsFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);
        ComposeView composeView = view.findViewById(R.id.compose_view);

        // 1. 获取 SharedPreferences 实例（对应 LoginActivity 中的 "MyAppConfig"）
        SharedPreferences prefs = requireActivity().getSharedPreferences("MyAppConfig", Context.MODE_PRIVATE);

        // 2. 动态读取登录时保存的 userId。第二个参数 -1L 是默认值，用于防空保护
        long currentUserId = prefs.getLong("userId", -1L);

        // 3. 校验并挂载 Compose 视图
        if (currentUserId != -1L) {
            // 成功获取到当前登录用户的真实 ID，传递给 Compose 引擎进行网络请求
            StatsComposeBridgeKt.setupStatisticsComposeView(composeView, currentUserId);
        } else {
            // 未获取到用户 ID（可能用户清除了缓存或未登录直接进来了）
            Toast.makeText(requireContext(), "未检测到登录信息，请重新登录", Toast.LENGTH_SHORT).show();
        }

        return view;
    }
}