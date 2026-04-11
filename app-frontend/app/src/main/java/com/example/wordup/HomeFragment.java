package com.example.wordup;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 首页Fragment的业务逻辑控制类。
 * 负责主页视图的加载、核心交互事件的处理以及各功能模块的路由跳转逻辑。
 */
public class HomeFragment extends Fragment {

    // 日期、书名及学习目标显示的 UI 组件
    private TextView tvDate;
    private TextView tvCurrentBook;
    private TextView tvTodayWords;

    // AI 设置状态栏对应的 UI 组件
    private ImageView ivStatusAntiDoze, ivStatusAISentence, ivStatusEmotion;
    private TextView tvStatusAntiDoze, tvStatusAISentence, tvStatusEmotion;

    // 动态获取当前登录用户的 ID
    private long currentUserId;

    // ==========================================
    // 独立缓存当前用户的新词配额、旧词配额以及总配额
    // ==========================================
    private int currentDailyNewTarget = 10;    // 默认兜底值
    private int currentDailyReviewTarget = 20; // 默认兜底值

    // 新增：内存级总数状态缓存，用于阻断 View 重建时的 XML 默认值闪现
    private int currentDailyTotalTarget = -1;

    // 防连点与探针网络请求状态位
    private boolean isCheckingJump = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 从 SharedPreferences 动态加载当前用户的真实 ID
        currentUserId = requireActivity().getSharedPreferences("MyAppConfig", Context.MODE_PRIVATE).getLong("userId", -1L);

        initViews(view);

        // 1. 学习模块：背诵新词逻辑绑定与跳转
        View btnLearnNew = view.findViewById(R.id.btnLearnNew);
        if (btnLearnNew != null) {
            // 传入 false 表示新词模式
            btnLearnNew.setOnClickListener(v -> handleLearningJump(false));
        }

