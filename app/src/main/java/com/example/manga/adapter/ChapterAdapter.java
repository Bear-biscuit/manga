package com.example.manga.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.manga.R;
import com.example.manga.model.Chapter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChapterAdapter extends RecyclerView.Adapter<ChapterAdapter.ChapterViewHolder> {
    private final Context context;
    private List<Chapter> chapterList;
    private final OnChapterClickListener listener;
    private final SimpleDateFormat dateFormat;
    
    public interface OnChapterClickListener {
        void onChapterClick(Chapter chapter);
    }
    
    public ChapterAdapter(Context context, OnChapterClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    }
    
    public void setChapterList(List<Chapter> chapterList) {
        this.chapterList = chapterList;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ChapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.chapter_list_item, parent, false);
        return new ChapterViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ChapterViewHolder holder, int position) {
        if (chapterList != null && position < chapterList.size()) {
            Chapter chapter = chapterList.get(position);
            holder.titleTextView.setText(chapter.getTitle());
            
            // 显示上次阅读信息
            if (chapter.getLastReadTime() > 0) {
                String lastRead = dateFormat.format(new Date(chapter.getLastReadTime()));
                String progress = context.getString(R.string.page_number, chapter.getLastReadPage() + 1, chapter.getTotalPages());
                holder.progressTextView.setText(String.format("%s - %s", lastRead, progress));
                holder.progressTextView.setVisibility(View.VISIBLE);
            } else {
                holder.progressTextView.setVisibility(View.GONE);
            }
            
            // 设置点击事件
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    // 添加日志记录
                    android.util.Log.d("ChapterAdapter", "Chapter clicked: " + chapter.getTitle() 
                            + ", path: " + chapter.getPath()
                            + ", manga path: " + chapter.getMangaPath());
                    listener.onChapterClick(chapter);
                }
            });
        }
    }
    
    @Override
    public int getItemCount() {
        return chapterList != null ? chapterList.size() : 0;
    }
    
    static class ChapterViewHolder extends RecyclerView.ViewHolder {
        final TextView titleTextView;
        final TextView progressTextView;
        
        ChapterViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.chapter_title);
            progressTextView = itemView.findViewById(R.id.chapter_progress);
        }
    }
} 