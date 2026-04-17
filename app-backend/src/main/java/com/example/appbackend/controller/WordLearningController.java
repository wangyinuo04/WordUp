package com.example.appbackend.controller;

import com.example.appbackend.Result;
import com.example.appbackend.dto.WordActionDTO;
import com.example.appbackend.dto.WordSyncDTO;
import com.example.appbackend.entity.UserWordRecord;
import com.example.appbackend.service.WordLearningService;
import com.example.appbackend.vo.WordLearningVO;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 背单词核心功能接口控制器
 */
@RestController
@RequestMapping("/api/learning")
public class WordLearningController {

    private final WordLearningService wordLearningService;

    public WordLearningController(WordLearningService wordLearningService) {
        this.wordLearningService = wordLearningService;
    }

    @GetMapping("/batch")
    public Result<Map<String, Object>> getWordBatch(
            @RequestParam("userId") Long userId,
            @RequestParam("bookId") Long bookId,
            @RequestParam("isReview") boolean isReview,
            @RequestParam(value = "batchSize", defaultValue = "10") int batchSize) {

        if (userId == null || bookId == null) {
            return Result.error(400, "请求参数异常：用户ID和词书ID不能为空");
        }
        try {
            List<WordLearningVO> batch = wordLearningService.getWordBatch(userId, bookId, batchSize, isReview);
            // 【修复点】：增加 bookId 参数，确保进度偏移量统计与当前词书隔离
            int offset = wordLearningService.getTodayStudiedCount(userId, bookId, isReview);
            Map<String, Object> data = new HashMap<>();
            data.put("words", batch);
            data.put("offset", offset);
            return Result.success(data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "获取单词批次失败");
        }
    }

    @PostMapping("/action")
    public Result<Void> submitWordAction(@RequestBody WordActionDTO actionDTO) {
        if (actionDTO.getUserId() == null || actionDTO.getWordId() == null || actionDTO.getIsKnown() == null) {
            return Result.error(400, "参数不能为空");
        }
        try {
            wordLearningService.submitWordAction(actionDTO.getUserId(), actionDTO.getWordId(), actionDTO.getIsKnown());
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "提交学习动作异常");
        }
    }

    @GetMapping("/all")
    public Result<List<com.example.appbackend.entity.Word>> getAllWords(@RequestParam("bookId") Long bookId) {
        if (bookId == null) return Result.error(400, "词书ID不能为空");
        try {
            List<com.example.appbackend.entity.Word> words = wordLearningService.getAllWordsByBookId(bookId);
            return Result.success(words);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "全量词库拉取失败");
        }
    }

    @PostMapping("/batch-sync")
    public Result<Void> batchSyncRecords(@RequestParam("userId") Long userId, @RequestBody List<WordSyncDTO> syncRecords) {
        if (userId == null || syncRecords == null || syncRecords.isEmpty()) {
            return Result.error(400, "同步数据为空");
        }
        try {
            wordLearningService.batchSyncLocalRecords(userId, syncRecords);
            return Result.success("同步成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "同步异常");
        }
    }

    /**
     * [新增接口] 获取用户全量进度记录 (下行同步专用)
     */
    @GetMapping("/records")
    public Result<List<UserWordRecord>> getUserRecords(@RequestParam("userId") Long userId, @RequestParam("bookId") Long bookId) {
        if (userId == null || bookId == null) return Result.error(400, "参数异常");
        try {
            List<UserWordRecord> records = wordLearningService.getUserRecordsByBook(userId, bookId);
            return Result.success(records);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "获取历史记录失败");
        }
    }
}