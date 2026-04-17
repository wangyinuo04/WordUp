package com.example.appbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.appbackend.entity.UserPlan;
import com.example.appbackend.entity.UserWordRecord;
import com.example.appbackend.mapper.UserPlanMapper;
import com.example.appbackend.mapper.UserWordRecordMapper;
import com.example.appbackend.mapper.WordMapper;
import com.example.appbackend.vo.WordLearningVO;
import com.example.appbackend.dto.WordSyncDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 核心背词算法业务逻辑服务类
 * 职责：处理单词批次调度、动作提交、端云批量同步及历史记录下行查询
 */
@Service
public class WordLearningService {

    private final UserWordRecordMapper userWordRecordMapper;
    private final UserPlanMapper userPlanMapper;
    private final WordMapper wordMapper;

    public WordLearningService(UserWordRecordMapper userWordRecordMapper,
                               UserPlanMapper userPlanMapper,
                               WordMapper wordMapper) {
        this.userWordRecordMapper = userWordRecordMapper;
        this.userPlanMapper = userPlanMapper;
        this.wordMapper = wordMapper;
    }

    public int getTodayStudiedCount(Long userId, Long bookId, boolean isReview) {
        if (isReview) {
            return userWordRecordMapper.countTodayReviewedOldWords(userId, bookId);
        } else {
            return userWordRecordMapper.countTodayLearnedNewWords(userId, bookId);
        }
    }

    public List<WordLearningVO> getWordBatch(Long userId, Long bookId, int batchSize, boolean isReview) {
        List<WordLearningVO> batchList = new ArrayList<>();
        int remainingLimit = batchSize;

        UserPlan userPlan = userPlanMapper.selectByUserId(userId);

        // 【核心修改 Action 1】：废弃自作聪明的覆盖逻辑，完全尊重前端传入的 bookId
        Long realBookId = bookId;

        List<WordLearningVO> tempOldWords = userWordRecordMapper.selectTempOldWords(userId, realBookId, remainingLimit);
        batchList.addAll(tempOldWords);
        remainingLimit -= tempOldWords.size();

        if (remainingLimit <= 0) return batchList;

        if (isReview) {
            int dailyReviewTarget = (userPlan != null && userPlan.getDailyReviewTarget() != null) ? userPlan.getDailyReviewTarget() : 20;
            int todayReviewed = userWordRecordMapper.countTodayReviewedOldWords(userId, realBookId);
            int quotaRemaining = dailyReviewTarget - todayReviewed;

            if (quotaRemaining > 0) {
                int fetchLimit = Math.min(remainingLimit, quotaRemaining);
                List<WordLearningVO> reviewWords = userWordRecordMapper.selectReviewWords(userId, realBookId, fetchLimit);
                batchList.addAll(reviewWords);
            }
        } else {
            int dailyNewTarget = (userPlan != null && userPlan.getDailyNewTarget() != null) ? userPlan.getDailyNewTarget() : 10;
            int todayLearned = userWordRecordMapper.countTodayLearnedNewWords(userId, realBookId);
            int quotaRemaining = dailyNewTarget - todayLearned;

            if (quotaRemaining > 0) {
                int fetchLimit = Math.min(remainingLimit, quotaRemaining);
                List<WordLearningVO> newWords = userWordRecordMapper.selectNewWords(userId, realBookId, fetchLimit);
                batchList.addAll(newWords);
            }
        }
        return batchList;
    }

    @Transactional(rollbackFor = Exception.class)
    public void submitWordAction(Long userId, Long wordId, boolean isKnown) {
        LambdaQueryWrapper<UserWordRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserWordRecord::getUserId, userId)
                .eq(UserWordRecord::getWordId, wordId);
        UserWordRecord record = userWordRecordMapper.selectOne(queryWrapper);

        LocalDateTime now = LocalDateTime.now();

