package com.example.appbackend.service;

import com.example.appbackend.entity.UserPlan;
import com.example.appbackend.entity.WordBook;
import com.example.appbackend.mapper.UserPlanMapper;
import com.example.appbackend.mapper.UserWordRecordMapper;
import com.example.appbackend.mapper.WordBookMapper;
import com.example.appbackend.vo.StudyProgressVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户学习计划与设置业务逻辑类
 */
@Service
public class UserPlanService {

    private final UserPlanMapper userPlanMapper;
    private final UserWordRecordMapper userWordRecordMapper;
    private final WordBookMapper wordBookMapper;

    public UserPlanService(UserPlanMapper userPlanMapper,
                           UserWordRecordMapper userWordRecordMapper,
                           WordBookMapper wordBookMapper) {
        this.userPlanMapper = userPlanMapper;
        this.userWordRecordMapper = userWordRecordMapper;
        this.wordBookMapper = wordBookMapper;
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
    public boolean updateDailyTarget(Long userId, Integer dailyTarget, Integer dailyNewTarget, Integer dailyReviewTarget) {
        getPlanByUserId(userId);
        return userPlanMapper.updateDailyTarget(userId, dailyTarget, dailyNewTarget, dailyReviewTarget) > 0;
    }

    /**
     * 【重构】获取指定用户的当前词书学习进度
     * 架构变更：不再信任前端传递的 bookId，改为由后端实时从计划表中提取真实绑定的 bookId
     * @param userId 用户 ID (用于数据隔离与进度溯源)
     * @return 封装好的学习进度视图对象
     */
    public StudyProgressVO getStudyProgress(Long userId) {
        // 1. 获取该用户真实的、最新的学习计划（getPlanByUserId自带防空容错）
        UserPlan currentPlan = getPlanByUserId(userId);
        Long currentBookId = currentPlan.getBookId();

        // 防御性编程：如果异常情况下仍为 null，默认兜底为 1 号词书
        if (currentBookId == null) {
            currentBookId = 1L;
        }

        // 2. 根据真实的 currentBookId 查询已学单词数量
        int learnedCount = userWordRecordMapper.countLearnedWordsByBook(userId, currentBookId);

        // 3. 根据真实的 currentBookId 查询词书总单词数
        WordBook wordBook = wordBookMapper.selectById(currentBookId);
        int totalCount = (wordBook != null && wordBook.getTotalWords() != null) ? wordBook.getTotalWords() : 0;

        // 4. 计算进度百分比 (保留一位小数)
        double progressPercentage = 0.0;
        if (totalCount > 0) {
            progressPercentage = (learnedCount * 100.0) / totalCount;
            progressPercentage = Math.round(progressPercentage * 10.0) / 10.0;
        }

        // 5. 封装视图对象并返回
        StudyProgressVO progressVO = new StudyProgressVO();
        progressVO.setBookId(currentBookId); // 顺便把真实的 bookId 带给前端
        progressVO.setLearnedCount(learnedCount);
        progressVO.setTotalCount(totalCount);
        progressVO.setProgressPercentage(progressPercentage);

        return progressVO;
    }
}