package com.example.appbackend.mapper;

import com.example.appbackend.entity.UserPlan;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户学习计划与设置数据访问接口
 */
@Mapper
public interface UserPlanMapper {

    @Select("SELECT * FROM user_plan WHERE user_id = #{userId}")
    UserPlan selectByUserId(Long userId);

    @Update("UPDATE user_plan SET anti_sleep_on = #{antiSleepOn}, ai_sentence_on = #{aiSentenceOn}, emotion_recog_on = #{emotionRecogOn} WHERE user_id = #{userId}")
    int updateAiSettings(UserPlan userPlan);

    /**
     * 仅更新用户当前关联的词书 ID
     */
    @Update("UPDATE user_plan SET book_id = #{bookId} WHERE user_id = #{userId}")
    int updateBookId(@Param("userId") Long userId, @Param("bookId") Long bookId);

    /**
     * 更新用户每日学习目标总数（新词与复习词总和）
     */
    @Update("UPDATE user_plan SET daily_target = #{dailyTarget} WHERE user_id = #{userId}")
    int updateDailyTarget(@Param("userId") Long userId, @Param("dailyTarget") Integer dailyTarget);

    /**
     * 【新增】为新注册用户初始化默认的学习计划与 AI 设置
     * 注意：因为数据库中 book_id 为 NOT NULL，此处默认分配 book_id = 1 作为初始词书
     */
    @Insert("INSERT INTO user_plan (user_id, book_id, daily_target, anti_sleep_on, ai_sentence_on, emotion_recog_on) " +
            "VALUES (#{userId}, #{bookId}, 150, 0, 0, 0)")
    int insertDefaultPlan(@Param("userId") Long userId, @Param("bookId") Long bookId);
}