package com.example.wordup;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

/**
 * 单词学习页面 Fragment。
 * 负责承载“背诵新词”与“复习旧词”业务逻辑的视图组件。
 */
public class WordLearningFragment extends Fragment {

    // UI 组件声明
    private TextView tvProgress;
    private TextView tvWord;
    private TextView tvPhonetic;
    private TextView tvDefinition;
    private TextView tvExample;
    private LinearLayout btnBack;
    private LinearLayout btnAIFeature;
    private AppCompatButton btnUnfamiliar;
    private AppCompatButton btnFamiliar;
    private LinearLayout layoutAntiSleep;

    // 状态数据声明
    private int currentPosition = 0;
    private int totalWords = 150;
    private boolean isReviewMode = false;

    public WordLearningFragment() {
        // Fragment 要求的空构造函数
    }

    /**
     * 实例化 Fragment 组件并传递状态参数。
     *
     * @param isReview 是否为复习模式
     * @param total    总单词数量
     * @return 配置完毕的 WordLearningFragment 实例
     */
    public static WordLearningFragment newInstance(boolean isReview, int total) {
        WordLearningFragment fragment = new WordLearningFragment();
        Bundle args = new Bundle();
        args.putBoolean("isReview", isReview);
        args.putInt("total", total);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_word_learning, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 提取并应用传递的参数状态
        if (getArguments() != null) {
            isReviewMode = getArguments().getBoolean("isReview", false);
            totalWords = getArguments().getInt("total", 150);
        }

        // 初始化视图组件
        initViews(view);

        // 绑定交互事件监听器
        setupClickListeners();

        // 执行初始数据的加载操作
        loadWord(currentPosition);
    }

    /**
     * 初始化页面组件引用。
     */
    private void initViews(View view) {
        tvProgress = view.findViewById(R.id.tvProgress);
        tvWord = view.findViewById(R.id.tvWord);
        tvPhonetic = view.findViewById(R.id.tvPhonetic);
        tvDefinition = view.findViewById(R.id.tvDefinition);
        tvExample = view.findViewById(R.id.tvExample);
        btnBack = view.findViewById(R.id.btnBack);
        btnAIFeature = view.findViewById(R.id.btnAIFeature);
        btnUnfamiliar = view.findViewById(R.id.btnUnfamiliar);
        btnFamiliar = view.findViewById(R.id.btnFamiliar);
        layoutAntiSleep = view.findViewById(R.id.layoutAntiSleep);
    }

    /**
     * 绑定各项点击事件处理逻辑。
     */
    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> goBack());
        btnUnfamiliar.setOnClickListener(v -> handleUnfamiliar());
        btnFamiliar.setOnClickListener(v -> handleFamiliar());
        btnAIFeature.setOnClickListener(v -> handleAIFeature());
    }

    /**
     * 根据索引位置加载并渲染指定的单词数据。
     *
     * @param position 当前单词的数据索引
     */
    private void loadWord(int position) {
        currentPosition = position;
        tvProgress.setText((currentPosition + 1) + "/" + totalWords);

        // 预留接口：后续需连接后端 API 获取真实单词数据
        tvWord.setText("apple");
        tvPhonetic.setText("/'æpl/");
        tvDefinition.setText("n. 苹果");
        tvExample.setText("I eat an apple every day.");
        tvExample.setVisibility(View.VISIBLE);
    }

    /**
     * 执行页面返回操作。
     * 通过弹出回退栈来恢复上级 Fragment 的视图显示。
     */
    private void goBack() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }

    /**
     * 处理“不认识”按钮的业务逻辑。
     */
    private void handleUnfamiliar() {
        Toast.makeText(getContext(), "已加入复习计划", Toast.LENGTH_SHORT).show();
        nextWord();
    }

    /**
     * 处理“认识”按钮的业务逻辑。
     */
    private void handleFamiliar() {
        Toast.makeText(getContext(), "太棒了！继续加油！", Toast.LENGTH_SHORT).show();
        nextWord();
    }

    /**
     * 处理“AI功能”按钮的业务逻辑，执行路由跳转至 AI 设置页。
     */
    private void handleAIFeature() {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), AISettingsActivity.class);
            startActivity(intent);
        }
    }

    /**
     * 执行切换至下一个单词的逻辑控制。
     */
    private void nextWord() {
        if (currentPosition < totalWords - 1) {
            loadWord(currentPosition + 1);
        } else {
            Toast.makeText(getContext(), "🎉 恭喜完成今日学习！", Toast.LENGTH_LONG).show();
            goBack();
        }
    }
}