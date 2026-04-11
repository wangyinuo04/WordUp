package com.example.wordup;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.common.util.concurrent.ListenableFuture;
import com.wordup.ai.analyzer.FaceMeshAnalyzer;
import com.wordup.ai.analyzer.WordUpAIResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 单词学习页面 Fragment。
 * 已完善：基于确切数据库难度映射(1难, 2中, 3易)的AI情绪实时调度单词难度策略。
 * 修复版：注入全局异步生命周期安全校验防止闪退；并在视图初始化阶段抹除 XML 默认占位数据防止闪现。
 */
public class WordLearningFragment extends Fragment {

    private LinearLayout layoutMainContent;
    private TextView tvProgress, tvWord, tvPhonetic, tvDefinition, tvDifficulty;
    private LinearLayout layoutDefinitionContainer;
    private LinearLayout btnBack, layoutAntiSleep;
    private AppCompatButton btnUnfamiliar, btnFamiliar;
    private View viewClickTarget;
    private TextView tvHintClick;
    private TextView tvChineseMeaning;

    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    private TextView tvTopAlertBanner;

    private LinearLayout layoutAiDisabled;
    private AppCompatButton btnGoToAiSettings;
    private ViewPager2 vpAiPanels;
    private PreviewView previewView;

    // AI面板底部圆点指示器的父容器与子视图数组
    private LinearLayout layoutAiIndicators;
    private View[] indicatorDots;

    private FrameLayout containerCameraFatigue;
    private TextView tvFatigueStatus;
    private TextView tvFatigueScore;
    private TextView tvFatigueHint;

    private FrameLayout containerCameraEmotion;
    private TextView tvEmotionState;

    private float startY;
    private int totalWords = 150;
    private boolean isReviewMode = false;

    private boolean isAIFeatureEnabled = false;
    private boolean isAntiDozeOn = false;
    private boolean isEmotionOn = false;
    private boolean isAiSentenceOn = false;

    private Toast mSingletonToast;
    private long lastClickTime = 0;
    private long lastVibrateTime = 0;

    private List<WordLearningVO> currentBatchList = new ArrayList<>();
    private int currentBatchIndex = 0;
    private int overallStudiedCount = 0;
    private boolean isFetchingBatch = false;

    private long CURRENT_USER_ID;
    private long CURRENT_BOOK_ID;

    // 全局记录当前实时情绪状态
    private String currentEmotionState = "Neutral";

    private ExecutorService cameraExecutor;
    private FaceMeshAnalyzer aiAnalyzer;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    private final Runnable hideAlertRunnable = () -> {
        if (tvTopAlertBanner != null) {
            tvTopAlertBanner.setVisibility(View.GONE);
        }
    };

    public WordLearningFragment() {}

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
        View contentView = inflater.inflate(R.layout.fragment_word_learning, container, false);
        FrameLayout rootWrapper = new FrameLayout(requireContext());
        rootWrapper.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        rootWrapper.addView(contentView);

        rootWrapper.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startY = event.getY();
                    return true;
                case MotionEvent.ACTION_UP:
                    float endY = event.getY();
                    float deltaY = startY - endY;
                    if (deltaY > 100) {
                        if (bottomSheetBehavior != null) {
                            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                        }
                    } else if (deltaY < -100) {
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

        if (getArguments() != null) {
            isReviewMode = getArguments().getBoolean("isReview", false);
            totalWords = getArguments().getInt("total", 150);
        }

        android.content.SharedPreferences prefs = requireActivity().getSharedPreferences("MyAppConfig", Context.MODE_PRIVATE);
        CURRENT_USER_ID = prefs.getLong("userId", -1L);
        CURRENT_BOOK_ID = prefs.getLong("current_book_id", 1L);

        if (CURRENT_USER_ID == -1L) {
            showSafeToast("登录状态异常，请重新登录");
            goBack();
            return;
        }

        initViews(view);
        setupBottomSheet(view);
        setupClickListeners();
        fetchNextBatch();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (CURRENT_USER_ID != -1L) {
            loadAiSettingsAndApply();
        }
    }

