package com.example.appbackend.controller;

import com.example.appbackend.Result;
import com.example.appbackend.entity.UserPlan;
import com.example.appbackend.service.UserPlanService;
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
        System.out.println("当前请求接收到的 userId 为: " + userPlan.getUserId());
        if (userPlan.getUserId() == null) {
            return Result.error(400, "请求参数异常：用户ID不能为空");
        }
        boolean isSuccess = userPlanService.updateAiSettings(userPlan);
        if (isSuccess) {
            return Result.success("设置更新成功", null);
        }
        return Result.error(500, "设置更新失败，请重试");
    }

    /**
     * 更新用户当前正在学习的词书
     */
    @PostMapping("/book/update")
    public Result<Void> updateUserBook(@RequestBody Map<String, Long> params) {
        System.out.println("当前请求接收到的 userId 为: " + params.get("userId"));
        Long userId = params.get("userId");
        Long bookId = params.get("bookId");

        if (userId == null || bookId == null) {
            return Result.error(400, "用户ID与词书ID不能为空");
        }

        boolean isSuccess = userPlanService.updateBookId(userId, bookId);
        if (isSuccess) {
            return Result.success("词书切换成功", null);
        }
        return Result.error(500, "词书切换失败");
    }

    /**
     * 更新用户的每日学习目标总数
     */
    @PostMapping("/daily-target/update")
    public Result<Void> updateDailyTarget(@RequestBody Map<String, Object> params) {
        System.out.println("当前请求接收到的 userId 为: " + params.get("userId"));
        Long userId;
        Integer dailyTarget;
        try {
            userId = Long.valueOf(params.get("userId").toString());
            dailyTarget = Integer.valueOf(params.get("dailyTarget").toString());
        } catch (Exception e) {
            return Result.error(400, "参数格式异常");
        }

        if (userId == null || dailyTarget == null) {
            return Result.error(400, "用户ID与每日目标数不能为空");
        }

        boolean isSuccess = userPlanService.updateDailyTarget(userId, dailyTarget);
        if (isSuccess) {
            return Result.success("每日学习目标更新成功", null);
        }
        return Result.error(500, "每日学习目标更新失败");
    }
}