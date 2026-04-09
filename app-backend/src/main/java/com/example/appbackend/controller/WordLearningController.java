package com.example.appbackend.controller;

import com.example.appbackend.Result;
import com.example.appbackend.dto.WordActionDTO;
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
            // 获取单词批次
            List<WordLearningVO> batch = wordLearningService.getWordBatch(userId, bookId, batchSize, isReview);
            // 获取该模式下今日已学的真实进度偏移量
            int offset = wordLearningService.getTodayStudiedCount(userId, isReview);

            // 封装为复合对象返回前端
            Map<String, Object> data = new HashMap<>();
            data.put("words", batch);
            data.put("offset", offset);

            return Result.success(data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "获取单词批次失败，请检查服务端系统日志");
        }
    }

    @PostMapping("/action")
    public Result<Void> submitWordAction(@RequestBody WordActionDTO actionDTO) {
        if (actionDTO.getUserId() == null || actionDTO.getWordId() == null || actionDTO.getIsKnown() == null) {
            return Result.error(400, "请求参数异常：用户ID、单词ID和操作结果均不能为空");
        }
        try {
            wordLearningService.submitWordAction(
                    actionDTO.getUserId(),
                    actionDTO.getWordId(),
                    actionDTO.getIsKnown()
            );
            return Result.success("操作结果记录成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "提交操作结果失败，请检查服务端系统日志");
        }
    }
}