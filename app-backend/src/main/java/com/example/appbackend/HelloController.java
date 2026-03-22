package com.example.appbackend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    // 核心：让 Spring 自动帮你把配置好的 Redis 工具塞进来
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @GetMapping("/hello")
    public String sayHello() {
        return "Hello Android! 郭组长的后端指挥中心已就绪，请求已收到！";
    }

    // 新增的 Redis 测试接口
    @GetMapping("/testRedis")
    public String testRedis() {
        // 1. 往 Redis 里存入一组键值对 (Key: "Project", Value: "AI-App")
        stringRedisTemplate.opsForValue().set("Project", "AI-App");

        // 2. 紧接着再根据 Key 把刚刚存进去的 Value 取出来
        String value = stringRedisTemplate.opsForValue().get("Project");

        return "太帅了！成功连接 Redis，取出的值是：" + value;
    }
}