package com.example.appbackend.controller;

import com.example.appbackend.Result;
import com.example.appbackend.entity.UserPlan;
import com.example.appbackend.service.UserPlanService;
import com.example.appbackend.vo.StudyProgressVO;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户学习计划与设置接口控制器
 */
@RestController
@RequestMapping("/api/plan")
public class UserPlanController {

    private final UserPlanService userPlanService;

    public UserPlanController(UserPlanService userPlanService) {
        this.userPlanService = userPlanService;
    }

    @GetMapping("/ai-settings")
    public Result<UserPlan> getAiSettings(@RequestParam("userId") Long userId) {
        UserPlan plan = userPlanService.getPlanByUserId(userId);
        if (plan != null) {
            return Result.success(plan);
        }
        return Result.error(404, "未查询到该用户的学习计划记录");
    }

    @PostMapping("/ai-settings/update")
    public Result<Void> updateAiSettings(@RequestBody UserPlan userPlan) {
        if (userPlan.getUserId() == null) {
            return Result.error(400, "请求参数异常：用户ID不能为空");
        }
        boolean isSuccess = userPlanService.updateAiSettings(userPlan);
        if (isSuccess) {
            return Result.success("AI设置更新成功", null);
        }
        return Result.error(500, "AI设置更新失败");
    }

    @PostMapping("/book/update")
    public Result<Void> updateBookId(@RequestBody Map<String, Object> params) {
        if (params.get("userId") == null || params.get("bookId") == null) {
            return Result.error(400, "参数异常：用户ID与词书ID不能为空");
        }
        Long userId = Long.valueOf(params.get("userId").toString());
        Long bookId = Long.valueOf(params.get("bookId").toString());

        boolean isSuccess = userPlanService.updateBookId(userId, bookId);
        if (isSuccess) {
            return Result.success("词书切换成功", null);
        }
        return Result.error(500, "词书切换失败");
    }

    @PostMapping("/daily-target/update")
    public Result<Void> updateDailyTarget(@RequestBody Map<String, Object> params) {
        if (params.get("userId") == null || params.get("dailyTarget") == null ||
                params.get("dailyNewTarget") == null || params.get("dailyReviewTarget") == null) {
            return Result.error(400, "参数不完整：缺少配额字段");
        }

        try {
            Long userId = Long.valueOf(params.get("userId").toString());
            Integer dailyTarget = Integer.valueOf(params.get("dailyTarget").toString());
            Integer dailyNewTarget = Integer.valueOf(params.get("dailyNewTarget").toString());
            Integer dailyReviewTarget = Integer.valueOf(params.get("dailyReviewTarget").toString());

            userPlanService.updateDailyTarget(userId, dailyTarget, dailyNewTarget, dailyReviewTarget);
            return Result.success("每日目标更新成功", null);
        } catch (Exception e) {
            return Result.error(500, "每日目标更新失败：" + e.getMessage());
        }
    }

    /**
     * 获取特定用户的真实词书学习进度
     * 【重构】参数精简，不再需要前端传递 bookId
     */
    @GetMapping("/progress")
    public Result<StudyProgressVO> getStudyProgress(@RequestParam("userId") Long userId) {
        if (userId == null) {
            return Result.error(400, "参数异常：用户ID不能为空");
        }
        StudyProgressVO progressVO = userPlanService.getStudyProgress(userId);
        return Result.success(progressVO);
    }
}