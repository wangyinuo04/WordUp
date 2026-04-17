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
import android.util.Log;
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

import com.example.wordup.db.AppDatabase;
import com.example.wordup.db.dao.LocalDataDao;
import com.example.wordup.db.entity.LocalUserWordRecord;
import com.example.wordup.db.entity.LocalWord;
import com.example.wordup.db.sync.DataSyncManager;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.common.util.concurrent.ListenableFuture;
import com.wordup.ai.analyzer.FaceMeshAnalyzer;
import com.wordup.ai.analyzer.WordUpAIResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 单词学习页面 Fragment。
 * 业务逻辑说明：
 * 1. 采用高灵敏度报警策略：极度疲劳判定同时监听 fatigueLevel 标志位与 statusMessage 文本。
 * 2. UI 状态同步：基于 statusMessage 实时渲染学习者的专注状态。
 * 3. 统计上报：自动聚合单次学习周期内的 AI 干预数据并上报至服务器。
 * 4. 离线架构：采用纯本地 SQLite 驱动背词流程，内置艾宾浩斯调度引擎，支持后台静默数据同步。
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

    private String currentEmotionState = "Neutral";

    private int sessionSleepyCount = 0;
    private int sessionUnfocusedCount = 0;
    private int sessionAiHardWords = 0;

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

            updateAIStatus("✅ 状态清醒", 0, 100, 0.5f);
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

        if (tvProgress != null) {
            tvProgress.setText("-/" + totalWords);
        }

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

    /**
     * 【本地数据库提取】执行离线单词池调度计算
     */
    private void fetchNextBatch() {
        if (isFetchingBatch) return;
        isFetchingBatch = true;

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                LocalDataDao dao = AppDatabase.getInstance(requireContext()).localDataDao();
                List<LocalWord> allWords = dao.getWordsByBookId(CURRENT_BOOK_ID);
                List<LocalUserWordRecord> userRecords = dao.getUserRecords(CURRENT_USER_ID);

                Map<Long, LocalUserWordRecord> recordMap = new HashMap<>();
                if (userRecords != null) {
                    for (LocalUserWordRecord r : userRecords) {
                        recordMap.put(r.word_id, r);
                    }
                }

                long currentTime = System.currentTimeMillis();
                List<WordLearningVO> batch = new ArrayList<>();
                int offsetCount = 0;

                // 本地艾宾浩斯双轨制状态过滤引擎
                for (LocalWord w : allWords) {
                    LocalUserWordRecord r = recordMap.get(w.id);
                    boolean isLearned = (r != null);

                    if (isReviewMode) {
                        if (isLearned && r.learn_status != null && r.learn_status == 1) {
                            if (r.next_review_time != null && r.next_review_time <= currentTime) {
                                if (batch.size() < 50) batch.add(mapToVO(w));
                            } else {
                                offsetCount++;
                            }
                        } else if (isLearned && r.learn_status != null && r.learn_status == 2) {
                            offsetCount++;
                        }
                    } else {
                        if (!isLearned) {
                            if (batch.size() < 50) batch.add(mapToVO(w));
                        } else {
                            offsetCount++;
                        }
                    }
                }

                final int finalOffset = offsetCount;
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        isFetchingBatch = false;
                        if (batch.isEmpty()) {
                            String finishMsg = isReviewMode ? "今日复习任务已完成！" : "今日学习任务已达标！";
                            showSafeToast(finishMsg);
                            goBack();
                            return;
                        }

                        overallStudiedCount = finalOffset;
                        currentBatchList = batch;
                        currentBatchIndex = 0;
                        loadWord(currentBatchIndex);
                    });
                }
            } catch (Exception e) {
                Log.e("WordLearning", "读取本地缓存异常", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        isFetchingBatch = false;
                        showSafeToast("本地缓存读取失败: " + e.getMessage());
                    });
                }
            }
        });
    }

    private WordLearningVO mapToVO(LocalWord lw) {
        WordLearningVO vo = new WordLearningVO();
        vo.setWordId(lw.id);
        vo.setSpelling(lw.spelling);
        vo.setPhonetic(lw.phonetic);
        vo.setTranslation(lw.translation);
        vo.setDifficulty(lw.difficulty);
        return vo;
    }

    private void applyEmotionScheduling() {
        if (!isEmotionOn || currentBatchList == null || currentBatchIndex + 1 >= currentBatchList.size()) return;
        int targetIndex = -1;
        if ("Positive".equals(currentEmotionState)) {
            for (int i = currentBatchIndex + 1; i < currentBatchList.size(); i++) {
                if (currentBatchList.get(i).getDifficulty() != null && currentBatchList.get(i).getDifficulty() == 1) {
                    targetIndex = i;
                    break;
                }
            }
        } else if ("Negative".equals(currentEmotionState)) {
            for (int i = currentBatchIndex + 1; i < currentBatchList.size(); i++) {
                if (currentBatchList.get(i).getDifficulty() != null && currentBatchList.get(i).getDifficulty() == 3) {
                    targetIndex = i;
                    break;
                }
            }
        }
        if (targetIndex != -1 && targetIndex != currentBatchIndex + 1) {
            WordLearningVO targetWord = currentBatchList.remove(targetIndex);
            currentBatchList.add(currentBatchIndex + 1, targetWord);
            if ("Positive".equals(currentEmotionState) && targetWord.getDifficulty() == 1) sessionAiHardWords++;
        }
    }

    /**
     * 【本地数据库注入】基于艾宾浩斯曲线写入本地 SQLite，并触发队列异步上传
     */
    private void submitActionAndNext(boolean isKnown) {
        if (currentBatchList == null || currentBatchList.isEmpty() || currentBatchIndex >= currentBatchList.size()) return;

        WordLearningVO currentWord = currentBatchList.get(currentBatchIndex);

        showSafeToast(isKnown ? "太棒了！继续加油！" : "已加入高频优先复习计划");

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                LocalDataDao dao = AppDatabase.getInstance(requireContext()).localDataDao();
                LocalUserWordRecord record = dao.getUserRecord(CURRENT_USER_ID, currentWord.getWordId());
                long now = System.currentTimeMillis();

                if (record == null) {
                    record = new LocalUserWordRecord();
                    record.user_id = CURRENT_USER_ID;
                    record.word_id = currentWord.getWordId();
                    record.learn_status = 1;
                    record.sync_status = 1;
                    if (isKnown) {
                        record.current_stage = 2;
                        record.next_review_time = now + 86400000L; // 延迟1天
                    } else {
                        record.current_stage = 1;
                        record.next_review_time = now; // 立即安排复习
                    }
                    dao.insertRecords(Collections.singletonList(record));
                } else {
                    record.sync_status = 1;
                    if (isKnown) {
                        int nextStage = Math.min(record.current_stage + 1, 5);
                        record.current_stage = nextStage;
                        record.next_review_time = calculateNextTime(nextStage, now);
                        if (nextStage == 5) {
                            record.learn_status = 2; // 满级标记为已掌握
                        }
                    } else {
                        record.current_stage = 1;
                        record.next_review_time = now;
                    }
                    dao.updateRecord(record);
                }

                // 触发数据同步层的增量上传
                DataSyncManager.getInstance(requireContext()).pushLocalProgressToCloud(CURRENT_USER_ID);

            } catch (Exception e) {
                Log.e("WordLearning", "离线进度保存异常: " + e.getMessage());
            }
        });

        applyEmotionScheduling();
        nextWord();
    }

    private long calculateNextTime(int stage, long baseTime) {
        switch (stage) {
            case 2: return baseTime + 86400000L;   // 1天
            case 3: return baseTime + 172800000L;  // 2天
            case 4: return baseTime + 345600000L;  // 4天
            case 5: return baseTime + 604800000L;  // 7天
            default: return baseTime;
        }
    }

    private void nextWord() {
        overallStudiedCount++;
        currentBatchIndex++;
        if (currentBatchIndex < currentBatchList.size()) loadWord(currentBatchIndex);
        else fetchNextBatch();
    }

    private void setupClickListeners() {
        if (btnBack != null) btnBack.setOnClickListener(v -> { if (!isFastDoubleClick()) goBack(); });
        if (btnUnfamiliar != null) btnUnfamiliar.setOnClickListener(v -> { if (!isFastDoubleClick()) submitActionAndNext(false); });
        if (btnFamiliar != null) btnFamiliar.setOnClickListener(v -> { if (!isFastDoubleClick()) submitActionAndNext(true); });
        if (viewClickTarget != null) viewClickTarget.setOnClickListener(v -> { if (layoutDefinitionContainer != null) layoutDefinitionContainer.setVisibility(View.VISIBLE); });
        if (tvHintClick != null) tvHintClick.setOnClickListener(v -> {
            tvHintClick.setVisibility(View.GONE);
            if (tvChineseMeaning != null) tvChineseMeaning.setVisibility(View.VISIBLE);
            if (layoutDefinitionContainer != null) layoutDefinitionContainer.setVisibility(View.VISIBLE);
        });
        if (btnGoToAiSettings != null) btnGoToAiSettings.setOnClickListener(v -> startActivity(new Intent(requireContext(), AISettingsActivity.class)));
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
        if (tvProgress != null) tvProgress.setText((overallStudiedCount + 1) + "/" + totalWords);
        if (layoutDefinitionContainer != null) layoutDefinitionContainer.setVisibility(View.INVISIBLE);
        if (tvHintClick != null) tvHintClick.setVisibility(View.VISIBLE);
        if (tvChineseMeaning != null) { tvChineseMeaning.setVisibility(View.GONE); tvChineseMeaning.setText(""); }
        if (tvWord != null) tvWord.setText(wordVO.getSpelling());
        if (tvPhonetic != null) tvPhonetic.setText(wordVO.getPhonetic() != null ? wordVO.getPhonetic() : "");
        if (tvDefinition != null) tvDefinition.setText(wordVO.getTranslation());
        if (tvDifficulty != null) {
            Integer diff = wordVO.getDifficulty();
            if (diff != null && diff == 1) {
                tvDifficulty.setText("难");
                tvDifficulty.setTextColor(Color.parseColor("#FF5252"));
                tvDifficulty.setBackgroundResource(R.drawable.bg_difficulty_tag_hard);
            } else if (diff != null && diff == 2) {
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
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) initCameraAndAI();
        else requestPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private void handlePermissionDenied() {
        showSafeToast("需授予相机权限以启用 AI 监测功能");
        isAntiDozeOn = false; isEmotionOn = false; isAIFeatureEnabled = isAiSentenceOn;
        applyAiSettingsToUI();
    }

    private void initCameraAndAI() {
        if (cameraExecutor != null) return;
        cameraExecutor = Executors.newSingleThreadExecutor();
        aiAnalyzer = new FaceMeshAnalyzer(requireContext(), result -> {
            if (getActivity() != null && isAdded()) getActivity().runOnUiThread(() -> processAIResult(result));
        });
        aiAnalyzer.setFatigueDetectionEnabled(isAntiDozeOn);
        aiAnalyzer.setEmotionDetectionEnabled(isEmotionOn);

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            if (!isAdded() || getView() == null) return;
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
                imageAnalysis.setAnalyzer(cameraExecutor, aiAnalyzer);
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(getViewLifecycleOwner(), CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalysis);
            } catch (ExecutionException | InterruptedException e) { e.printStackTrace(); }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    /**
     * AI 结果核心处理方法。
     * 极度疲劳判定同时监听 fatigueLevel 标志位与 statusMessage，提升预警即时性。
     */
    private void processAIResult(WordUpAIResult result) {
        if (!isAIFeatureEnabled || getView() == null || !isAdded()) return;
        if (result.isCalibrating) return;

        if (isAntiDozeOn) {
            boolean isFatiguedByMessage = result.statusMessage != null && result.statusMessage.contains("持续闭眼报警");
            if (result.fatigueLevel == 1 || isFatiguedByMessage) {
                sessionSleepyCount++;
                showTopAlert("极度疲劳");
                triggerVibrationAlert();
            } else if (result.statusMessage != null && result.statusMessage.contains("未检测到人脸")) {
                sessionUnfocusedCount++;
                showTopAlert("未检测到人脸");
                triggerVibrationAlert();
            }
        }

        float emotionRatio;
        if ("Positive".equals(result.emotion)) { emotionRatio = 1.0f; currentEmotionState = "Positive"; }
        else if ("Negative".equals(result.emotion)) { emotionRatio = 0.0f; currentEmotionState = "Negative"; }
        else { emotionRatio = 0.5f; currentEmotionState = "Neutral"; }

        updateAIStatus(result.statusMessage, result.fatigueLevel, result.fatigueScore, emotionRatio);
    }

    private void triggerVibrationAlert() {
        long currentTime = SystemClock.elapsedRealtime();
        if (currentTime - lastVibrateTime < 3000) return;
        lastVibrateTime = currentTime;
        if (getContext() == null) return;
        Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            else vibrator.vibrate(500);
        }
    }

    private void setupBottomSheet(View view) {
        LinearLayout aiBottomSheet = view.findViewById(R.id.aiBottomSheet);
        if (aiBottomSheet == null) return;
        bottomSheetBehavior = BottomSheetBehavior.from(aiBottomSheet);
        bottomSheetBehavior.setHideable(true);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    public void updateAIStatus(String statusMessage, int fatigueLevel, int fatigueScore, float emotion) {
        if (!isAIFeatureEnabled || getView() == null || !isAdded()) return;

        if (tvFatigueScore != null && tvFatigueStatus != null && tvFatigueHint != null) {
            if (!isAntiDozeOn) {
                tvFatigueStatus.setText("当前状态：未开启");
                tvFatigueStatus.setTextColor(Color.parseColor("#888888"));
                tvFatigueScore.setText("能量分数：--");
            } else {
                tvFatigueScore.setText("能量分数：" + fatigueScore);
                boolean isFatiguedByMessage = statusMessage != null && statusMessage.contains("持续闭眼报警");
                if (fatigueLevel == 1 || isFatiguedByMessage) {
                    tvFatigueStatus.setText("当前状态：极度疲劳");
                    tvFatigueStatus.setTextColor(Color.parseColor("#FF5252"));
                    tvFatigueHint.setText("提示信息：立即休息，避免过度疲劳！");
                } else {
                    tvFatigueStatus.setText("当前状态：清醒");
                    tvFatigueStatus.setTextColor(Color.parseColor("#4CAF50"));
                    tvFatigueHint.setText("提示信息：" + (statusMessage != null ? statusMessage : "状态良好"));
                }
            }
        }

        if (tvEmotionState != null) {
            if (!isEmotionOn) {
                tvEmotionState.setText("当前情绪：未开启");
                tvEmotionState.setTextColor(Color.parseColor("#888888"));
            } else {
                if (emotion >= 0.8f) { tvEmotionState.setText("当前情绪：积极向上"); tvEmotionState.setTextColor(Color.parseColor("#4CAF50")); }
                else if (emotion <= 0.2f) { tvEmotionState.setText("当前情绪：低落烦躁"); tvEmotionState.setTextColor(Color.parseColor("#FF5252")); }
                else { tvEmotionState.setText("当前情绪：平静专注"); tvEmotionState.setTextColor(Color.parseColor("#333333")); }
            }
        }
    }

    private void attachPreviewViewToCurrentPage(int position) {
        if (previewView == null || getView() == null) return;
        ViewGroup parent = (ViewGroup) previewView.getParent();
        if (parent != null) parent.removeView(previewView);
        if (position == 0 && containerCameraFatigue != null) containerCameraFatigue.addView(previewView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        else if (position == 1 && containerCameraEmotion != null) containerCameraEmotion.addView(previewView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void uploadAiStatsSession() {
        if (sessionSleepyCount > 0 || sessionUnfocusedCount > 0 || sessionAiHardWords > 0) {
            AiNetworkHelper.updateAiStatsSession(CURRENT_USER_ID, sessionSleepyCount, sessionUnfocusedCount, sessionAiHardWords, new AiNetworkHelper.UpdateStatsCallback() {
                @Override public void onSuccess() { Log.d("WordLearning", "AI 统计上报成功"); }
                @Override public void onFailure(String errorMsg) { Log.e("WordLearning", "AI 统计上报失败: " + errorMsg); }
            });
            sessionSleepyCount = 0; sessionUnfocusedCount = 0; sessionAiHardWords = 0;
        }
    }

    private void goBack() { if (getActivity() != null && isAdded()) getActivity().getSupportFragmentManager().popBackStack(); }

    @Override
    public void onDestroyView() {
        uploadAiStatsSession();
        super.onDestroyView();
        if (tvTopAlertBanner != null) tvTopAlertBanner.removeCallbacks(hideAlertRunnable);
        if (cameraExecutor != null) cameraExecutor.shutdown();
    }

    private void generateSentenceWithAI(String words, AppCompatButton btnAiGenerate, TextView tvAiSentenceResult) {
        if (btnAiGenerate != null) { btnAiGenerate.setEnabled(false); btnAiGenerate.setText("生成中"); }
        AiNetworkHelper.generateSentenceWithAI(words, new AiNetworkHelper.GenerateSentenceCallback() {
            @Override
            public void onSuccess(String sentence) {
                if (!isAdded() || getActivity() == null) return;
                if (tvAiSentenceResult != null) { tvAiSentenceResult.setTextColor(Color.parseColor("#333333")); tvAiSentenceResult.setText(sentence); }
                if (btnAiGenerate != null) { btnAiGenerate.setEnabled(true); btnAiGenerate.setText("AI造句"); }
            }
            @Override
            public void onFailure(String errorMsg) {
                if (!isAdded() || getActivity() == null) return;
                if (tvAiSentenceResult != null) { tvAiSentenceResult.setTextColor(Color.parseColor("#FF5252")); tvAiSentenceResult.setText("生成失败：" + errorMsg); }
                if (btnAiGenerate != null) { btnAiGenerate.setEnabled(true); btnAiGenerate.setText("重试"); }
            }
        });
    }

    private class AiPagerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == 0) return new FatigueViewHolder(inflater.inflate(R.layout.layout_ai_panel_fatigue, parent, false));
            else if (viewType == 1) return new EmotionViewHolder(inflater.inflate(R.layout.layout_ai_panel_emotion, parent, false));
            else return new SentenceViewHolder(inflater.inflate(R.layout.layout_ai_panel_sentence, parent, false));
        }
        @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (vpAiPanels != null && position == vpAiPanels.getCurrentItem()) attachPreviewViewToCurrentPage(position);
            if (holder instanceof SentenceViewHolder) ((SentenceViewHolder) holder).bind(isAiSentenceOn);
        }
        @Override public int getItemCount() { return 3; }
        @Override public int getItemViewType(int position) { return position; }
    }

    private class FatigueViewHolder extends RecyclerView.ViewHolder {
        public FatigueViewHolder(@NonNull View v) {
            super(v);
            containerCameraFatigue = v.findViewById(R.id.containerCameraFatigue);
            tvFatigueStatus = v.findViewById(R.id.tvFatigueStatus);
            tvFatigueScore = v.findViewById(R.id.tvFatigueScore);
            tvFatigueHint = v.findViewById(R.id.tvFatigueHint);
        }
    }

    private class EmotionViewHolder extends RecyclerView.ViewHolder {
        public EmotionViewHolder(@NonNull View v) {
            super(v);
            containerCameraEmotion = v.findViewById(R.id.containerCameraEmotion);
            tvEmotionState = v.findViewById(R.id.tvEmotionState);
        }
    }

    private class SentenceViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDisabled, tvResult;
        private final LinearLayout layoutContent;
        private final EditText etInput;
        private final AppCompatButton btnGenerate;
        public SentenceViewHolder(@NonNull View v) {
            super(v);
            tvDisabled = v.findViewById(R.id.tvAiSentenceDisabled);
            layoutContent = v.findViewById(R.id.layoutAiSentenceContent);
            etInput = v.findViewById(R.id.etAiWordsInput);
            btnGenerate = v.findViewById(R.id.btnAiGenerate);
            tvResult = v.findViewById(R.id.tvAiSentenceResult);
            if (btnGenerate != null) btnGenerate.setOnClickListener(view -> {
                String input = etInput.getText().toString().trim();
                if (input.isEmpty()) { showSafeToast("请输入单词"); return; }
                tvResult.setText("请求中...");
                generateSentenceWithAI(input, btnGenerate, tvResult);
            });
        }
        public void bind(boolean enabled) {
            if (tvDisabled != null) tvDisabled.setVisibility(enabled ? View.GONE : View.VISIBLE);
            if (layoutContent != null) layoutContent.setVisibility(enabled ? View.VISIBLE : View.GONE);
        }
    }
}