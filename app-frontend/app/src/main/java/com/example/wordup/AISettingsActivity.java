package com.example.wordup;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.wordup.ai.analyzer.FaceMeshAnalyzer;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI设置页面的业务逻辑控制类。
 * 包含防瞌睡功能的后台实时眼部基准校准逻辑与严格比例的居中弹窗展示。
 */
public class AISettingsActivity extends AppCompatActivity {

    private ImageView ivBack;
    private SwitchCompat switchAntiDoze;
    private SwitchCompat switchAISentence;
    private SwitchCompat switchEmotion;

    private long currentUserId;
    private boolean isProgrammaticChange = false;

    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;
    private FaceMeshAnalyzer aiAnalyzer;

    private Dialog calibrationDialog;
    private ProgressBar pbCalibration;
    private boolean hasStartedCalibration = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_settings);

        currentUserId = getSharedPreferences("MyAppConfig", Context.MODE_PRIVATE).getLong("userId", -1L);

        if (currentUserId == -1L) {
            Toast.makeText(this, "用户信息异常，请重新登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initPermissions();
        initView();
        initListener();
        fetchCurrentSettings();
    }

    private void initPermissions() {
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        startCalibrationFlow();
                    } else {
                        Toast.makeText(this, "需要相机权限才能完成眼部校准", Toast.LENGTH_SHORT).show();
                        revertAntiDozeSwitch();
                    }
                }
        );
    }

    private void initView() {
        ivBack = findViewById(R.id.ivBack);
        switchAntiDoze = findViewById(R.id.switchAntiDoze);
        switchAISentence = findViewById(R.id.switchAISentence);
        switchEmotion = findViewById(R.id.switchEmotion);
    }

    private void fetchCurrentSettings() {
        AiNetworkHelper.getAiSettings(currentUserId, new AiNetworkHelper.GetSettingsCallback() {
            @Override
            public void onSuccess(UserPlan plan) {
                isProgrammaticChange = true;
                switchAntiDoze.setChecked(plan.getAntiSleepOn() != null && plan.getAntiSleepOn() == 1);
                switchAISentence.setChecked(plan.getAiSentenceOn() != null && plan.getAiSentenceOn() == 1);
                switchEmotion.setChecked(plan.getEmotionRecogOn() != null && plan.getEmotionRecogOn() == 1);
                isProgrammaticChange = false;
            }

            @Override
            public void onFailure(String errorMsg) {
                Toast.makeText(AISettingsActivity.this, "配置加载失败：" + errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initListener() {
        ivBack.setOnClickListener(v -> finish());

        switchAntiDoze.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isProgrammaticChange) return;

            if (isChecked) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    startCalibrationFlow();
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA);
                }
            } else {
                syncSettingsToBackend();
            }
        });

        switchAISentence.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isProgrammaticChange) syncSettingsToBackend();
        });

        switchEmotion.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isProgrammaticChange) syncSettingsToBackend();
        });
    }

    private void startCalibrationFlow() {
        showCalibrationDialog();
        initCameraAndAI();
    }

    /**
     * 实例化并配置严格比例的中央弹窗
     */
    private void showCalibrationDialog() {
        calibrationDialog = new Dialog(this);
        calibrationDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        calibrationDialog.setContentView(R.layout.layout_dialog_calibration);
        calibrationDialog.setCancelable(false);

        Window window = calibrationDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);

            // 获取屏幕真实的物理像素分辨率
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            int screenWidth = displayMetrics.widthPixels;
            int screenHeight = displayMetrics.heightPixels;

            // 动态计算弹窗尺寸：宽为屏幕的3/5，高为屏幕的1/5
            int dialogWidth = (int) (screenWidth * 0.6);
            int dialogHeight = (int) (screenHeight * 0.2);

            window.setLayout(dialogWidth, dialogHeight);
            window.setGravity(Gravity.CENTER);
            window.setDimAmount(0.6f);
        }

        pbCalibration = calibrationDialog.findViewById(R.id.pbCalibration);

        if (pbCalibration != null) {
            // 修改进度条颜色为纯白色
            pbCalibration.setProgressTintList(ColorStateList.valueOf(Color.WHITE));
        }

        calibrationDialog.show();
    }

    private void initCameraAndAI() {
        hasStartedCalibration = false;
        cameraExecutor = Executors.newSingleThreadExecutor();

        aiAnalyzer = new FaceMeshAnalyzer(this, result -> {
            runOnUiThread(() -> {
                if (calibrationDialog == null || !calibrationDialog.isShowing()) return;

                if (result.isCalibrating) {
                    hasStartedCalibration = true;
                    if (pbCalibration != null) {
                        pbCalibration.setProgress(result.calibrationProgress);
                    }
                } else if (hasStartedCalibration && !result.isCalibrating) {
                    hasStartedCalibration = false;
                    finishCalibrationSuccessfully();
                }
            });
        });

        aiAnalyzer.setFatigueDetectionEnabled(true);

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                imageAnalysis.setAnalyzer(cameraExecutor, aiAnalyzer);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void finishCalibrationSuccessfully() {
        stopCameraAndAI();
        if (calibrationDialog != null && calibrationDialog.isShowing()) {
            calibrationDialog.dismiss();
        }
        Toast.makeText(this, "眼部基准校准完成，防瞌睡已开启", Toast.LENGTH_SHORT).show();
        syncSettingsToBackend();
    }

    private void revertAntiDozeSwitch() {
        isProgrammaticChange = true;
        switchAntiDoze.setChecked(false);
        isProgrammaticChange = false;
    }

    private void stopCameraAndAI() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            cameraProvider = null;
        }
        if (cameraExecutor != null && !cameraExecutor.isShutdown()) {
            cameraExecutor.shutdown();
            cameraExecutor = null;
        }
        if (aiAnalyzer != null) {
            aiAnalyzer.setFatigueDetectionEnabled(false);
            aiAnalyzer = null;
        }
    }

    private void syncSettingsToBackend() {
        UserPlan currentPlan = new UserPlan();
        currentPlan.setUserId(currentUserId);
        currentPlan.setAntiSleepOn(switchAntiDoze.isChecked() ? 1 : 0);
        currentPlan.setAiSentenceOn(switchAISentence.isChecked() ? 1 : 0);
        currentPlan.setEmotionRecogOn(switchEmotion.isChecked() ? 1 : 0);

        AiNetworkHelper.updateAiSettings(currentPlan, new AiNetworkHelper.UpdateSettingsCallback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(String errorMsg) {
                Toast.makeText(AISettingsActivity.this, "配置同步失败，请检查网络", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCameraAndAI();
    }
}