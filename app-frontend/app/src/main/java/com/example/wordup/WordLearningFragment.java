package com.example.wordup;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.common.util.concurrent.ListenableFuture;
import com.wordup.ai.analyzer.FaceMeshAnalyzer;
import com.wordup.ai.analyzer.WordUpAIResult;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 单词学习页面 Fragment。
 * 负责承载“背诵新词”与“复习旧词”业务逻辑的视图组件，并集成 AI 监控面板与防抖机制。
 * 视图绑定严格依据 fragment_word_learning.xml。
 */
public class WordLearningFragment extends Fragment {

    // 界面核心容器
    private LinearLayout layoutMainContent;

    // 单词卡片相关组件
    private TextView tvProgress, tvWord, tvPhonetic, tvDefinition, tvDifficulty;
    private LinearLayout layoutDefinitionContainer;
    private LinearLayout btnBack, layoutAntiSleep;
    private AppCompatButton btnUnfamiliar, btnFamiliar;
    private View viewClickTarget;

    // 提示文字与中文释义
    private TextView tvHintClick;
    private TextView tvChineseMeaning;

    // AI 监控面板组件（来源于被引用的 layout_ai_status_sheet）
    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    private LinearLayout layoutAiEnabled;
    private TextView tvAiDisabled, tvDrowsiness, tvEmotion;
    private ProgressBar pbDrowsiness, pbEmotion;

    // 交互逻辑与状态数据声明
    private float startY;
    private int currentPosition = 0;
    private int totalWords = 150;
    private boolean isReviewMode = false;
    private boolean isAIFeatureEnabled = true;

    // 性能优化控制参数
    private Toast mSingletonToast;
    private long lastClickTime = 0;

    // AI 分析器与相机线程池声明
    private ExecutorService cameraExecutor;
    private FaceMeshAnalyzer aiAnalyzer;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    public WordLearningFragment() {
        // Fragment 要求的空构造函数
    }

