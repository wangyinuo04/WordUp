package com.example.appbackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.appbackend.entity.User;
import com.example.appbackend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.web.multipart.MultipartFile;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import java.io.File;
import java.io.IOException;

@RestController
public class LoginController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // 使用 PostMapping 更安全，因为账号密码不能直接暴露在浏览器地址栏里
    @PostMapping("/login")
    public String login(@RequestParam("username") String username, @RequestParam("password") String password)  {

        // 1. 去 MySQL 数据库里查有没有这个账号和密码
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username).eq("password", password);
        User user = userMapper.selectOne(queryWrapper);

        // 2. 判断是否登录成功
        if (user == null) {
            return "登录失败：账号或密码错误！";
        }

        // 3. 登录成功，生成一段随机的 Token（也就是给 App 发放的门禁卡）
        // UUID 会生成类似 550e8400-e29b-41d4-a716-446655440000 这样的字符串，我们把横杠去掉
        String token = UUID.randomUUID().toString().replace("-", "");

        // 4. 把 Token 存进 Redis，设置 2 小时过期
        // 键名格式为：token:admin，对应的值就是这一长串 UUID
        stringRedisTemplate.opsForValue().set("token:" + username, token, 2, TimeUnit.HOURS);

        return "登录成功！你的专属 Token 是：" + token;
    }

    // 新增的注册接口
    @PostMapping("/register")
    public String register(@RequestParam("username") String username, @RequestParam("password") String password) {

        // 1. 先去数据库查一查，这个名字是不是已经被人抢注了
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        // selectCount 是 MyBatis-Plus 自带的统计行数的方法
        Long count = userMapper.selectCount(queryWrapper);

        if (count > 0) {
            return "注册失败：该账号已被注册，换个名字吧！";
        }

        // 2. 名字没被占用，创建一个新用户实体
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(password);

        // 3. 直接存入 MySQL 数据库（MyBatis-Plus 的 insert 方法全自动搞定）
        userMapper.insert(newUser);

        return "注册成功！请直接点击登录体验。";
    }

    // 新增的头像上传接口
    @PostMapping("/uploadAvatar")
    public String uploadAvatar(@RequestParam("file") MultipartFile file, @RequestParam("username") String username) {

        if (file.isEmpty()) {
            return "上传失败：图片为空";
        }

        try {
            // 1. 确定保存图片的本地物理路径（自动在项目根目录下生成 uploads 文件夹）
            String uploadDir = System.getProperty("user.dir") + File.separator + "uploads" + File.separator;
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs(); // 文件夹不存在就自动创建
            }

            // 2. 给图片重新起个名字（防止重名覆盖）
            String originalFilename = file.getOriginalFilename();
            // 提取后缀名（比如 .jpg, .png），做个防空判断
            String suffix = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
            // 用 UUID 生成唯一文件名（去掉横杠保持整洁）
            String newFilename = UUID.randomUUID().toString().replace("-", "") + suffix;

            // 3. 核心：将网络传输过来的文件，真正写入到你电脑的硬盘里
            File dest = new File(uploadDir + newFilename);
            file.transferTo(dest);

            // 4. 拼接出这张图片的网络访问 URL（模拟器专属访问本机的 10.0.2.2 魔法 IP）
            String avatarUrl = "http://10.0.2.2:8080/uploads/" + newFilename;

            // 5. 顶级 MyBatis-Plus 魔法：直接精准更新对应用户的 avatar_url 字段
            UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
            // 寻找账号匹配的行，并把刚生成的 url 塞进 avatar_url 字段
            updateWrapper.eq("username", username).set("avatar_url", avatarUrl);
            // 执行更新（传入 null 代表我们不需要传入完整的 User 实体对象，直接按 Wrapper 里的 set 规则更新即可）
            userMapper.update(null, updateWrapper);

            // 6. 成功后，把这个 URL 返回给 Android 端
            return avatarUrl;

        } catch (IOException e) {
            e.printStackTrace();
            return "上传失败：" + e.getMessage();
        }
    }


}
