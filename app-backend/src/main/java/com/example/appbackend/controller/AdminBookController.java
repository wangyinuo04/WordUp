package com.example.appbackend.controller;

import com.example.appbackend.Result;
import com.example.appbackend.service.WordBookService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 词书数据管理接口控制器
 */
@RestController
@RequestMapping("/api/admin/book")
public class AdminBookController {

    private final WordBookService wordBookService;

    public AdminBookController(WordBookService wordBookService) {
        this.wordBookService = wordBookService;
    }

    /**
     * 上传并解析词书文件入库
     */
    @PostMapping("/upload")
    public Result<Void> uploadBook(@RequestParam("bookName") String bookName,
                                   @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error(400, "上传的文件不能为空");
        }
        try {
            wordBookService.importBookData(bookName, file);
            return Result.success("词书《" + bookName + "》导入成功！", null);
        } catch (Exception e) {
            return Result.error(500, "词书导入失败：" + e.getMessage());
        }
    }
}