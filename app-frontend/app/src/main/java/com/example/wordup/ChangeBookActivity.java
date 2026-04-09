package com.example.wordup;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

/**
 * 换书页面的业务逻辑控制类
 * 已修复：将硬编码 ID 替换为从 SharedPreferences 动态加载。
 */
public class ChangeBookActivity extends AppCompatActivity {

    private RecyclerView rvMyBooks;
    private BookAdapter adapter;

    // 动态获取当前登录用户的 ID
    private long currentUserId;

    private TextView tvCurrentBookName;
    private TextView tvCurrentWordCount;
    private ProgressBar pbCurrentProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_book);

        // 加载当前真实用户 ID
        currentUserId = getSharedPreferences("MyAppConfig", Context.MODE_PRIVATE).getLong("userId", -1L);

        if (currentUserId == -1L) {
            Toast.makeText(this, "用户信息异常，请重新登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initView();
        fetchCurrentBook();
        fetchBookList();
    }

    private void initView() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        View currentBookCard = findViewById(R.id.currentBookCard);
        tvCurrentBookName = currentBookCard.findViewById(R.id.tvBookName);
        tvCurrentWordCount = currentBookCard.findViewById(R.id.tvWordCount);
        pbCurrentProgress = currentBookCard.findViewById(R.id.pbBookProgress);

        rvMyBooks = findViewById(R.id.rvMyBooks);
        rvMyBooks.setLayoutManager(new LinearLayoutManager(this));

        adapter = new BookAdapter(new ArrayList<>());
        rvMyBooks.setAdapter(adapter);

        // 使用动态的 currentUserId 进行更新请求
        adapter.setOnItemClickListener(book -> {
            BookNetworkHelper.updateCurrentBook(currentUserId, book.getId(), new BookNetworkHelper.UpdateBookCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(ChangeBookActivity.this, "已切换为：" + book.getBookName(), Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onFailure(String errorMsg) {
                    Toast.makeText(ChangeBookActivity.this, "切换失败：" + errorMsg, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void fetchCurrentBook() {
        // 使用动态的 currentUserId 查询当前词书
        BookNetworkHelper.getCurrentBook(currentUserId, new BookNetworkHelper.GetCurrentBookCallback() {
            @Override
            public void onSuccess(String bookName) {
                tvCurrentBookName.setText(bookName);
                tvCurrentWordCount.setText("当前学习中...");
                pbCurrentProgress.setProgress(0);
            }

            @Override
            public void onFailure(String errorMsg) {
                tvCurrentBookName.setText("暂无正在学习的词书");
                tvCurrentWordCount.setText("");
                pbCurrentProgress.setProgress(0);
            }
        });
    }

    private void fetchBookList() {
        BookNetworkHelper.getBookList(new BookNetworkHelper.GetBookListCallback() {
            @Override
            public void onSuccess(List<Book> bookList) {
                adapter.updateData(bookList);
            }

            @Override
            public void onFailure(String errorMsg) {
                Toast.makeText(ChangeBookActivity.this, "获取词书列表失败：" + errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}