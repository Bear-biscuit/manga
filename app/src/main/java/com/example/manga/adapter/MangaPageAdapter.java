package com.example.manga.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.manga.R;

import java.io.File;
import java.util.List;

public class MangaPageAdapter extends RecyclerView.Adapter<MangaPageAdapter.PageViewHolder> {
    private final Context context;
    private List<String> pageList; // 页面图片路径列表
    
    public MangaPageAdapter(Context context) {
        this.context = context;
    }
    
    public void setPageList(List<String> pageList) {
        this.pageList = pageList;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.manga_page_item, parent, false);
        return new PageViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        if (pageList != null && position < pageList.size()) {
            String pagePath = pageList.get(position);
            
            // 使用Glide加载图片
            Glide.with(context)
                    .load(new File(pagePath))
                    .placeholder(R.drawable.ic_manga_placeholder)
                    .error(R.drawable.ic_manga_placeholder)
                    .into(holder.pageImageView);
        }
    }
    
    @Override
    public int getItemCount() {
        return pageList != null ? pageList.size() : 0;
    }
    
    static class PageViewHolder extends RecyclerView.ViewHolder {
        final ImageView pageImageView;
        
        PageViewHolder(@NonNull View itemView) {
            super(itemView);
            pageImageView = itemView.findViewById(R.id.manga_page_image);
        }
    }
} 