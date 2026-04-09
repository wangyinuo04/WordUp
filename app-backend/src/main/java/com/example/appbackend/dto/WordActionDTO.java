package com.example.appbackend.dto;

import lombok.Data;

/**
 * 单词操作数据传输对象
 * 用于接收前端发送的用户点击"认识/不认识"操作请求
 */
@Data
public class WordActionDTO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 单词ID
     */
    private Long wordId;

    /**
     * 是否认识 (true: 认识, false: 不认识)
     */
    private Boolean isKnown;
}