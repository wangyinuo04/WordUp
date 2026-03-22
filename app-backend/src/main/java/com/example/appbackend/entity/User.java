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

    private String username;
    private String password;
}