    private void loadAiSettingsAndApply() {
        AiNetworkHelper.getAiSettings(CURRENT_USER_ID, new AiNetworkHelper.GetSettingsCallback() {
            @Override
            public void onSuccess(UserPlan plan) {
                // 防止 Fragment 被退栈后触发渲染导致空指针闪退
                if (!isAdded() || getActivity() == null) return;

                isAntiDozeOn = (plan.getAntiSleepOn() != null && plan.getAntiSleepOn() == 1);
                isEmotionOn = (plan.getEmotionRecogOn() != null && plan.getEmotionRecogOn() == 1);
                isAiSentenceOn = (plan.getAiSentenceOn() != null && plan.getAiSentenceOn() == 1);

                isAIFeatureEnabled = isAntiDozeOn || isEmotionOn || isAiSentenceOn;
                applyAiSettingsToUI();
            }

            @Override
            public void onFailure(String errorMsg) {
                if (!isAdded() || getActivity() == null) return;
                showSafeToast("获取 AI 设置失败: " + errorMsg);
            }
        });
    }

    private void applyAiSettingsToUI() {
        if (!isAIFeatureEnabled) {
            if (layoutAiDisabled != null) layoutAiDisabled.setVisibility(View.VISIBLE);
            if (vpAiPanels != null) vpAiPanels.setVisibility(View.GONE);
            if (layoutAiIndicators != null) layoutAiIndicators.setVisibility(View.GONE);

            if (aiAnalyzer != null) {
                aiAnalyzer.setFatigueDetectionEnabled(false);
                aiAnalyzer.setEmotionDetectionEnabled(false);
            }
        } else {
            if (layoutAiDisabled != null) layoutAiDisabled.setVisibility(View.GONE);
            if (vpAiPanels != null) vpAiPanels.setVisibility(View.VISIBLE);
            if (layoutAiIndicators != null) layoutAiIndicators.setVisibility(View.VISIBLE);

            if (aiAnalyzer != null) {
                aiAnalyzer.setFatigueDetectionEnabled(isAntiDozeOn);
                aiAnalyzer.setEmotionDetectionEnabled(isEmotionOn);
            } else {
                checkCameraPermissionAndInit();
            }

            updateAIStatus(1.0f, 0.5f);
        }

        if (vpAiPanels != null && vpAiPanels.getAdapter() != null) {
            vpAiPanels.getAdapter().notifyDataSetChanged();
        }
    }

