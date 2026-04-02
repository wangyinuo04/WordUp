package com.example.wordup;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * 应用主界面的容器Activity。
 * 负责底部导航栏的管理及各顶级Fragment的路由切换。
 */
public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // 初始化默认显示的 Fragment 组件（优先加载首页视图）
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
            // 设定底部导航栏的默认选中状态
            bottomNav.setSelectedItemId(R.id.nav_study);
        }

        // 绑定底部导航栏的菜单项选中事件监听器
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            // 根据选中的菜单项资源ID，实例化对应的 Fragment 目标类
            if (itemId == R.id.nav_study) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_stats) {
                // 统计页面视图实例（需确保项目中已存在 StatsFragment 类定义）
                selectedFragment = new StatsFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            // 若匹配成功，则执行 Fragment 的替换与事务提交操作
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