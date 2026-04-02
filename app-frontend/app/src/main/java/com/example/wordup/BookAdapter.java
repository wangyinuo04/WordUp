package com.example.wordup;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {

    private List<Book> bookList;

    public BookAdapter(List<Book> bookList) {
        this.bookList = bookList;
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 加载你之前创建的 item_book.xml
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_book, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        Book book = bookList.get(position);
        holder.tvBookName.setText(book.getBookName());
        holder.tvWordCount.setText("共 " + book.getTotalWords() + " 词");
        holder.pbBookProgress.setProgress(book.getProgress());

        // 这里的文字颜色已经通过 XML 引用了 @color/color_text_primary，保持基调统一
    }

    @Override
    public int getItemCount() {
        return bookList.size();
    }

    static class BookViewHolder extends RecyclerView.ViewHolder {
        TextView tvBookName, tvWordCount;
        ProgressBar pbBookProgress;

        public BookViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBookName = itemView.findViewById(R.id.tvBookName);
            tvWordCount = itemView.findViewById(R.id.tvWordCount);
            pbBookProgress = itemView.findViewById(R.id.pbBookProgress);
        }
    }
}