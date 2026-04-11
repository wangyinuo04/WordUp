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
     * 逻辑：is_temp_old = 1，按更新时间升序（最久未背的优先推送）
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
     * 逻辑：处于学习中 (learn_status = 1)，非临时旧词 (is_temp_old = 0)，且下次复习时间小于等于当前时间，按词书基准顺序
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
     * 逻辑：左连接用户记录表，当记录不存在(uwr.id IS NULL)或未学习(learn_status = 0)时，按词书基准顺序推送
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
     * 动态配额核算：统计用户今日首次学习的新词数量
     * 逻辑：通过 created_at 字段匹配今天日期
     */
    @Select("SELECT COUNT(*) FROM user_word_record WHERE user_id = #{userId} AND DATE(created_at) = CURDATE()")
    int countTodayLearnedWords(@Param("userId") Long userId);

    /**
     * 动态配额核算 1：统计今天首次学习的新词数量
     * 逻辑：通过 created_at 字段匹配今天日期
     */
    @Select("SELECT COUNT(*) FROM user_word_record WHERE user_id = #{userId} AND DATE(created_at) = CURDATE()")
    int countTodayLearnedNewWords(@Param("userId") Long userId);

    /**
     * 动态配额核算 2：统计今天复习过的历史旧词数量
     * 逻辑：今天更新过状态 (updated_at = 今天)，且不是今天刚创建的词 (created_at < 今天)
     */
    @Select("SELECT COUNT(*) FROM user_word_record WHERE user_id = #{userId} AND DATE(updated_at) = CURDATE() AND DATE(created_at) < CURDATE()")
    int countTodayReviewedOldWords(@Param("userId") Long userId);

    /**
     * 学习进度核算：统计某一用户在特定词书下已存在记录的单词总数
     * 逻辑：联查 word 表过滤 book_id，精准匹配 user_id 保障数据隔离
     */
    @Select("SELECT COUNT(uwr.id) " +
            "FROM user_word_record uwr " +
            "JOIN word w ON uwr.word_id = w.id " +
            "WHERE uwr.user_id = #{userId} " +
            "  AND w.book_id = #{bookId}")
    int countLearnedWordsByBook(@Param("userId") Long userId, @Param("bookId") Long bookId);

}