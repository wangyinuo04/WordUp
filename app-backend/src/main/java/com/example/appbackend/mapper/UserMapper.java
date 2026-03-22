package com.example.appbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.appbackend.entity.User; // 引入你刚才建的 User 类
import org.apache.ibatis.annotations.Mapper;

@Mapper // 让 Spring Boot 知道这是一个用来操作数据库的接口
public interface UserMapper extends BaseMapper<User> {
    // 没错，里面什么都不用写！
    // 继承了 BaseMapper 之后，增删改查的方法它已经全部自动帮你写好了！
}