    /**
     * 实例化 Fragment 组件并传递状态参数。
     */
    public static WordLearningFragment newInstance(boolean isReview, int total) {
        WordLearningFragment fragment = new WordLearningFragment();
        Bundle args = new Bundle();
        args.putBoolean("isReview", isReview);
        args.putInt("total", total);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 注册动态权限请求回调（必须在 onCreate 等早期生命周期阶段执行）
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        initCameraAndAI();
                    } else {
                        handlePermissionDenied();
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 渲染基础布局
        View contentView = inflater.inflate(R.layout.fragment_word_learning, container, false);

        // 创建全屏触摸拦截器
        FrameLayout rootWrapper = new FrameLayout(requireContext());
        rootWrapper.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        rootWrapper.addView(contentView);

        // 针对模拟器优化的滑动算法
        rootWrapper.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startY = event.getY();
                    return true;
                case MotionEvent.ACTION_UP:
                    float endY = event.getY();
                    float deltaY = startY - endY; // 差值为正表示向上滑

                    if (deltaY > 100) {
                        // 上划动作：展开 AI 面板
                        if (bottomSheetBehavior != null) {
                            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                        }
                    } else if (deltaY < -100) {
                        // 下划动作：隐藏 AI 面板
                        if (bottomSheetBehavior != null) {
                            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                        }
                    }
                    return true;
            }
            return false;
        });

        return rootWrapper;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 初始化参数
        if (getArguments() != null) {
            isReviewMode = getArguments().getBoolean("isReview", false);
            totalWords = getArguments().getInt("total", 150);
        }

        initViews(view);
        setupBottomSheet(view);
        setupClickListeners();
        loadWord(currentPosition);

        // 检查权限并按需启动 CameraX 与 AI 监测流
        if (isAIFeatureEnabled) {
            checkCameraPermissionAndInit();
        }
    }

    /**
     * 初始化视图组件引用
     */
    private void initViews(View view) {
        layoutMainContent = view.findViewById(R.id.layoutMainContent);

        // 基础卡片组件
        tvProgress = view.findViewById(R.id.tvProgress);
        tvWord = view.findViewById(R.id.tvWord);
        tvPhonetic = view.findViewById(R.id.tvPhonetic);
        tvDefinition = view.findViewById(R.id.tvDefinition);
        tvDifficulty = view.findViewById(R.id.tvDifficulty);
        layoutDefinitionContainer = view.findViewById(R.id.layoutDefinitionContainer);
        viewClickTarget = view.findViewById(R.id.viewClickTarget);

        // 按钮与功能容器
        btnBack = view.findViewById(R.id.btnBack);
        layoutAntiSleep = view.findViewById(R.id.layoutAntiSleep);
        btnUnfamiliar = view.findViewById(R.id.btnUnfamiliar);
        btnFamiliar = view.findViewById(R.id.btnFamiliar);

        // AI 监控面板组件
        layoutAiEnabled = view.findViewById(R.id.layoutAiEnabled);
        tvAiDisabled = view.findViewById(R.id.tvAiDisabled);
        tvDrowsiness = view.findViewById(R.id.tvDrowsiness);
        tvEmotion = view.findViewById(R.id.tvEmotion);
        pbDrowsiness = view.findViewById(R.id.pbDrowsiness);
        pbEmotion = view.findViewById(R.id.pbEmotion);

        // 绑定提示文字与中文释义控件
        tvHintClick = view.findViewById(R.id.tvHintClick);
        tvChineseMeaning = view.findViewById(R.id.tvChineseMeaning);
    }

    /**
     * 检查相机权限。若已授权则直接启动 AI 流；若未授权则发起动态申请。
     */
    private void checkCameraPermissionAndInit() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            initCameraAndAI();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    /**
     * 处理用户拒绝相机权限的逻辑
     */
    private void handlePermissionDenied() {
        showSafeToast("需授予相机权限以启用 AI 监测功能");
        isAIFeatureEnabled = false;
        if (layoutAiEnabled != null) layoutAiEnabled.setVisibility(View.GONE);
        if (tvAiDisabled != null) {
            tvAiDisabled.setVisibility(View.VISIBLE);
            tvAiDisabled.setText("未授权相机，AI 功能已挂起");
        }
    }

    /**
     * 初始化 CameraX 图像分析流与 FaceMeshAnalyzer
     */
    private void initCameraAndAI() {
        // 创建独立的单线程池供图像分析使用，防止阻塞主线程
        cameraExecutor = Executors.newSingleThreadExecutor();

        // 实例化核心分析器
        aiAnalyzer = new FaceMeshAnalyzer(requireContext(), result -> {
            // SDK 回调位于子线程，须切换至主线程执行 UI 更新
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> processAIResult(result));
            }
        });

        // 开启双路监测功能
        aiAnalyzer.setFatigueDetectionEnabled(true);
        aiAnalyzer.setEmotionDetectionEnabled(true);

        // 配置 CameraX 图像分析流
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // 设定分析流策略：仅保留最新帧，丢弃过期帧以降低延迟
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                // 将分析器绑定至图像流
                imageAnalysis.setAnalyzer(cameraExecutor, aiAnalyzer);

                // 默认调用前置摄像头
                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

                cameraProvider.unbindAll();
                // 绑定生命周期，CameraX 将自动管理相机的开启与关闭
                cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    /**
     * 解析 SDK 回调数据并驱动视图更新
     */
    private void processAIResult(WordUpAIResult result) {
        if (!isAIFeatureEnabled || getView() == null) return;

        // 处理校准状态拦截
        if (result.isCalibrating) {
            if (tvAiDisabled != null) {
                tvAiDisabled.setVisibility(View.VISIBLE);
                tvAiDisabled.setText("正在校准眼部基准... " + result.calibrationProgress + "%");
            }
            if (layoutAiEnabled != null) layoutAiEnabled.setVisibility(View.GONE);
            return;
        } else {
            if (tvAiDisabled != null) tvAiDisabled.setVisibility(View.GONE);
            if (layoutAiEnabled != null) layoutAiEnabled.setVisibility(View.VISIBLE);
        }

        // 1. 处理疲劳预警逻辑
        if (result.fatigueLevel == 1) {
            showSafeToast("监测到极度疲劳，请注意休息！");
        }

        // 换算疲劳指数为百分比浮点数供进度条使用 (能量值 score 越低越疲劳，此处做反转适配 UI)
        float drowsinessRatio = (100f - result.fatigueScore) / 100f;

        // 2. 处理情绪映射逻辑
        float emotionRatio = 0.5f; // 默认中性
        if ("Positive".equals(result.emotion)) {
            emotionRatio = 1.0f;
        } else if ("Negative".equals(result.emotion)) {
            emotionRatio = 0.0f;
        }

        // 执行底层视图渲染
        updateAIStatus(drowsinessRatio, emotionRatio);
    }

    /**
     * 配置 AI 面板及界面联动压缩逻辑
     */
    private void setupBottomSheet(View view) {
        LinearLayout aiBottomSheet = view.findViewById(R.id.aiBottomSheet);
        if (aiBottomSheet == null) return;

        bottomSheetBehavior = BottomSheetBehavior.from(aiBottomSheet);

        // 初始状态设为隐藏
        bottomSheetBehavior.setHideable(true);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        // 核心逻辑：监听滑动并压缩主界面
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    // 隐藏时重置 Padding
                    if (layoutMainContent != null) {
                        layoutMainContent.setPadding(dpToPx(24), dpToPx(20), dpToPx(24), dpToPx(24));
                    }
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                if (layoutMainContent == null) return;

                // slideOffset 范围: -1.0(HIDDEN) -> 0.0(COLLAPSED) -> 1.0(EXPANDED)
                float fraction = slideOffset + 1.0f; // 归一化为 0.0 到 2.0
                int sheetHeight = bottomSheet.getHeight();

                // 根据面板升起的高度动态增加主布局底部的 Padding，从而挤压单词卡片
                int currentVisibleHeight = (int) (sheetHeight * (fraction / 2.0f));
                int basePaddingBottom = dpToPx(24); // 原始底部 padding

                int targetPadding = Math.max(basePaddingBottom, currentVisibleHeight);
                layoutMainContent.setPadding(
                        layoutMainContent.getPaddingLeft(),
                        layoutMainContent.getPaddingTop(),
                        layoutMainContent.getPaddingRight(),
                        targetPadding
                );
            }
        });

        // 根据 AI 功能开关切换内部视图
        if (layoutAiEnabled != null) layoutAiEnabled.setVisibility(isAIFeatureEnabled ? View.VISIBLE : View.GONE);
        if (tvAiDisabled != null) tvAiDisabled.setVisibility(isAIFeatureEnabled ? View.GONE : View.VISIBLE);
    }

    /**
     * 绑定各项点击事件处理逻辑，强制进行防抖拦截
     */
    private void setupClickListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (!isFastDoubleClick()) goBack();
            });
        }

        if (btnUnfamiliar != null) {
            btnUnfamiliar.setOnClickListener(v -> {
                if (!isFastDoubleClick()) {
                    showSafeToast("已加入复习计划");
                    nextWord();
                }
            });
        }

        if (btnFamiliar != null) {
            btnFamiliar.setOnClickListener(v -> {
                if (!isFastDoubleClick()) {
                    showSafeToast("太棒了！继续加油！");
                    nextWord();
                }
            });
        }

        if (viewClickTarget != null && layoutDefinitionContainer != null) {
            viewClickTarget.setOnClickListener(v -> {
                layoutDefinitionContainer.setVisibility(View.VISIBLE);
            });
        }

        if (tvHintClick != null) {
            tvHintClick.setOnClickListener(v -> {
                tvHintClick.setVisibility(View.GONE);
                if (tvChineseMeaning != null) {
                    tvChineseMeaning.setVisibility(View.VISIBLE);
                }
                if (layoutDefinitionContainer != null) {
                    layoutDefinitionContainer.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    /**
     * 防抖判定逻辑：低于 300 毫秒的连续点击均视为无效拦截
     */
    private boolean isFastDoubleClick() {
        long time = SystemClock.elapsedRealtime();
        if (time - lastClickTime < 300) {
            return true;
        }
        lastClickTime = time;
        return false;
    }

    /**
     * 安全展示单例 Toast，防止排队与内存泄漏
     */
    private void showSafeToast(String message) {
        if (getContext() == null) return;
        if (mSingletonToast != null) {
            mSingletonToast.cancel();
        }
        mSingletonToast = Toast.makeText(getContext(), message, Toast.LENGTH_SHORT);
        mSingletonToast.show();
    }

    /**
     * 渲染单词数据及难度标签
     */
    private void loadWord(int position) {
        currentPosition = position;
        if (tvProgress != null) {
            tvProgress.setText((currentPosition + 1) + "/" + totalWords);
        }

        if (layoutDefinitionContainer != null) {
            layoutDefinitionContainer.setVisibility(View.INVISIBLE);
        }

        if (tvHintClick != null) {
            tvHintClick.setVisibility(View.VISIBLE);
        }
        if (tvChineseMeaning != null) {
            tvChineseMeaning.setVisibility(View.GONE);
            tvChineseMeaning.setText("");
        }

        // 单词数据赋值（实际开发应对接数据层接口）
        String word = "ancestry";
        if (tvWord != null) tvWord.setText(word);
        if (tvPhonetic != null) tvPhonetic.setText("/ˈænsestri/");
        if (tvDefinition != null) tvDefinition.setText("n. 祖先；血统");

        // 难度标注逻辑判定
        if (tvDifficulty != null) {
            if (word.length() > 7) {
                tvDifficulty.setText("难");
                tvDifficulty.setTextColor(Color.parseColor("#FF5252"));
                tvDifficulty.setBackgroundResource(R.drawable.bg_difficulty_tag_hard);
            } else {
                tvDifficulty.setText("易");
                tvDifficulty.setTextColor(Color.parseColor("#4CAF50"));
                tvDifficulty.setBackgroundResource(R.drawable.bg_difficulty_tag_easy);
            }
        }
    }

    /**
     * 实时更新 AI 监测数值
     * @param drowsiness 瞌睡数值 (0.0 - 1.0)
     * @param emotion 情绪数值 (0.0 - 1.0)
     */
    public void updateAIStatus(float drowsiness, float emotion) {
        if (!isAIFeatureEnabled || getView() == null) return;

        if (tvDrowsiness != null) tvDrowsiness.setText(String.format("瞌睡指数: %.2f", drowsiness));
        if (pbDrowsiness != null) pbDrowsiness.setProgress((int) (drowsiness * 100));

        if (tvEmotion != null) tvEmotion.setText(String.format("情绪数值: %.2f", emotion));
        if (pbEmotion != null) pbEmotion.setProgress((int) (emotion * 100));
    }

    private void nextWord() {
        if (currentPosition < totalWords - 1) {
            loadWord(currentPosition + 1);
        } else {
            showSafeToast("🎉 恭喜完成今日学习！");
            goBack();
        }
    }

    private void goBack() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }

    /**
     * 辅助工具：dp 单位转 px 像素
     */
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 关闭线程池以释放系统资源
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}