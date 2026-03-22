package com.example.wordup;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // 🌟 魔法 1：初始化默认显示的 Fragment（优先展示我们刚刚画好的惊艳“学习”主页！）
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment()) // 👈 这里改成了新写的 HomeFragment
                    .commit();
            // 默认点亮底部的“学习”图标
            bottomNav.setSelectedItemId(R.id.nav_study);
        }

        // 监听底部导航栏的点击事件
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            // 🌟 魔法 2：将 nav_study 完美映射到你的 HomeFragment 演员上！
            if (itemId == R.id.nav_study) {
                selectedFragment = new HomeFragment(); // 👈 这里将原来的 StudyFragment 换成了 HomeFragment
            } else if (itemId == R.id.nav_stats) {
                // 统计页暂时保留你的原逻辑（前提是你项目里已经建了一个空的 StatsFragment 类，否则这里会报红）
                selectedFragment = new StatsFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            // 执行 Fragment 替换事务
            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });
    }
}