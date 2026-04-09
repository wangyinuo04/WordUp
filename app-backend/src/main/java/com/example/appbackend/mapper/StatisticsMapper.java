package com.example.appbackend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 统计模块专用数据访问接口
 * 负责处理复杂的聚合查询与多表联查
 */
@Mapper
public interface StatisticsMapper {

    /**
     * 记忆漏斗聚合查询
     * 统计指定用户在各个复习阶段的单词分布数量
     *
     * @param userId 用户ID
     * @return 包含阶段(stage)与数量(wordCount)的映射列表
     */
    @Select("SELECT current_stage AS stage, COUNT(*) AS wordCount " +
            "FROM user_word_record " +
            "WHERE user_id = #{userId} AND learn_status > 0 " +
            "GROUP BY current_stage")
    List<Map<String, Object>> getMemoryFunnelStageCounts(@Param("userId") Long userId);

    /**
     * 高频错词聚合查询
     * 联合 user_word_record 与 word 表，获取错误次数最多的前 N 个单词
     *
     * @param userId 用户ID
     * @param limit  提取数量限制
     * @return 包含单词拼写(word)与错误次数权重(weight)的映射列表
     */
    @Select("SELECT w.spelling AS word, uwr.error_count AS weight " +
            "FROM user_word_record uwr " +
            "JOIN word w ON uwr.word_id = w.id " +
            "WHERE uwr.user_id = #{userId} AND uwr.error_count > 0 " +
            "ORDER BY uwr.error_count DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> getTopHardWords(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 学习热力图趋势查询
     * 获取指定日期范围内的每日复习量数据
     *
     * @param userId    用户ID
     * @param startDate 起始日期
     * @return 包含日期(recordDate)与复习量(reviewedCount)的映射列表
     */
    @Select("SELECT record_date AS recordDate, reviewed_count AS reviewedCount " +
            "FROM user_daily_stats " +
            "WHERE user_id = #{userId} AND record_date >= #{startDate} " +
            "ORDER BY record_date ASC")
    List<Map<String, Object>> getHeatmapStats(@Param("userId") Long userId, @Param("startDate") LocalDate startDate);
}