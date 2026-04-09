package com.example.appbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.appbackend.entity.UserDailyStats;
import org.apache.ibatis.annotations.Mapper;

/**
 * 每日学习数据聚合表数据访问接口
 */
@Mapper
public interface UserDailyStatsMapper extends BaseMapper<UserDailyStats> {
}