        if (record == null) {
            record = new UserWordRecord();
            record.setUserId(userId);
            record.setWordId(wordId);
            record.setLearnStatus(1);
            record.setErrorCount(0);
            record.setCreatedAt(now);

            if (isKnown) {
                record.setIsTempOld(false);
                record.setConsecutiveKnownCount(0);
                record.setCurrentStage(2);
                record.setNextReviewTime(calculateNextReviewTime(2, now));
            } else {
                record.setIsTempOld(true);
                record.setConsecutiveKnownCount(0);
                record.setCurrentStage(1);
                record.setNextReviewTime(now);
            }
            userWordRecordMapper.insert(record);
        } else {
            if (Boolean.TRUE.equals(record.getIsTempOld())) {
                if (isKnown) {
                    record.setConsecutiveKnownCount(record.getConsecutiveKnownCount() + 1);
                    if (record.getConsecutiveKnownCount() >= 3) {
                        record.setIsTempOld(false);
                        record.setConsecutiveKnownCount(0);
                        int nextStage = Math.min(record.getCurrentStage() + 1, 5);
                        record.setCurrentStage(nextStage);
                        record.setNextReviewTime(calculateNextReviewTime(nextStage, now));
                        if (nextStage == 5) {
                            record.setLearnStatus(2);
                        }
                    }
                } else {
                    record.setConsecutiveKnownCount(0);
                    record.setErrorCount(record.getErrorCount() + 1);
                }
            } else {
                if (isKnown) {
                    int nextStage = Math.min(record.getCurrentStage() + 1, 5);
                    record.setCurrentStage(nextStage);
                    record.setNextReviewTime(calculateNextReviewTime(nextStage, now));
                    if (nextStage == 5) {
                        record.setLearnStatus(2);
                    }
                } else {
                    record.setIsTempOld(true);
                    record.setConsecutiveKnownCount(0);
                    record.setErrorCount(record.getErrorCount() + 1);
                }
            }
            record.setUpdatedAt(now);
            userWordRecordMapper.updateById(record);
        }
    }

    public List<com.example.appbackend.entity.Word> getAllWordsByBookId(Long bookId) {
        return wordMapper.selectByBookId(bookId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchSyncLocalRecords(Long userId, List<WordSyncDTO> syncRecords) {
        LocalDateTime now = LocalDateTime.now();
        for (WordSyncDTO dto : syncRecords) {
            LambdaQueryWrapper<UserWordRecord> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(UserWordRecord::getUserId, userId)
                    .eq(UserWordRecord::getWordId, dto.getWordId());
            UserWordRecord record = userWordRecordMapper.selectOne(queryWrapper);

            LocalDateTime nextReview = dto.getNextReviewTime() != null ?
                    java.time.Instant.ofEpochMilli(dto.getNextReviewTime()).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : now;

            if (record == null) {
                record = new UserWordRecord();
                record.setUserId(userId);
                record.setWordId(dto.getWordId());
                record.setLearnStatus(dto.getLearnStatus());
                record.setCurrentStage(dto.getCurrentStage());
                record.setNextReviewTime(nextReview);
                record.setCreatedAt(now);
                record.setUpdatedAt(now);
                userWordRecordMapper.insert(record);
            } else {
                record.setLearnStatus(dto.getLearnStatus());
                record.setCurrentStage(dto.getCurrentStage());
                record.setNextReviewTime(nextReview);
                record.setUpdatedAt(now);
                userWordRecordMapper.updateById(record);
            }
        }
    }

    public List<UserWordRecord> getUserRecordsByBook(Long userId, Long bookId) {
        LambdaQueryWrapper<UserWordRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserWordRecord::getUserId, userId);
        return userWordRecordMapper.selectList(queryWrapper);
    }

    private LocalDateTime calculateNextReviewTime(int stage, LocalDateTime baseTime) {
        switch (stage) {
            case 2: return baseTime.plusDays(1);
            case 3: return baseTime.plusDays(2);
            case 4: return baseTime.plusDays(4);
            case 5: return baseTime.plusDays(7);
            default: return baseTime;
        }
    }
}