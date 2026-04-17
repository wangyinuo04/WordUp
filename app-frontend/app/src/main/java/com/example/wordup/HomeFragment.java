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
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import com.example.wordup.db.sync.DataSyncManager;
import com.example.wordup.db.sync.SyncCallback;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 首页Fragment的业务逻辑控制类。
 */
public class HomeFragment extends Fragment {

    private TextView tvDate;
    private TextView tvCurrentBook;
    private TextView tvTodayWords;

    private ImageView ivStatusAntiDoze, ivStatusAISentence, ivStatusEmotion;
    private TextView tvStatusAntiDoze, tvStatusAISentence, tvStatusEmotion;

    private TextView tvProgressText;
    private ProgressBar progressBar;

    private long currentUserId;

    private int currentDailyNewTarget = 10;
    private int currentDailyReviewTarget = 20;

    private int currentDailyTotalTarget = -1;
    private double currentProgressPercentage = -1.0;

    private boolean isCheckingJump = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        currentUserId = requireActivity().getSharedPreferences("MyAppConfig", Context.MODE_PRIVATE).getLong("userId", -1L);

        initViews(view);

        View btnLearnNew = view.findViewById(R.id.btnLearnNew);
        if (btnLearnNew != null) {
            btnLearnNew.setOnClickListener(v -> handleLearningJump(false));
        }

        View btnReviewOld = view.findViewById(R.id.btnReviewOld);
        if (btnReviewOld != null) {
            btnReviewOld.setOnClickListener(v -> handleLearningJump(true));
        }

        View btnChangePlan = view.findViewById(R.id.btnChangePlan);
        if (btnChangePlan != null) {
            btnChangePlan.setOnClickListener(v -> showPlanBottomSheet());
        }

