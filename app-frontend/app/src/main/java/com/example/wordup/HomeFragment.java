package com.example.wordup;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;

/**
 * 首页Fragment的业务逻辑控制类。
 * 负责主页视图的加载、核心交互事件的处理以及各功能模块的路由跳转逻辑。
 */
public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 实例化并返回该Fragment的视图对象
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. 学习模块：背诵新词逻辑绑定与跳转
        View btnLearnNew = view.findViewById(R.id.btnLearnNew);
        if (btnLearnNew != null) {
            btnLearnNew.setOnClickListener(v -> {
                TextView tvTodayWords = view.findViewById(R.id.tvTodayWords);
                int totalWords = 150; // 设定默认单词数量

                // 尝试从UI组件中提取用户当前的每日单词目标
                try {
                    if (tvTodayWords != null) {
                        String wordsText = tvTodayWords.getText().toString();
                        totalWords = Integer.parseInt(wordsText);
                    }
                } catch (NumberFormatException e) {
                    // 若解析失败，采用默认值确保流程不中断
                    totalWords = 150;
                }

                // 实例化学习页面组件并传递所需的参数状态（false 表示新词模式）
                WordLearningFragment fragment = WordLearningFragment.newInstance(false, totalWords);

                // 执行 Fragment 路由跳转事务并加入回退栈
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, fragment)
                            .addToBackStack(null)
                            .commit();
                }
            });
        }

        // 2. 学习模块：复习旧词逻辑绑定与跳转
        View btnReviewOld = view.findViewById(R.id.btnReviewOld);
        if (btnReviewOld != null) {
            btnReviewOld.setOnClickListener(v -> {
                TextView tvTodayWords = view.findViewById(R.id.tvTodayWords);
                int totalWords = 150; // 设定默认单词数量

                // 尝试从UI组件中提取用户当前的每日单词目标
                try {
                    if (tvTodayWords != null) {
                        String wordsText = tvTodayWords.getText().toString();
                        totalWords = Integer.parseInt(wordsText);
                    }
                } catch (NumberFormatException e) {
                    // 若解析失败，采用默认值确保流程不中断
                    totalWords = 150;
                }

                // 实例化学习页面组件并传递所需的参数状态（true 表示复习模式）
                WordLearningFragment fragment = WordLearningFragment.newInstance(true, totalWords);

                // 执行 Fragment 路由跳转事务并加入回退栈
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, fragment)
                            .addToBackStack(null)
                            .commit();
                }
            });
        }

        // 3. 计划变更模块：展示底部配置弹窗
        View btnChangePlan = view.findViewById(R.id.btnChangePlan);
        if (btnChangePlan != null) {
            btnChangePlan.setOnClickListener(v -> showPlanBottomSheet());
        }

        // 4. 设置模块：AI 设置页面路由跳转逻辑
        AppCompatButton btnSettingsAI = view.findViewById(R.id.btnSettingsAI);
        if (btnSettingsAI != null) {
            btnSettingsAI.setOnClickListener(v -> {
                // 构建并执行显式 Intent 跳转指令
                Intent intent = new Intent(requireActivity(), AISettingsActivity.class);
                startActivity(intent);
            });
        }

        // 5. 书籍变更模块：换书页面路由跳转逻辑
        View btnChangeBook = view.findViewById(R.id.btnChangeBook);
        if (btnChangeBook != null) {
            btnChangeBook.setOnClickListener(v -> {
                // 实例化 Intent，指向 ChangeBookActivity 进行视图跳转
                Intent intent = new Intent(requireActivity(), ChangeBookActivity.class);
                startActivity(intent);
            });
        }
    }

    /**
     * 实例化并展示更改计划的底部抽屉弹窗。
     * 包含输入监听与动态复习数量的计算逻辑。
     */
    private void showPlanBottomSheet() {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.layout_change_plan_bottom_sheet, null);
        bottomSheet.setContentView(sheetView);

        // 初始化弹窗内的组件引用
        EditText etNewWords = sheetView.findViewById(R.id.etNewWords);
        TextView tvReviewWords = sheetView.findViewById(R.id.tvReviewWords);
        RadioGroup rgRatio = sheetView.findViewById(R.id.rgRatio);
        Button btnConfirm = sheetView.findViewById(R.id.btnConfirmPlan);

        // 设定初始默认复习比例因子 (1:2)
        final int[] currentRatio = {2};

        // 绑定比例选择状态变更监听器
        rgRatio.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb12) {
                currentRatio[0] = 2;
            } else if (checkedId == R.id.rb13) {
                currentRatio[0] = 3;
            } else if (checkedId == R.id.rb14) {
                currentRatio[0] = 4;
            }
            updateReviewCalculation(etNewWords, tvReviewWords, currentRatio[0]);
        });

        // 绑定新词数量输入框的文本变更监听器，实现数据的实时联动
        etNewWords.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateReviewCalculation(etNewWords, tvReviewWords, currentRatio[0]);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 绑定确认修改按钮的点击事件监听器
        btnConfirm.setOnClickListener(v -> {
            String newWordsStr = etNewWords.getText().toString();
            if (newWordsStr.isEmpty()) {
                Toast.makeText(getContext(), "请输入新词数量", Toast.LENGTH_SHORT).show();
                return;
            }

            int newCount = Integer.parseInt(newWordsStr);
            int reviewCount = newCount * currentRatio[0];
            int totalTarget = newCount + reviewCount;

            // 预留接口：调用后端服务更新数据库 user_plan 表中的 daily_target 数据
            Toast.makeText(getContext(), "计划已更新，每日总目标：" + totalTarget, Toast.LENGTH_SHORT).show();
            bottomSheet.dismiss();
        });

        // 绑定弹窗展示监听器以处理背景透明度，确保自定义圆角样式正常渲染
        bottomSheet.setOnShowListener(dialog -> {
            BottomSheetDialog d = (BottomSheetDialog) dialog;
            View bottomSheetInternal = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheetInternal != null) {
                bottomSheetInternal.setBackgroundResource(android.R.color.transparent);
            }
        });

        // 执行弹窗展示操作
        bottomSheet.show();
    }

    /**
     * 根据当前输入的新词数量与设定的比例因子，动态计算并更新复习词数的展示状态。
     *
     * @param etNew    新词数量输入组件
     * @param tvReview 复习词数展示组件
     * @param ratio    当前的复习比例因子
     */
    private void updateReviewCalculation(EditText etNew, TextView tvReview, int ratio) {
        String input = etNew.getText().toString();
        if (!input.isEmpty()) {
            try {
                int newVal = Integer.parseInt(input);
                tvReview.setText(String.valueOf(newVal * ratio));
            } catch (NumberFormatException e) {
                tvReview.setText("0");
            }
        } else {
            tvReview.setText("0");
        }
    }
}