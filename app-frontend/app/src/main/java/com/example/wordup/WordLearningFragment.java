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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 单词学习页面 Fragment。
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
    private LinearLayout layoutAiEnabled;
    private TextView tvAiDisabled, tvDrowsiness, tvEmotion;
    private ProgressBar pbDrowsiness, pbEmotion;

    private float startY;
    private int totalWords = 150;
    private boolean isReviewMode = false;
    private boolean isAIFeatureEnabled = true;

    private Toast mSingletonToast;
    private long lastClickTime = 0;

    private List<WordLearningVO> currentBatchList = new ArrayList<>();
    private int currentBatchIndex = 0;
    private int overallStudiedCount = 0;
    private boolean isFetchingBatch = false;

    private long CURRENT_USER_ID;
    private long CURRENT_BOOK_ID;

    private ExecutorService cameraExecutor;
    private FaceMeshAnalyzer aiAnalyzer;
    private ActivityResultLauncher<String> requestPermissionLauncher;

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

        android.content.SharedPreferences prefs = requireActivity().getSharedPreferences("MyAppConfig", android.content.Context.MODE_PRIVATE);
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

        if (isAIFeatureEnabled) {
            checkCameraPermissionAndInit();
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

        layoutAiEnabled = view.findViewById(R.id.layoutAiEnabled);
        tvAiDisabled = view.findViewById(R.id.tvAiDisabled);
        tvDrowsiness = view.findViewById(R.id.tvDrowsiness);
        tvEmotion = view.findViewById(R.id.tvEmotion);
        pbDrowsiness = view.findViewById(R.id.pbDrowsiness);
        pbEmotion = view.findViewById(R.id.pbEmotion);

        tvHintClick = view.findViewById(R.id.tvHintClick);
        tvChineseMeaning = view.findViewById(R.id.tvChineseMeaning);
    }

    private void fetchNextBatch() {
        if (isFetchingBatch) return;
        isFetchingBatch = true;

        WordLearningNetworkHelper.getWordBatch(CURRENT_USER_ID, CURRENT_BOOK_ID, 10, isReviewMode, new WordLearningNetworkHelper.GetBatchCallback() {
            @Override
            public void onSuccess(List<WordLearningVO> wordList, int offset) {
                isFetchingBatch = false;

                if (wordList == null || wordList.isEmpty()) {
                    String finishMsg = isReviewMode ? "🎉 恭喜！今日复习任务已全部完成！" : "🎉 恭喜！今日新词学习已达标！";
                    showSafeToast(finishMsg);
                    goBack();
                    return;
                }

                // ========================================================
                // 核心修复：以后端传来的真实完成数量作为进度起点，彻底告别每次都从 1 开始
                // ========================================================
                overallStudiedCount = offset;

                currentBatchList = wordList;
                currentBatchIndex = 0;
                loadWord(currentBatchIndex);
            }

            @Override
            public void onFailure(String errorMsg) {
                isFetchingBatch = false;
                showSafeToast("获取单词批次失败: " + errorMsg);
            }
        });
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
    }

    private boolean isFastDoubleClick() {
        long time = SystemClock.elapsedRealtime();
        if (time - lastClickTime < 300) return true;
        lastClickTime = time;
        return false;
    }

    private void showSafeToast(String message) {
        if (getContext() == null) return;
        if (mSingletonToast != null) mSingletonToast.cancel();
        mSingletonToast = Toast.makeText(getContext(), message, Toast.LENGTH_SHORT);
        mSingletonToast.show();
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
            if (difficultyValue != null && difficultyValue >= 3) {
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
        isAIFeatureEnabled = false;
        if (layoutAiEnabled != null) layoutAiEnabled.setVisibility(View.GONE);
        if (tvAiDisabled != null) {
            tvAiDisabled.setVisibility(View.VISIBLE);
            tvAiDisabled.setText("未授权相机，AI 功能已挂起");
        }
    }

    private void initCameraAndAI() {
        cameraExecutor = Executors.newSingleThreadExecutor();
        aiAnalyzer = new FaceMeshAnalyzer(requireContext(), result -> {
            if (getActivity() != null) getActivity().runOnUiThread(() -> processAIResult(result));
        });
        aiAnalyzer.setFatigueDetectionEnabled(true);
        aiAnalyzer.setEmotionDetectionEnabled(true);

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                imageAnalysis.setAnalyzer(cameraExecutor, aiAnalyzer);
                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, imageAnalysis);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void processAIResult(WordUpAIResult result) {
        if (!isAIFeatureEnabled || getView() == null) return;
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
        if (result.fatigueLevel == 1) showSafeToast("监测到极度疲劳，请注意休息！");
        float drowsinessRatio = (100f - result.fatigueScore) / 100f;
        float emotionRatio = 0.5f;
        if ("Positive".equals(result.emotion)) emotionRatio = 1.0f;
        else if ("Negative".equals(result.emotion)) emotionRatio = 0.0f;
        updateAIStatus(drowsinessRatio, emotionRatio);
    }

    private void setupBottomSheet(View view) {
        LinearLayout aiBottomSheet = view.findViewById(R.id.aiBottomSheet);
        if (aiBottomSheet == null) return;

        bottomSheetBehavior = BottomSheetBehavior.from(aiBottomSheet);
        bottomSheetBehavior.setHideable(true);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    if (layoutMainContent != null) layoutMainContent.setPadding(dpToPx(24), dpToPx(20), dpToPx(24), dpToPx(24));
                }
            }
            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                if (layoutMainContent == null) return;
                float fraction = slideOffset + 1.0f;
                int sheetHeight = bottomSheet.getHeight();
                int currentVisibleHeight = (int) (sheetHeight * (fraction / 2.0f));
                int basePaddingBottom = dpToPx(24);
                int targetPadding = Math.max(basePaddingBottom, currentVisibleHeight);
                layoutMainContent.setPadding(
                        layoutMainContent.getPaddingLeft(),
                        layoutMainContent.getPaddingTop(),
                        layoutMainContent.getPaddingRight(),
                        targetPadding
                );
            }
        });
        if (layoutAiEnabled != null) layoutAiEnabled.setVisibility(isAIFeatureEnabled ? View.VISIBLE : View.GONE);
        if (tvAiDisabled != null) tvAiDisabled.setVisibility(isAIFeatureEnabled ? View.GONE : View.VISIBLE);
    }

    public void updateAIStatus(float drowsiness, float emotion) {
        if (!isAIFeatureEnabled || getView() == null) return;
        if (tvDrowsiness != null) tvDrowsiness.setText(String.format("瞌睡指数: %.2f", drowsiness));
        if (pbDrowsiness != null) pbDrowsiness.setProgress((int) (drowsiness * 100));
        if (tvEmotion != null) tvEmotion.setText(String.format("情绪数值: %.2f", emotion));
        if (pbEmotion != null) pbEmotion.setProgress((int) (emotion * 100));
    }

    private void goBack() {
        if (getActivity() != null) getActivity().getSupportFragmentManager().popBackStack();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cameraExecutor != null) cameraExecutor.shutdown();
    }
}