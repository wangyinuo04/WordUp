package com.example.appbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user") // 告诉 MyBatis-Plus 这个类对应数据库的 user 表
public class User {

    @TableId(type = IdType.AUTO) // 告诉它这是主键，且是自增的
    private Integer id;

    // --- 基础登录与认证字段 ---
    private String username;
    private String password;
    private String avatarUrl;    // 头像链接 (对应数据库 avatar_url)

    // --- 个人资料与统计字段 (本次新增) ---
    private String nickname;     // 用户昵称
    private String gender;       // 性别
    private String school;       // 学校
    private String grade;        // 年级
    private Integer streakDays;  // 连续打卡天数 (对应数据库 streak_days)

}