        AppCompatButton btnSettingsAI = view.findViewById(R.id.btnSettingsAI);
        if (btnSettingsAI != null) {
            btnSettingsAI.setOnClickListener(v -> {
                Intent intent = new Intent(requireActivity(), AISettingsActivity.class);
                startActivity(intent);
            });
        }

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
        if (currentUserId != -1L) {
            refreshAiSettingsStatus();
            refreshCurrentBookName();
            refreshStudyProgress();
            checkAndHealBookId(); // 启动独立的原生网络巡逻引擎！
        }
        if (tvDate != null) {
            updateDateText();
        }
    }

    // ========================================================================
    // 【终极防御：完全独立的暴力原生自愈引擎】
    // ========================================================================
    private void checkAndHealBookId() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(NetworkConfig.GET_AI_SETTINGS_URL + "?userId=" + currentUserId)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String res = response.body().string();
                    JSONObject json = new JSONObject(res);
                    if (json.getInt("code") == 200) {
                        JSONObject data = json.optJSONObject("data");
                        if (data != null) {
                            // 暴力解析，无惧任何实体类缺失！
                            long cloudBookId = data.optLong("bookId", data.optLong("book_id", -1L));
                            if (cloudBookId != -1L) {
                                long localBookId = requireActivity().getSharedPreferences("MyAppConfig", Context.MODE_PRIVATE)
                                        .getLong("current_book_id", 1L);

                                if (localBookId != cloudBookId) {
                                    // 确诊错位，强行纠正并补发同步货源
                                    requireActivity().getSharedPreferences("MyAppConfig", Context.MODE_PRIVATE)
                                            .edit()
                                            .putLong("current_book_id", cloudBookId)
                                            .apply();

                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() -> {
                                            Toast.makeText(requireContext(), "已为您自动校准最新词库...", Toast.LENGTH_SHORT).show();
                                            DataSyncManager.getInstance(requireContext()).fetchCloudDataToLocal(currentUserId, cloudBookId, new SyncCallback() {
                                                @Override
                                                public void onSuccess() {
                                                    if (getActivity() != null) {
                                                        getActivity().runOnUiThread(() -> {
                                                            Toast.makeText(requireContext(), "专属词库已就绪！可以离线背词了", Toast.LENGTH_SHORT).show();
                                                            refreshCurrentBookName();
                                                        });
                                                    }
                                                }
                                                @Override
                                                public void onFailure(String errorMessage) {}
                                            });
                                        });
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void initViews(View view) {
        tvDate = view.findViewById(R.id.tvDate);
        tvCurrentBook = view.findViewById(R.id.tvCurrentBook);
        tvTodayWords = view.findViewById(R.id.tvTodayWords);
        tvProgressText = view.findViewById(R.id.tvProgressText);
        progressBar = view.findViewById(R.id.progressBar);

        if (tvTodayWords != null) {
            if (currentDailyTotalTarget != -1) {
                tvTodayWords.setText(String.valueOf(currentDailyTotalTarget));
            } else {
                tvTodayWords.setText("--");
            }
        }

        if (tvProgressText != null && progressBar != null) {
            if (currentProgressPercentage != -1.0) {
                tvProgressText.setText("当前进度 " + currentProgressPercentage + "%");
                progressBar.setProgress((int) currentProgressPercentage);
            } else {
                tvProgressText.setText("当前进度 --%");
                progressBar.setProgress(0);
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
                if (tvCurrentBook.getText().toString().isEmpty() || tvCurrentBook.getText().toString().equals("《暂无词书》") || tvCurrentBook.getText().toString().equals("--")) {
                    tvCurrentBook.setText("《离线词书模式》");
                }
            }
        });
    }

    private void refreshStudyProgress() {
        PlanNetworkHelper.getStudyProgress(currentUserId, new PlanNetworkHelper.FetchProgressCallback() {
            @Override
            public void onSuccess(int learnedCount, int totalCount, double progressPercentage) {
                if (!isAdded()) return;
                currentProgressPercentage = progressPercentage;
                if (tvProgressText != null && progressBar != null) {
                    tvProgressText.setText("当前进度 " + progressPercentage + "%");
                    progressBar.setProgress((int) progressPercentage);
                }
            }

            @Override
            public void onFailure(String errorMsg) {
                if (!isAdded()) return;
                if (tvProgressText != null && progressBar != null && currentProgressPercentage == -1.0) {
                    tvProgressText.setText("当前进度 0%");
                    progressBar.setProgress(0);
                }
            }
        });
    }

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

                if (plan.getDailyNewTarget() != null) currentDailyNewTarget = plan.getDailyNewTarget();
                if (plan.getDailyReviewTarget() != null) currentDailyReviewTarget = plan.getDailyReviewTarget();

                if (plan.getDailyTarget() != null) {
                    currentDailyTotalTarget = plan.getDailyTarget();
                    if (tvTodayWords != null) tvTodayWords.setText(String.valueOf(currentDailyTotalTarget));
                }
            }

            @Override
            public void onFailure(String errorMsg) {}
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

    private void handleLearningJump(boolean isReviewMode) {
        if (isCheckingJump) return;
        isCheckingJump = true;

        int targetWords = isReviewMode ? currentDailyReviewTarget : currentDailyNewTarget;
        long currentBookId = requireActivity().getSharedPreferences("MyAppConfig", Context.MODE_PRIVATE).getLong("current_book_id", 1L);

        WordLearningNetworkHelper.getWordBatch(currentUserId, currentBookId, 1, isReviewMode, new WordLearningNetworkHelper.GetBatchCallback() {
            @Override
            public void onSuccess(List<WordLearningVO> wordList, int offset) {
                isCheckingJump = false;
                if (!isAdded()) return;

                if (wordList == null || wordList.isEmpty()) {
                    String finishMsg = isReviewMode ? "🎉 恭喜！今日复习任务已全部完成！" : "🎉 恭喜！今日新词学习已达标！";
                    Toast.makeText(requireContext(), finishMsg, Toast.LENGTH_SHORT).show();
                } else {
                    WordLearningFragment fragment = WordLearningFragment.newInstance(isReviewMode, targetWords);
                    if (getActivity() != null) {
                        getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, fragment)
                                .addToBackStack(null)
                                .commit();
                    }
                }
            }

            @Override
            public void onFailure(String errorMsg) {
                isCheckingJump = false;
                if (!isAdded()) return;

                Toast.makeText(requireContext(), "网络离线，已进入本地缓存学习模式", Toast.LENGTH_SHORT).show();

                WordLearningFragment fragment = WordLearningFragment.newInstance(isReviewMode, targetWords);
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, fragment)
                            .addToBackStack(null)
                            .commit();
                }
            }
        });
    }

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