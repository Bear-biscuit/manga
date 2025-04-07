package com.example.manga.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.manga.R;
import com.example.manga.model.Manga;

import java.io.File;
import java.util.List;

public class MangaGridAdapter extends RecyclerView.Adapter<MangaGridAdapter.MangaViewHolder> {
    private final Context context;
    private List<Manga> mangaList;
    private final OnMangaClickListener listener;
    
    public interface OnMangaClickListener {
        void onMangaClick(Manga manga);
    }
    
    public MangaGridAdapter(Context context, OnMangaClickListener listener) {
        this.context = context;
        this.listener = listener;
    }
    
    public void setMangaList(List<Manga> mangaList) {
        this.mangaList = mangaList;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public MangaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.manga_grid_item, parent, false);
        return new MangaViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull MangaViewHolder holder, int position) {
        if (mangaList != null && position < mangaList.size()) {
            Manga manga = mangaList.get(position);
            holder.titleTextView.setText(manga.getTitle());
            
            // 加载封面图片
            if (manga.getCoverPath() != null) {
                Glide.with(context)
                        .load(new File(manga.getCoverPath()))
                        .placeholder(R.drawable.ic_manga_placeholder)
                        .error(R.drawable.ic_manga_placeholder)
                        .into(holder.coverImageView);
            } else {
                holder.coverImageView.setImageResource(R.drawable.ic_manga_placeholder);
            }
            
            // 设置点击事件
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMangaClick(manga);
                }
            });
        }
    }
    
    @Override
    public int getItemCount() {
        return mangaList != null ? mangaList.size() : 0;
    }
    
    static class MangaViewHolder extends RecyclerView.ViewHolder {
        final ImageView coverImageView;
        final TextView titleTextView;
        
        MangaViewHolder(@NonNull View itemView) {
            super(itemView);
            coverImageView = itemView.findViewById(R.id.manga_cover);
            titleTextView = itemView.findViewById(R.id.manga_title);
        }
    }
} 