package com.example.appbackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.appbackend.Result;
import com.example.appbackend.entity.User;
import com.example.appbackend.mapper.UserMapper;
import com.example.appbackend.mapper.UserPlanMapper; // 新增引入
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
public class LoginController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserPlanMapper userPlanMapper; // 新增注入用于初始化设置

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // 登录接口 (保留了板块一中新增的 userId 下发逻辑)
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestParam("username") String username, @RequestParam("password") String password)  {

        // 1. 查询数据库匹配账号密码
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username).eq("password", password);
        User user = userMapper.selectOne(queryWrapper);

        // 2. 校验登录状态
        if (user == null) {
            return Result.error(400, "登录失败：账号或密码错误！");
        }

        // 3. 生成授权 Token
        String token = UUID.randomUUID().toString().replace("-", "");

        // 4. 将 Token 存入 Redis，设置 2 小时过期时长
        stringRedisTemplate.opsForValue().set("token:" + username, token, 2, TimeUnit.HOURS);

        // 5. 封装多维度返回数据
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("token", token);
        responseData.put("userId", user.getId()); // 下发真实用户ID
        responseData.put("avatar_url", user.getAvatarUrl() != null ? user.getAvatarUrl() : "");

        return Result.success("登录成功！", responseData);
    }

    // 注册接口 (新增初始化 user_plan 逻辑)
    @PostMapping("/register")
    public Result<Object> register(@RequestParam("username") String username, @RequestParam("password") String password) {

        // 1. 校验账号是否被占用
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        Long count = userMapper.selectCount(queryWrapper);

        if (count > 0) {
            return Result.error(400, "注册失败：该账号已被注册，换个名字吧！");
        }

        // 2. 创建新用户实体
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(password);

        // 3. 存入 user 表
        userMapper.insert(newUser);

        // 4. 获取 MyBatis-Plus 自动回写的自增主键 ID
        Long newUserId = newUser.getId().longValue();

        // 5. 【修复核心】在 user_plan 表中为该新用户初始化默认配置记录
        // 传入新用户 ID，并预设分配 ID 为 1 的词书作为初始词书
        userPlanMapper.insertDefaultPlan(newUserId, 1L);

        return Result.success("注册成功！请直接点击登录体验。", null);
    }

    // 头像上传接口
    @PostMapping("/uploadAvatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file, @RequestParam("username") String username) {

        if (file.isEmpty()) {
            return Result.error(400, "上传失败：图片为空");
        }

        try {
            String uploadDir = System.getProperty("user.dir") + File.separator + "uploads" + File.separator;
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String originalFilename = file.getOriginalFilename();
            String suffix = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
            String newFilename = UUID.randomUUID().toString().replace("-", "") + suffix;

            File dest = new File(uploadDir + newFilename);
            file.transferTo(dest);

            String avatarUrl = "http://10.0.2.2:8080/uploads/" + newFilename;

            UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("username", username).set("avatar_url", avatarUrl);
            userMapper.update(null, updateWrapper);

            return Result.success("头像上传成功", avatarUrl);

        } catch (IOException e) {
            e.printStackTrace();
            return Result.error(500, "服务器保存图片失败：" + e.getMessage());
        }
    }
}