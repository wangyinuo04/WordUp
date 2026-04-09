package com.example.wordup.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordup.ui.model.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

// --- 1. 定义匹配 JSON 结构的 DTO (数据传输对象) ---
data class ApiResponse(val code: Int, val msg: String, val data: ApiDashboardData?)
data class ApiDashboardData(
    val overview: ApiOverview, val funnel: ApiFunnel,
    val aiEmotion: ApiAiEmotion, val hardWordsMap: ApiHardWordsMap
)
data class ApiOverview(val streakDays: Int, val todayProgress: Float, val newWordsCount: Int, val reviewWordsCount: Int, val studyMinutes: Int)
data class ApiFunnel(val phase1Count: Int, val phase2Count: Int, val phase3Count: Int, val phase4Count: Int, val phase5Count: Int, val longTermRetentionRate: Float)
data class ApiAiEmotion(val radarScores: List<Float>, val weeklyTrend: List<Float>, val sleepyCount: Int, val unfocusedCount: Int, val aiHardWordsPushed: Int)
data class ApiHardWordsMap(val hardWords: List<ApiWordCloudItem>, val heatmapLevels: List<Int>)
data class ApiWordCloudItem(val word: String, val weight: Int)

// --- 2. 状态管理器 ---
class StatisticsViewModel : ViewModel() {
    // 绑定给 UI 观察的可变状态
    var overviewState by mutableStateOf(LearningOverviewState())
    var funnelState by mutableStateOf(MemoryFunnelState())
    var aiEmotionState by mutableStateOf(AiEmotionState())
    var hardWordsState by mutableStateOf(HardWordsState())

    // 加载状态
    var isLoading by mutableStateOf(true)

    private val client = OkHttpClient()
    private val gson = Gson()

    fun loadStatisticsData(userId: Long) {
        viewModelScope.launch {
            isLoading = true
            try {
                // 切换到 IO 线程执行网络请求
                val responseStr = withContext(Dispatchers.IO) {
                    // 【关键修复】使用 NetworkConfig 中的统一地址，不再硬编码 10.0.2.2
                    val baseUrl = com.example.wordup.NetworkConfig.BASE_URL
                    val url = "$baseUrl/api/statistics/dashboard?userId=$userId"
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute().use { response ->
                        response.body?.string()
                    }
                }

                if (responseStr != null) {
                    val apiResponse = gson.fromJson(responseStr, ApiResponse::class.java)
                    if (apiResponse.code == 200 && apiResponse.data != null) {
                        val d = apiResponse.data
                        // 将后端 DTO 映射到前台的 UI State 模型中
                        overviewState = LearningOverviewState(d.overview.streakDays, d.overview.todayProgress, d.overview.newWordsCount, d.overview.reviewWordsCount, d.overview.studyMinutes)
                        funnelState = MemoryFunnelState(d.funnel.phase1Count, d.funnel.phase2Count, d.funnel.phase3Count, d.funnel.phase4Count, d.funnel.phase5Count, d.funnel.longTermRetentionRate)
                        aiEmotionState = AiEmotionState(d.aiEmotion.radarScores, d.aiEmotion.weeklyTrend, d.aiEmotion.sleepyCount, d.aiEmotion.unfocusedCount, d.aiEmotion.aiHardWordsPushed)
                        hardWordsState = HardWordsState(
                            hardWords = d.hardWordsMap.hardWords.map { WordCloudItem(it.word, it.weight) },
                            heatmapLevels = d.hardWordsMap.heatmapLevels
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace() // 网络异常或解析异常
            } finally {
                isLoading = false
            }
        }
    }
}