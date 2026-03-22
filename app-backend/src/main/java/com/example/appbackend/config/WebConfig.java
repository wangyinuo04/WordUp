package com.example.appbackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 获取当前项目的根目录路径，并在其下寻找/创建 uploads 文件夹
        String uploadDir = System.getProperty("user.dir") + File.separator + "uploads" + File.separator;

        // 将 /uploads/** 的网络请求映射到本地的物理文件夹
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir);
    }
}