    private void initViews(View view) {
        layoutMainContent = view.findViewById(R.id.layoutMainContent);
        tvProgress = view.findViewById(R.id.tvProgress);
        tvWord = view.findViewById(R.id.tvWord);
        tvPhonetic = view.findViewById(R.id.tvPhonetic);
        tvDefinition = view.findViewById(R.id.tvDefinition);
        tvDifficulty = view.findViewById(R.id.tvDifficulty);
        layoutDefinitionContainer = view.findViewById(R.id.layoutDefinitionContainer);
        viewClickTarget = view.findViewById(R.id.viewClickTarget);

        btnBack = view.findViewById(R.id.btnBack);
        layoutAntiSleep = view.findViewById(R.id.layoutAntiSleep);
        btnUnfamiliar = view.findViewById(R.id.btnUnfamiliar);
        btnFamiliar = view.findViewById(R.id.btnFamiliar);
        tvHintClick = view.findViewById(R.id.tvHintClick);
        tvChineseMeaning = view.findViewById(R.id.tvChineseMeaning);

        tvTopAlertBanner = view.findViewById(R.id.tvTopAlertBanner);

        vpAiPanels = view.findViewById(R.id.vpAiPanels);
        layoutAiDisabled = view.findViewById(R.id.layoutAiDisabled);
        btnGoToAiSettings = view.findViewById(R.id.btnGoToAiSettings);

        layoutAiIndicators = view.findViewById(R.id.layoutAiIndicators);
        View dotPanel0 = view.findViewById(R.id.dotPanel0);
        View dotPanel1 = view.findViewById(R.id.dotPanel1);
        View dotPanel2 = view.findViewById(R.id.dotPanel2);
        indicatorDots = new View[]{dotPanel0, dotPanel1, dotPanel2};

        previewView = new PreviewView(requireContext());

        // ========================================================================
        // 核心修复：执行视觉静默。清空 XML 中写死的默认占位数据，防止网络请求期间的闪现
        // ========================================================================
        if (tvProgress != null) {
            // 此时 totalWords 已经从 Argument 中获取到了真实的配额（如 80 或 160）
            tvProgress.setText("-/" + totalWords);
        }
        if (tvWord != null) tvWord.setText("");
        if (tvPhonetic != null) tvPhonetic.setText("");
        if (tvDefinition != null) tvDefinition.setText("");
        if (tvDifficulty != null) {
            tvDifficulty.setText("");
            tvDifficulty.setBackgroundResource(0); // 清除死数据的默认背景框
        }
        if (layoutDefinitionContainer != null) {
            layoutDefinitionContainer.setVisibility(View.INVISIBLE);
        }
        if (tvHintClick != null) {
            tvHintClick.setVisibility(View.INVISIBLE);
        }
        // ========================================================================

        if (vpAiPanels != null) {
            vpAiPanels.setAdapter(new AiPagerAdapter());
            vpAiPanels.setOffscreenPageLimit(3);
            vpAiPanels.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    attachPreviewViewToCurrentPage(position);
                    updateIndicators(position);
                }
            });
        }
    }

    private void updateIndicators(int position) {
        if (indicatorDots == null) return;
        for (int i = 0; i < indicatorDots.length; i++) {
            if (indicatorDots[i] != null) {
                if (i == position) {
                    indicatorDots[i].setBackgroundResource(R.drawable.bg_dot_active);
                } else {
                    indicatorDots[i].setBackgroundResource(R.drawable.bg_dot_inactive);
                }
            }
        }
    }

    private void fetchNextBatch() {
        if (isFetchingBatch) return;
        isFetchingBatch = true;

        WordLearningNetworkHelper.getWordBatch(CURRENT_USER_ID, CURRENT_BOOK_ID, 50, isReviewMode, new WordLearningNetworkHelper.GetBatchCallback() {
            @Override
            public void onSuccess(List<WordLearningVO> wordList, int offset) {
                // 如果在网络请求期间用户已退栈或者页面触发销毁操作，则拦截处理逻辑
                if (!isAdded() || getActivity() == null) return;

                isFetchingBatch = false;

                if (wordList == null || wordList.isEmpty()) {
                    String finishMsg = isReviewMode ? "🎉 恭喜！今日复习任务已全部完成！" : "🎉 恭喜！今日新词学习已达标！";
                    showSafeToast(finishMsg);
                    goBack();
                    return;
                }

                overallStudiedCount = offset;
                currentBatchList = wordList;
                currentBatchIndex = 0;
                loadWord(currentBatchIndex);
            }

            @Override
            public void onFailure(String errorMsg) {
                if (!isAdded() || getActivity() == null) return;
                isFetchingBatch = false;
                showSafeToast("获取单词批次失败: " + errorMsg);
            }
        });
    }

    private void applyEmotionScheduling() {
        if (!isEmotionOn || currentBatchList == null || currentBatchIndex + 1 >= currentBatchList.size()) {
            return;
        }

        int targetIndex = -1;

        if ("Positive".equals(currentEmotionState)) {
            for (int i = currentBatchIndex + 1; i < currentBatchList.size(); i++) {
                Integer diff = currentBatchList.get(i).getDifficulty();
                if (diff != null && diff == 1) {
                    targetIndex = i;
                    break;
                }
            }
        } else if ("Negative".equals(currentEmotionState)) {
            for (int i = currentBatchIndex + 1; i < currentBatchList.size(); i++) {
                Integer diff = currentBatchList.get(i).getDifficulty();
                if (diff != null && diff == 3) {
                    targetIndex = i;
                    break;
                }
            }
        }

        if (targetIndex != -1 && targetIndex != currentBatchIndex + 1) {
            WordLearningVO targetWord = currentBatchList.remove(targetIndex);
            currentBatchList.add(currentBatchIndex + 1, targetWord);
        }
    }

    private void submitActionAndNext(boolean isKnown) {
        if (currentBatchList == null || currentBatchList.isEmpty() || currentBatchIndex >= currentBatchList.size()) return;

        WordLearningVO currentWord = currentBatchList.get(currentBatchIndex);

        if (isKnown) {
            showSafeToast("太棒了！继续加油！");
        } else {
            showSafeToast("已加入高频优先复习计划");
        }

        WordLearningNetworkHelper.submitWordAction(CURRENT_USER_ID, currentWord.getWordId(), isKnown, new WordLearningNetworkHelper.SubmitActionCallback() {
            @Override
            public void onSuccess() {}
            @Override
            public void onFailure(String errorMsg) {}
        });

        applyEmotionScheduling();

        nextWord();
    }

    private void nextWord() {
        overallStudiedCount++;
        currentBatchIndex++;

        if (currentBatchIndex < currentBatchList.size()) {
            loadWord(currentBatchIndex);
        } else {
            fetchNextBatch();
        }
    }

    private void setupClickListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (!isFastDoubleClick()) goBack();
            });
        }
        if (btnUnfamiliar != null) {
            btnUnfamiliar.setOnClickListener(v -> {
                if (!isFastDoubleClick()) submitActionAndNext(false);
            });
        }
        if (btnFamiliar != null) {
            btnFamiliar.setOnClickListener(v -> {
                if (!isFastDoubleClick()) submitActionAndNext(true);
            });
        }
        if (viewClickTarget != null && layoutDefinitionContainer != null) {
            viewClickTarget.setOnClickListener(v -> layoutDefinitionContainer.setVisibility(View.VISIBLE));
        }
        if (tvHintClick != null) {
            tvHintClick.setOnClickListener(v -> {
                tvHintClick.setVisibility(View.GONE);
                if (tvChineseMeaning != null) tvChineseMeaning.setVisibility(View.VISIBLE);
                if (layoutDefinitionContainer != null) layoutDefinitionContainer.setVisibility(View.VISIBLE);
            });
        }
        if (btnGoToAiSettings != null) {
            btnGoToAiSettings.setOnClickListener(v -> startActivity(new Intent(requireContext(), AISettingsActivity.class)));
        }
    }

    private boolean isFastDoubleClick() {
        long time = SystemClock.elapsedRealtime();
        if (time - lastClickTime < 300) return true;
        lastClickTime = time;
        return false;
    }

    private void showSafeToast(String message) {
        if (getContext() == null || getActivity() == null) return;
        if (mSingletonToast != null) mSingletonToast.cancel();
        mSingletonToast = Toast.makeText(getContext(), message, Toast.LENGTH_SHORT);
        mSingletonToast.show();
    }

    private void showTopAlert(String message) {
        if (tvTopAlertBanner != null) {
            tvTopAlertBanner.setText(message);
            tvTopAlertBanner.setVisibility(View.VISIBLE);
            tvTopAlertBanner.removeCallbacks(hideAlertRunnable);
            tvTopAlertBanner.postDelayed(hideAlertRunnable, 3000);
        }
    }

    private void loadWord(int index) {
        if (currentBatchList == null || index >= currentBatchList.size()) return;
        WordLearningVO wordVO = currentBatchList.get(index);

        if (tvProgress != null) {
            tvProgress.setText((overallStudiedCount + 1) + "/" + totalWords);
        }

        if (layoutDefinitionContainer != null) layoutDefinitionContainer.setVisibility(View.INVISIBLE);
        if (tvHintClick != null) tvHintClick.setVisibility(View.VISIBLE);
        if (tvChineseMeaning != null) {
            tvChineseMeaning.setVisibility(View.GONE);
            tvChineseMeaning.setText("");
        }

        String wordSpelling = wordVO.getSpelling();
        if (tvWord != null) tvWord.setText(wordSpelling);
        if (tvPhonetic != null) {
            String phonetic = wordVO.getPhonetic();
            tvPhonetic.setText((phonetic != null && !phonetic.isEmpty()) ? phonetic : "");
        }
        if (tvDefinition != null) tvDefinition.setText(wordVO.getTranslation());

        if (tvDifficulty != null) {
            Integer difficultyValue = wordVO.getDifficulty();
            if (difficultyValue != null && difficultyValue == 1) {
                tvDifficulty.setText("难");
                tvDifficulty.setTextColor(Color.parseColor("#FF5252"));
                tvDifficulty.setBackgroundResource(R.drawable.bg_difficulty_tag_hard);
            } else if (difficultyValue != null && difficultyValue == 2) {
                tvDifficulty.setText("中");
                tvDifficulty.setTextColor(Color.parseColor("#FF9800"));
                tvDifficulty.setBackgroundResource(R.drawable.bg_difficulty_tag_middle);
            } else {
                tvDifficulty.setText("易");
                tvDifficulty.setTextColor(Color.parseColor("#4CAF50"));
                tvDifficulty.setBackgroundResource(R.drawable.bg_difficulty_tag_easy);
            }
        }
    }

    private void checkCameraPermissionAndInit() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            initCameraAndAI();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void handlePermissionDenied() {
        showSafeToast("需授予相机权限以启用 AI 监测功能");
        isAntiDozeOn = false;
        isEmotionOn = false;
        isAIFeatureEnabled = isAiSentenceOn;
        applyAiSettingsToUI();
    }

    private void initCameraAndAI() {
        if (cameraExecutor != null) return;
        cameraExecutor = Executors.newSingleThreadExecutor();
        aiAnalyzer = new FaceMeshAnalyzer(requireContext(), result -> {
            if (getActivity() != null && isAdded()) {
                getActivity().runOnUiThread(() -> processAIResult(result));
            }
        });

        aiAnalyzer.setFatigueDetectionEnabled(isAntiDozeOn);
        aiAnalyzer.setEmotionDetectionEnabled(isEmotionOn);

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            // 防止 Fragment 视图被系统销毁时调用 getViewLifecycleOwner() 导致生命周期异常闪退
            if (!isAdded() || getView() == null) return;

            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                imageAnalysis.setAnalyzer(cameraExecutor, aiAnalyzer);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, preview, imageAnalysis);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void processAIResult(WordUpAIResult result) {
        if (!isAIFeatureEnabled || getView() == null || !isAdded()) return;

        if (result.isCalibrating) {
            return;
        }

        if (isAntiDozeOn && result.fatigueLevel == 1) {
            showTopAlert("监测到极度疲劳，请注意休息！");
            triggerVibrationAlert();
        }

        float drowsinessRatio = result.fatigueScore / 100f;
        float emotionRatio;

        if ("Positive".equals(result.emotion)) {
            emotionRatio = 1.0f;
            currentEmotionState = "Positive";
        } else if ("Negative".equals(result.emotion)) {
            emotionRatio = 0.0f;
            currentEmotionState = "Negative";
        } else {
            emotionRatio = 0.5f;
            currentEmotionState = "Neutral";
        }

        updateAIStatus(drowsinessRatio, emotionRatio);
    }

    private void triggerVibrationAlert() {
        long currentTime = SystemClock.elapsedRealtime();
        if (currentTime - lastVibrateTime < 3000) {
            return;
        }
        lastVibrateTime = currentTime;

        if (getContext() == null) return;
        Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(500);
            }
        }
    }

    private void setupBottomSheet(View view) {
        LinearLayout aiBottomSheet = view.findViewById(R.id.aiBottomSheet);
        if (aiBottomSheet == null) return;

        bottomSheetBehavior = BottomSheetBehavior.from(aiBottomSheet);
        bottomSheetBehavior.setHideable(true);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    public void updateAIStatus(float drowsiness, float emotion) {
        if (!isAIFeatureEnabled || getView() == null || !isAdded()) return;

        if (tvFatigueScore != null && tvFatigueStatus != null && tvFatigueHint != null) {
            if (!isAntiDozeOn) {
                tvFatigueStatus.setText("当前状态：未开启");
                tvFatigueStatus.setTextColor(Color.parseColor("#888888"));
                tvFatigueScore.setText("能量分数：--");
                tvFatigueHint.setText("提示信息：请前往 AI设置 中开启");
            } else {
                int score = (int) (drowsiness * 100);
                tvFatigueScore.setText("能量分数：" + score);

                if (score >= 80) {
                    tvFatigueStatus.setText("当前状态：清醒");
                    tvFatigueStatus.setTextColor(Color.parseColor("#4CAF50"));
                    tvFatigueHint.setText("提示信息：专注力极佳，继续保持！");
                } else if (score >= 40) {
                    tvFatigueStatus.setText("当前状态：轻微疲劳");
                    tvFatigueStatus.setTextColor(Color.parseColor("#FF9800"));
                    tvFatigueHint.setText("提示信息：注意眨眼放松，适量补水。");
                } else {
                    tvFatigueStatus.setText("当前状态：极度疲劳");
                    tvFatigueStatus.setTextColor(Color.parseColor("#FF5252"));
                    tvFatigueHint.setText("提示信息：建议立即休息片刻再继续！");
                }
            }
        }

        if (tvEmotionState != null) {
            if (!isEmotionOn) {
                tvEmotionState.setText("当前情绪：未开启");
                tvEmotionState.setTextColor(Color.parseColor("#888888"));
            } else {
                if (emotion >= 0.8f) {
                    tvEmotionState.setText("当前情绪：积极向上 \uD83D\uDE04");
                    tvEmotionState.setTextColor(Color.parseColor("#4CAF50"));
                } else if (emotion <= 0.2f) {
                    tvEmotionState.setText("当前情绪：低落烦躁 \uD83D\uDE1E");
                    tvEmotionState.setTextColor(Color.parseColor("#FF5252"));
                } else {
                    tvEmotionState.setText("当前情绪：平静专注 \uD83D\uDE10");
                    tvEmotionState.setTextColor(Color.parseColor("#333333"));
                }
            }
        }
    }

    private void attachPreviewViewToCurrentPage(int position) {
        if (previewView == null || getView() == null) return;
        ViewGroup parent = (ViewGroup) previewView.getParent();
        if (parent != null) {
            parent.removeView(previewView);
        }
        if (position == 0 && containerCameraFatigue != null) {
            containerCameraFatigue.addView(previewView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } else if (position == 1 && containerCameraEmotion != null) {
            containerCameraEmotion.addView(previewView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }

    private void goBack() {
        if (getActivity() != null && isAdded()) {
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (tvTopAlertBanner != null) {
            tvTopAlertBanner.removeCallbacks(hideAlertRunnable);
        }
        if (cameraExecutor != null) cameraExecutor.shutdown();
    }

    private void generateSentenceWithAI(String words, AppCompatButton btnAiGenerate, TextView tvAiSentenceResult) {
        if (btnAiGenerate != null) {
            btnAiGenerate.setEnabled(false);
            btnAiGenerate.setText("生成中");
        }

        AiNetworkHelper.generateSentenceWithAI(words, new AiNetworkHelper.GenerateSentenceCallback() {
            @Override
            public void onSuccess(String sentence) {
                if (!isAdded() || getActivity() == null) return;
                if (tvAiSentenceResult != null) {
                    tvAiSentenceResult.setTextColor(Color.parseColor("#333333"));
                    tvAiSentenceResult.setText(sentence);
                }
                if (btnAiGenerate != null) {
                    btnAiGenerate.setEnabled(true);
                    btnAiGenerate.setText("AI造句");
                }
            }

            @Override
            public void onFailure(String errorMsg) {
                if (!isAdded() || getActivity() == null) return;
                if (tvAiSentenceResult != null) {
                    tvAiSentenceResult.setTextColor(Color.parseColor("#FF5252"));
                    tvAiSentenceResult.setText("生成失败：" + errorMsg);
                }
                showSafeToast("AI 造句请求异常，请检查网络或配置");
                if (btnAiGenerate != null) {
                    btnAiGenerate.setEnabled(true);
                    btnAiGenerate.setText("重试");
                }
            }
        });
    }

    private class AiPagerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == 0) {
                View v = inflater.inflate(R.layout.layout_ai_panel_fatigue, parent, false);
                return new FatigueViewHolder(v);
            } else if (viewType == 1) {
                View v = inflater.inflate(R.layout.layout_ai_panel_emotion, parent, false);
                return new EmotionViewHolder(v);
            } else {
                View v = inflater.inflate(R.layout.layout_ai_panel_sentence, parent, false);
                return new SentenceViewHolder(v);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (vpAiPanels != null && position == vpAiPanels.getCurrentItem()) {
                attachPreviewViewToCurrentPage(position);
            }

            if (holder instanceof SentenceViewHolder) {
                ((SentenceViewHolder) holder).bind(isAiSentenceOn);
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }
    }

    private class FatigueViewHolder extends RecyclerView.ViewHolder {
        public FatigueViewHolder(@NonNull View itemView) {
            super(itemView);
            containerCameraFatigue = itemView.findViewById(R.id.containerCameraFatigue);
            tvFatigueStatus = itemView.findViewById(R.id.tvFatigueStatus);
            tvFatigueScore = itemView.findViewById(R.id.tvFatigueScore);
            tvFatigueHint = itemView.findViewById(R.id.tvFatigueHint);
        }
    }

    private class EmotionViewHolder extends RecyclerView.ViewHolder {
        public EmotionViewHolder(@NonNull View itemView) {
            super(itemView);
            containerCameraEmotion = itemView.findViewById(R.id.containerCameraEmotion);
            tvEmotionState = itemView.findViewById(R.id.tvEmotionState);
        }
    }

    private class SentenceViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvDisabled;
        private final LinearLayout layoutContent;
        private final EditText etInput;
        private final AppCompatButton btnGenerate;
        private final TextView tvResult;

        public SentenceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDisabled = itemView.findViewById(R.id.tvAiSentenceDisabled);
            layoutContent = itemView.findViewById(R.id.layoutAiSentenceContent);
            etInput = itemView.findViewById(R.id.etAiWordsInput);
            btnGenerate = itemView.findViewById(R.id.btnAiGenerate);
            tvResult = itemView.findViewById(R.id.tvAiSentenceResult);

            if (btnGenerate != null) {
                btnGenerate.setOnClickListener(v -> {
                    String inputWords = etInput.getText().toString().trim();
                    if (inputWords.isEmpty()) {
                        showSafeToast("请输入需要造句的单词");
                        return;
                    }
                    tvResult.setText("请求发送中...");
                    tvResult.setTextColor(Color.parseColor("#888888"));
                    generateSentenceWithAI(inputWords, btnGenerate, tvResult);
                });
            }
        }

        public void bind(boolean isSentenceEnabled) {
            if (isSentenceEnabled) {
                if (tvDisabled != null) tvDisabled.setVisibility(View.GONE);
                if (layoutContent != null) layoutContent.setVisibility(View.VISIBLE);
            } else {
                if (tvDisabled != null) tvDisabled.setVisibility(View.VISIBLE);
                if (layoutContent != null) layoutContent.setVisibility(View.GONE);
            }
        }
    }
}