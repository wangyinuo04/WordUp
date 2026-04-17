package com.example.appbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.appbackend.entity.UserWordRecord;
import com.example.appbackend.vo.WordLearningVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户单词记忆记录表数据访问接口
 */
@Mapper
public interface UserWordRecordMapper extends BaseMapper<UserWordRecord> {

    /**
     * 队列优先级 1 (P0)：提取临时旧词
     */
    @Select("SELECT w.id AS wordId, w.spelling, w.phonetic, w.translation, w.difficulty AS difficulty, " +
            "uwr.id AS recordId, uwr.learn_status AS learnStatus, " +
            "uwr.is_temp_old AS isTempOld, uwr.consecutive_known_count AS consecutiveKnownCount, " +
            "uwr.current_stage AS currentStage, uwr.next_review_time AS nextReviewTime " +
            "FROM user_word_record uwr " +
            "JOIN word w ON uwr.word_id = w.id " +
            "WHERE uwr.user_id = #{userId} " +
            "  AND uwr.is_temp_old = 1 " +
            "  AND w.book_id = #{bookId} " +
            "ORDER BY uwr.updated_at ASC " +
            "LIMIT #{limit}")
    List<WordLearningVO> selectTempOldWords(@Param("userId") Long userId, @Param("bookId") Long bookId, @Param("limit") int limit);

    /**
     * 队列优先级 2 (P1)：提取今日待复习词 (艾宾浩斯)
     */
    @Select("SELECT w.id AS wordId, w.spelling, w.phonetic, w.translation, w.difficulty AS difficulty, " +
            "uwr.id AS recordId, uwr.learn_status AS learnStatus, " +
            "uwr.is_temp_old AS isTempOld, uwr.consecutive_known_count AS consecutiveKnownCount, " +
            "uwr.current_stage AS currentStage, uwr.next_review_time AS nextReviewTime " +
            "FROM user_word_record uwr " +
            "JOIN word w ON uwr.word_id = w.id " +
            "WHERE uwr.user_id = #{userId} " +
            "  AND uwr.learn_status = 1 " +
            "  AND uwr.is_temp_old = 0 " +
            "  AND uwr.next_review_time <= NOW() " +
            "  AND w.book_id = #{bookId} " +
            "ORDER BY w.id ASC " +
            "LIMIT #{limit}")
    List<WordLearningVO> selectReviewWords(@Param("userId") Long userId, @Param("bookId") Long bookId, @Param("limit") int limit);

    /**
     * 队列优先级 3 (P2)：提取新词
     */
    @Select("SELECT w.id AS wordId, w.spelling, w.phonetic, w.translation, w.difficulty AS difficulty, " +
            "uwr.id AS recordId, IFNULL(uwr.learn_status, 0) AS learnStatus, " +
            "IFNULL(uwr.is_temp_old, 0) AS isTempOld, IFNULL(uwr.consecutive_known_count, 0) AS consecutiveKnownCount, " +
            "IFNULL(uwr.current_stage, 1) AS currentStage, uwr.next_review_time AS nextReviewTime " +
            "FROM word w " +
            "LEFT JOIN user_word_record uwr ON w.id = uwr.word_id AND uwr.user_id = #{userId} " +
            "WHERE w.book_id = #{bookId} " +
            "  AND (uwr.id IS NULL OR uwr.learn_status = 0) " +
            "ORDER BY w.id ASC " +
            "LIMIT #{limit}")
    List<WordLearningVO> selectNewWords(@Param("userId") Long userId, @Param("bookId") Long bookId, @Param("limit") int limit);

    /**
     * 【修复点】：动态配额核算：统计用户今日首次学习的新词数量 (连表过滤 bookId)
     */
    @Select("SELECT COUNT(*) FROM user_word_record uwr " +
            "JOIN word w ON uwr.word_id = w.id " +
            "WHERE uwr.user_id = #{userId} AND w.book_id = #{bookId} AND DATE(uwr.created_at) = CURDATE()")
    int countTodayLearnedWords(@Param("userId") Long userId, @Param("bookId") Long bookId);

    /**
     * 【修复点】：动态配额核算 1：统计今天首次学习的新词数量 (连表过滤 bookId)
     */
    @Select("SELECT COUNT(*) FROM user_word_record uwr " +
            "JOIN word w ON uwr.word_id = w.id " +
            "WHERE uwr.user_id = #{userId} AND w.book_id = #{bookId} AND DATE(uwr.created_at) = CURDATE()")
    int countTodayLearnedNewWords(@Param("userId") Long userId, @Param("bookId") Long bookId);

    /**
     * 【修复点】：动态配额核算 2：统计今天复习过的历史旧词数量 (连表过滤 bookId)
     */
    @Select("SELECT COUNT(*) FROM user_word_record uwr " +
            "JOIN word w ON uwr.word_id = w.id " +
            "WHERE uwr.user_id = #{userId} AND w.book_id = #{bookId} AND DATE(uwr.updated_at) = CURDATE() AND DATE(uwr.created_at) < CURDATE()")
    int countTodayReviewedOldWords(@Param("userId") Long userId, @Param("bookId") Long bookId);

    /**
     * 学习进度核算：统计某一用户在特定词书下已存在记录的单词总数
     */
    @Select("SELECT COUNT(uwr.id) " +
            "FROM user_word_record uwr " +
            "JOIN word w ON uwr.word_id = w.id " +
            "WHERE uwr.user_id = #{userId} " +
            "  AND w.book_id = #{bookId}")
    int countLearnedWordsByBook(@Param("userId") Long userId, @Param("bookId") Long bookId);

}