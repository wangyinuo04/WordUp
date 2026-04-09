package com.example.appbackend.service;

import com.example.appbackend.entity.UserPlan;
import com.example.appbackend.mapper.UserPlanMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户学习计划与设置业务逻辑类
 */
@Service
public class UserPlanService {

    private final UserPlanMapper userPlanMapper;

    public UserPlanService(UserPlanMapper userPlanMapper) {
        this.userPlanMapper = userPlanMapper;
    }

    /**
     * 获取用户计划，增加“懒初始化”逻辑以兼容旧用户数据
     */
    @Transactional(rollbackFor = Exception.class)
    public UserPlan getPlanByUserId(Long userId) {
        UserPlan plan = userPlanMapper.selectByUserId(userId);
        // 如果数据库中不存在该用户的配置记录（针对注册较早的旧账号）
        if (plan == null) {
            // 实时补全初始化记录，默认分配 ID 为 1 的词书
            userPlanMapper.insertDefaultPlan(userId, 1L);
            // 重新查询获取最新生成的记录
            return userPlanMapper.selectByUserId(userId);
        }
        return plan;
    }

    public boolean updateAiSettings(UserPlan userPlan) {
        // 更新前先调用 getPlanByUserId 确保记录必定存在，防止旧账号直接执行更新失败
        getPlanByUserId(userPlan.getUserId());
        return userPlanMapper.updateAiSettings(userPlan) > 0;
    }

    /**
     * 更新用户的词书 ID
     */
    public boolean updateBookId(Long userId, Long bookId) {
        // 更新前先确保记录必定存在
        getPlanByUserId(userId);
        return userPlanMapper.updateBookId(userId, bookId) > 0;
    }

    /**
     * 更新用户的每日学习目标总数
     */
    // 在 Service 或其实现类中，将原本的方法参数补充完整，并调用 Mapper
    public void updateDailyTarget(Long userId, Integer dailyTarget, Integer dailyNewTarget, Integer dailyReviewTarget) {
        // 更新前先确保记录必定存在
        getPlanByUserId(userId);
        userPlanMapper.updateDailyTarget(userId, dailyTarget, dailyNewTarget, dailyReviewTarget);
    }
}