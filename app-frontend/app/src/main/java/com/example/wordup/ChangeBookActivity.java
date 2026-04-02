package com.example.wordup;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ChangeBookActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_book);

        // 1. 设置返回按钮（之前已完成）
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // 2. 初始化模拟数据 (模拟从数据库 word_book 表读取的数据)
        List<Book> myBooks = new ArrayList<>();
        myBooks.add(new Book("考研必刷词", 5500, 45));
        myBooks.add(new Book("雅思词汇真经", 3500, 10));
        myBooks.add(new Book("GRE 核心词汇", 3000, 0));

        // 3. 配置 RecyclerView
        RecyclerView rvMyBooks = findViewById(R.id.rvMyBooks);
        rvMyBooks.setLayoutManager(new LinearLayoutManager(this));
        BookAdapter adapter = new BookAdapter(myBooks);
        rvMyBooks.setAdapter(adapter);
    }
}