        // 2. 学习模块：复习旧词逻辑绑定与跳转
        View btnReviewOld = view.findViewById(R.id.btnReviewOld);
        if (btnReviewOld != null) {
            // 传入 true 表示复习模式
            btnReviewOld.setOnClickListener(v -> handleLearningJump(true));
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
                Intent intent = new Intent(requireActivity(), AISettingsActivity.class);
                startActivity(intent);
            });
        }

        // 5. 书籍变更模块：换书页面路由跳转逻辑
        View btnChangeBook = view.findViewById(R.id.btnChangeBook);
        if (btnChangeBook != null) {
            btnChangeBook.setOnClickListener(v -> {
                Intent intent = new Intent(requireActivity(), ChangeBookActivity.class);
                startActivity(intent);
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // 只有在获取到有效用户 ID 时才发起网络请求
        if (currentUserId != -1L) {
            refreshAiSettingsStatus(); // 该接口同时负责拉取并渲染 dailyTarget 和拆分后的配额
            refreshCurrentBookName();
        }
        if (tvDate != null) {
            updateDateText();
        }
    }

    /**
     * 集中初始化所有视图控件引用
     */
    private void initViews(View view) {
        tvDate = view.findViewById(R.id.tvDate);
        tvCurrentBook = view.findViewById(R.id.tvCurrentBook);
        tvTodayWords = view.findViewById(R.id.tvTodayWords);

        // ========================================================================
        // 核心修复：View 重建时立刻使用内存缓存值覆盖 XML 中的默认值 150
        // ========================================================================
        if (tvTodayWords != null) {
            if (currentDailyTotalTarget != -1) {
                tvTodayWords.setText(String.valueOf(currentDailyTotalTarget));
            } else {
                tvTodayWords.setText("--"); // 首次尚未加载完成时，使用破折号占位防止误导
            }
        }

        ivStatusAntiDoze = view.findViewById(R.id.ivStatusAntiDoze);
        tvStatusAntiDoze = view.findViewById(R.id.tvStatusAntiDoze);

        ivStatusAISentence = view.findViewById(R.id.ivStatusAISentence);
        tvStatusAISentence = view.findViewById(R.id.tvStatusAISentence);

        ivStatusEmotion = view.findViewById(R.id.ivStatusEmotion);
        tvStatusEmotion = view.findViewById(R.id.tvStatusEmotion);

        if (tvDate != null) updateDateText();
    }

    private void updateDateText() {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd EEE.", Locale.ENGLISH);
        String currentDate = sdf.format(new Date());
        tvDate.setText(currentDate);
    }

    /**
     * 请求后端获取当前正在学习的词书名称并渲染
     */
    private void refreshCurrentBookName() {
        if (tvCurrentBook == null) return;

        BookNetworkHelper.getCurrentBook(currentUserId, new BookNetworkHelper.GetCurrentBookCallback() {
            @Override
            public void onSuccess(String bookName) {
                if (!isAdded()) return;
                tvCurrentBook.setText("《" + bookName + "》");
            }

            @Override
            public void onFailure(String errorMsg) {
                if (!isAdded()) return;
                tvCurrentBook.setText("《暂无词书》");
            }
        });
    }

    /**
     * 请求后端数据并渲染 AI 状态 UI 面板，同时更新独立配额缓存
     */
    private void refreshAiSettingsStatus() {
        AiNetworkHelper.getAiSettings(currentUserId, new AiNetworkHelper.GetSettingsCallback() {
            @Override
            public void onSuccess(UserPlan plan) {
                if (!isAdded()) return;

                boolean antiDozeOn = plan.getAntiSleepOn() != null && plan.getAntiSleepOn() == 1;
                boolean aiSentenceOn = plan.getAiSentenceOn() != null && plan.getAiSentenceOn() == 1;
                boolean emotionOn = plan.getEmotionRecogOn() != null && plan.getEmotionRecogOn() == 1;

                renderSingleStatus(ivStatusAntiDoze, tvStatusAntiDoze, antiDozeOn, "防瞌睡");
                renderSingleStatus(ivStatusAISentence, tvStatusAISentence, aiSentenceOn, "AI造句");
                renderSingleStatus(ivStatusEmotion, tvStatusEmotion, emotionOn, "情绪识别");

                if (plan.getDailyNewTarget() != null) {
                    currentDailyNewTarget = plan.getDailyNewTarget();
                }
                if (plan.getDailyReviewTarget() != null) {
                    currentDailyReviewTarget = plan.getDailyReviewTarget();
                }

                if (plan.getDailyTarget() != null) {
                    // 同步更新缓存状态，并渲染 UI
                    currentDailyTotalTarget = plan.getDailyTarget();
                    if (tvTodayWords != null) {
                        tvTodayWords.setText(String.valueOf(currentDailyTotalTarget));
                    }
                }
            }

            @Override
            public void onFailure(String errorMsg) {
                // 静默处理
            }
        });
    }

    private void renderSingleStatus(ImageView iv, TextView tv, boolean isOn, String featureName) {
        if (iv == null || tv == null) return;
        if (isOn) {
            iv.setImageResource(R.drawable.ic_status_check);
            tv.setText("已开启" + featureName);
            tv.setTextColor(Color.parseColor("#666666"));
        } else {
            iv.setImageResource(R.drawable.ic_status_cross);
            tv.setText("未开启" + featureName);
            tv.setTextColor(Color.parseColor("#999999"));
        }
    }

    // ========================================================================
    // API 探针校验，阻断空数据时的页面闪现 (已通过验证，不做随意修改)
    // ========================================================================
    private void handleLearningJump(boolean isReviewMode) {
        // 防止用户狂点按钮导致发起多次请求或发生多次 Fragment 叠加
        if (isCheckingJump) return;
        isCheckingJump = true;

        int targetWords = isReviewMode ? currentDailyReviewTarget : currentDailyNewTarget;
        long currentBookId = requireActivity().getSharedPreferences("MyAppConfig", Context.MODE_PRIVATE).getLong("current_book_id", 1L);

        // 前置探针请求：仅拉取 size 为 1 的数据，轻量级探测后端是否还有待背单词
        WordLearningNetworkHelper.getWordBatch(currentUserId, currentBookId, 1, isReviewMode, new WordLearningNetworkHelper.GetBatchCallback() {
            @Override
            public void onSuccess(List<WordLearningVO> wordList, int offset) {
                isCheckingJump = false;
                if (!isAdded()) return;

                if (wordList == null || wordList.isEmpty()) {
                    // 探针发现已无数据，直接在主页拦截并弹出提示，从根源切断页面闪现
                    String finishMsg = isReviewMode ? "🎉 恭喜！今日复习任务已全部完成！" : "🎉 恭喜！今日新词学习已达标！";
                    Toast.makeText(requireContext(), finishMsg, Toast.LENGTH_SHORT).show();
                } else {
                    // 探针确认有数据，正常放行 UI 跳转
                    WordLearningFragment fragment = WordLearningFragment.newInstance(isReviewMode, targetWords);
                    if (getActivity() != null) {
                        getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, fragment) // 请确保 R.id.fragment_container 是实际容器ID
                                .addToBackStack(null)
                                .commit();
                    }
                }
            }

            @Override
            public void onFailure(String errorMsg) {
                isCheckingJump = false;
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "数据校验失败: " + errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ========================================================================
    // 弹窗与更新业务逻辑
    // ========================================================================
    private void showPlanBottomSheet() {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.layout_change_plan_bottom_sheet, null);
        bottomSheet.setContentView(sheetView);

        EditText etNewWords = sheetView.findViewById(R.id.etNewWords);
        TextView tvReviewWords = sheetView.findViewById(R.id.tvReviewWords);
        RadioGroup rgRatio = sheetView.findViewById(R.id.rgRatio);
        Button btnConfirm = sheetView.findViewById(R.id.btnConfirmPlan);

        final int[] currentRatio = {2};

        rgRatio.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb12) currentRatio[0] = 2;
            else if (checkedId == R.id.rb13) currentRatio[0] = 3;
            else if (checkedId == R.id.rb14) currentRatio[0] = 4;
            updateReviewCalculation(etNewWords, tvReviewWords, currentRatio[0]);
        });

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

        btnConfirm.setOnClickListener(v -> {
            String newWordsStr = etNewWords.getText().toString();
            if (newWordsStr.isEmpty()) {
                Toast.makeText(getContext(), "请输入新词数量", Toast.LENGTH_SHORT).show();
                return;
            }

            btnConfirm.setEnabled(false);

            int newCount = Integer.parseInt(newWordsStr);
            int reviewCount = newCount * currentRatio[0];
            int totalTarget = newCount + reviewCount;

            PlanNetworkHelper.updateDailyTarget(currentUserId, totalTarget, newCount, reviewCount, new PlanNetworkHelper.UpdatePlanCallback() {
                @Override
                public void onSuccess() {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "计划已更新", Toast.LENGTH_SHORT).show();

                    currentDailyNewTarget = newCount;
                    currentDailyReviewTarget = reviewCount;

                    // 同步更新缓存状态，并渲染 UI
                    currentDailyTotalTarget = totalTarget;
                    if (tvTodayWords != null) {
                        tvTodayWords.setText(String.valueOf(currentDailyTotalTarget));
                    }
                    bottomSheet.dismiss();
                }

                @Override
                public void onFailure(String errorMsg) {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "修改失败: " + errorMsg, Toast.LENGTH_SHORT).show();
                    btnConfirm.setEnabled(true);
                }
            });
        });

        bottomSheet.setOnShowListener(dialog -> {
            BottomSheetDialog d = (BottomSheetDialog) dialog;
            View bottomSheetInternal = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheetInternal != null) {
                bottomSheetInternal.setBackgroundResource(android.R.color.transparent);
            }
        });

        bottomSheet.show();
    }

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