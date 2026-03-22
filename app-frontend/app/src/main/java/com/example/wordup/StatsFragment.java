package com.example.wordup;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.List;

public class StatsFragment extends Fragment {

    private BarChart chartWordInput;
    private LineChart chartDuration;

    private TextView tvTabWeekWord, tvTabMonthWord;
    private TextView tvTabWeekDuration, tvTabMonthDuration;

    private View indicatorWord, indicatorDuration;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 这一行代码的魔法就是：把我们刚刚写的极其漂亮的 fragment_home.xml 变成真实的画面！
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initCharts(view);
        initTabs(view);
    }

    private void initTabs(View view) {
        tvTabWeekWord = view.findViewById(R.id.tvTabWeekWord);
        tvTabMonthWord = view.findViewById(R.id.tvTabMonthWord);
        tvTabWeekDuration = view.findViewById(R.id.tvTabWeekDuration);
        tvTabMonthDuration = view.findViewById(R.id.tvTabMonthDuration);

        indicatorWord = view.findViewById(R.id.indicatorMonthWord);
        indicatorDuration = view.findViewById(R.id.indicatorWeekDuration);

        /* 单词输入量：点击事件及数据联动 */
        tvTabWeekWord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateTabAppearance(tvTabWeekWord, tvTabMonthWord);
                moveIndicator(indicatorWord, tvTabWeekWord);
                loadWordChartData(true); // 动态加载周数据
            }
        });

        tvTabMonthWord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateTabAppearance(tvTabMonthWord, tvTabWeekWord);
                moveIndicator(indicatorWord, tvTabMonthWord);
                loadWordChartData(false); // 动态加载月数据
            }
        });

        /* 学习时长：点击事件及数据联动 */
        tvTabWeekDuration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateTabAppearance(tvTabWeekDuration, tvTabMonthDuration);
                moveIndicator(indicatorDuration, tvTabWeekDuration);
                loadDurationChartData(true); // 动态加载周数据
            }
        });

        tvTabMonthDuration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateTabAppearance(tvTabMonthDuration, tvTabWeekDuration);
                moveIndicator(indicatorDuration, tvTabMonthDuration);
                loadDurationChartData(false); // 动态加载月数据
            }
        });
    }

    private void moveIndicator(View indicator, TextView targetTab) {
        if (indicator.getLayoutParams() instanceof ConstraintLayout.LayoutParams) {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) indicator.getLayoutParams();
            params.startToStart = targetTab.getId();
            params.endToEnd = targetTab.getId();
            params.topToBottom = targetTab.getId();
            indicator.setLayoutParams(params);
        }
    }

    private void updateTabAppearance(TextView selected, TextView unselected) {
        if (getContext() == null) return;

        int colorPrimary = ContextCompat.getColor(getContext(), R.color.color_text_primary);
        int colorGray = Color.parseColor("#888888");

        selected.setTextColor(colorPrimary);
        selected.setTypeface(null, Typeface.BOLD);

        unselected.setTextColor(colorGray);
        unselected.setTypeface(null, Typeface.NORMAL);
    }

    private void initCharts(View view) {
        chartWordInput = view.findViewById(R.id.flChartWordInput);
        chartDuration = view.findViewById(R.id.flChartDuration);

        setupWordInputChartBase();
        setupDurationChartBase();

        /* 首次进入页面，默认渲染“最近一周”的数据 */
        loadWordChartData(true);
        loadDurationChartData(true);
    }

    /**
     * 单词输入量：基础样式配置（不含数据）
     */
    private void setupWordInputChartBase() {
        chartWordInput.setTouchEnabled(false);
        chartWordInput.getDescription().setEnabled(false);
        chartWordInput.getLegend().setEnabled(false);
        chartWordInput.setDrawGridBackground(false);

        XAxis xAxis = chartWordInput.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setAxisLineColor(Color.parseColor("#E0E0E0"));
        xAxis.setTextColor(Color.parseColor("#888888"));

        YAxis leftAxis = chartWordInput.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setDrawLabels(false);
        leftAxis.setAxisMinimum(0f);
        chartWordInput.getAxisRight().setEnabled(false);
    }

    /**
     * 单词输入量：根据维度动态注入数据与 X 轴标签
     * @param isWeekly true 为周数据，false 为月数据
     */
    private void loadWordChartData(boolean isWeekly) {
        String[] labels;
        List<BarEntry> entries = new ArrayList<>();

        if (isWeekly) {
            // 周维度标签与静态测试数据
            labels = new String[]{"03-16", "03-17", "03-18", "03-19", "03-20", "03-21", "今日"};
            entries.add(new BarEntry(0f, new float[]{10f, 15f}));
            entries.add(new BarEntry(1f, new float[]{12f, 20f}));
            entries.add(new BarEntry(2f, new float[]{18f, 10f}));
            entries.add(new BarEntry(3f, new float[]{0f, 0f}));
            entries.add(new BarEntry(4f, new float[]{15f, 15f}));
            entries.add(new BarEntry(5f, new float[]{20f, 10f}));
            entries.add(new BarEntry(6f, new float[]{8f, 5f}));
        } else {
            // 月维度标签与静态测试数据（还原设计图跨度）
            labels = new String[]{"10", "11", "12", "01", "02", "本月", "04", "05", "06", "07", "08", "09"};
            entries.add(new BarEntry(0f, new float[]{0f, 0f}));
            entries.add(new BarEntry(1f, new float[]{120f, 180f}));
            entries.add(new BarEntry(2f, new float[]{50f, 150f}));
            entries.add(new BarEntry(3f, new float[]{0f, 0f}));
            entries.add(new BarEntry(4f, new float[]{0f, 0f}));
            entries.add(new BarEntry(5f, new float[]{0f, 0f}));
            entries.add(new BarEntry(6f, new float[]{0f, 0f}));
            entries.add(new BarEntry(7f, new float[]{0f, 0f}));
            entries.add(new BarEntry(8f, new float[]{0f, 0f}));
            entries.add(new BarEntry(9f, new float[]{0f, 0f}));
            entries.add(new BarEntry(10f, new float[]{0f, 0f}));
            entries.add(new BarEntry(11f, new float[]{0f, 0f}));
        }

        // 动态重置 X 轴渲染器
        XAxis xAxis = chartWordInput.getXAxis();
        xAxis.setLabelCount(labels.length);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < labels.length) {
                    return labels[index];
                }
                return "";
            }
        });

        BarDataSet dataSet = new BarDataSet(entries, "word_data");
        dataSet.setColors(Color.parseColor("#FF9800"), Color.parseColor("#9C27B0"));
        dataSet.setDrawValues(false);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.18f);
        chartWordInput.setData(barData);
        chartWordInput.notifyDataSetChanged(); // 通知图表数据集已变更
        chartWordInput.invalidate(); // 执行重绘动画
    }

    /**
     * 学习时长：基础样式配置（不含数据）
     */
    private void setupDurationChartBase() {
        chartDuration.setTouchEnabled(false);
        chartDuration.getDescription().setEnabled(false);
        chartDuration.getLegend().setEnabled(false);
        chartDuration.setDrawGridBackground(false);

        XAxis xAxis = chartDuration.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setAxisLineColor(Color.parseColor("#E0E0E0"));
        xAxis.setTextColor(Color.parseColor("#888888"));

        YAxis leftAxis = chartDuration.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setDrawLabels(false);
        leftAxis.setAxisMinimum(0f);
        chartDuration.getAxisRight().setEnabled(false);
    }

    /**
     * 学习时长：根据维度动态注入数据与 X 轴标签
     * @param isWeekly true 为周数据，false 为月数据
     */
    private void loadDurationChartData(boolean isWeekly) {
        String[] labels;
        List<Entry> entries = new ArrayList<>();

        if (isWeekly) {
            labels = new String[]{"03-14", "03-15", "03-16", "03-17", "03-18", "03-19", "今日"};
            entries.add(new Entry(0f, 0f));
            entries.add(new Entry(1f, 0f));
            entries.add(new Entry(2f, 0f));
            entries.add(new Entry(3f, 1f));
            entries.add(new Entry(4f, 0f));
            entries.add(new Entry(5f, 0f));
            entries.add(new Entry(6f, 0f));
        } else {
            labels = new String[]{"10", "11", "12", "01", "02", "本月", "04", "05", "06", "07", "08", "09"};
            entries.add(new Entry(0f, 0f));
            entries.add(new Entry(1f, 415f));
            entries.add(new Entry(2f, 367f));
            entries.add(new Entry(3f, 0f));
            entries.add(new Entry(4f, 0f));
            entries.add(new Entry(5f, 1f));
            entries.add(new Entry(6f, 0f));
            entries.add(new Entry(7f, 0f));
            entries.add(new Entry(8f, 0f));
            entries.add(new Entry(9f, 0f));
            entries.add(new Entry(10f, 0f));
            entries.add(new Entry(11f, 0f));
        }

        XAxis xAxis = chartDuration.getXAxis();
        xAxis.setLabelCount(labels.length);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < labels.length) {
                    return labels[index];
                }
                return "";
            }
        });

        LineDataSet dataSet = new LineDataSet(entries, "duration_data");
        dataSet.setColor(Color.parseColor("#E0E0E0"));
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(Color.parseColor("#E0E0E0"));
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(true);
        dataSet.setDrawValues(true);
        dataSet.setValueTextColor(Color.parseColor("#888888"));
        dataSet.setValueTextSize(10f);
        dataSet.setMode(LineDataSet.Mode.LINEAR);

        LineData lineData = new LineData(dataSet);
        chartDuration.setData(lineData);
        chartDuration.notifyDataSetChanged();
        chartDuration.invalidate();
    }
}