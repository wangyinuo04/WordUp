package com.example.appbackend.controller;

import com.example.appbackend.Result;
import com.example.appbackend.entity.UserPlan;
import com.example.appbackend.entity.WordBook;
import com.example.appbackend.service.UserPlanService;
import com.example.appbackend.service.WordBookService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 词书对外查询接口控制器
 */
@RestController
@RequestMapping("/api/book")
public class BookController {

    private final WordBookService wordBookService;
    private final UserPlanService userPlanService;

    public BookController(WordBookService wordBookService, UserPlanService userPlanService) {
        this.wordBookService = wordBookService;
        this.userPlanService = userPlanService;
    }

    /**
     * 获取所有可供选择的词书列表
     */
    @GetMapping("/list")
    public Result<List<WordBook>> getBookList() {
        List<WordBook> books = wordBookService.getAllBooks();
        return Result.success(books);
    }

    /**
     * 查询用户当前正在学习的词书名称
     */
    @GetMapping("/current")
    public Result<String> getCurrentBookName(@RequestParam("userId") Long userId) {
        UserPlan plan = userPlanService.getPlanByUserId(userId);
        if (plan == null || plan.getBookId() == null) {
            return Result.error(404, "用户未绑定词书");
        }

        WordBook book = wordBookService.getBookById(plan.getBookId());
        if (book != null) {
            return Result.success("success", book.getBookName());
        }
        return Result.error(404, "未找到对应的词书信息");
    }
}