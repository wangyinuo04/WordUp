package com.example.appbackend.controller;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.appbackend.entity.User;
import com.example.appbackend.mapper.UserMapper;
import com.example.appbackend.Result; // <--- 这里的路径修正了
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin(origins = "*") // 解决模拟器跨域问题
public class UserController {

    @Autowired
    private UserMapper userMapper;

    /**
     * 资料更新接口
     * 接收前端：{"username":"xxx", "nickname":"新名字"} 这种格式
     */
    @PostMapping("/updateProfile")
    public Result<String> updateProfile(@RequestBody Map<String, String> params) {
        String username = params.get("username");

        // 1. 基础校验
        if (username == null || username.isEmpty()) {
            return Result.error("用户名不能为空");
        }

        // 2. 找出除了 username 之外的那个 key
        String updateKey = "";
        String updateValue = "";

        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            if (!key.equals("username")) {
                updateKey = key;
                updateValue = entry.getValue();
                break;
            }
        }

        if (updateKey.isEmpty()) {
            return Result.error("未检测到需要修改的字段");
        }

        // 3. 使用 MyBatis-Plus 执行更新
        // 这里会根据前端传的 key（如 gender, grade）自动匹配数据库字段
        UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("username", username).set(updateKey, updateValue);

        try {
            int rows = userMapper.update(null, updateWrapper);
            if (rows > 0) {
                System.out.println("成功更新 " + username + " 的 " + updateKey + " 为: " + updateValue);
                return Result.success("更新成功");
            } else {
                return Result.error("更新失败，用户不存在");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("数据库操作异常：" + e.getMessage());
        }
    }
}