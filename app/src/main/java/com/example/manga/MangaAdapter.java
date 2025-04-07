package com.example.manga;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class MangaAdapter extends BaseAdapter {
    private Context context;
    private List<MangaItem> mangaList;
    private LayoutInflater inflater;

    public MangaAdapter(Context context, List<MangaItem> mangaList) {
        this.context = context;
        this.mangaList = mangaList;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return mangaList.size();
    }

    @Override
    public Object getItem(int position) {
        return mangaList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.manga_grid_item, parent, false);
            holder = new ViewHolder();
            holder.titleTextView = convertView.findViewById(R.id.manga_title);
            holder.coverImageView = convertView.findViewById(R.id.manga_cover);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        MangaItem mangaItem = mangaList.get(position);
        holder.titleTextView.setText(mangaItem.getTitle());
        holder.coverImageView.setImageResource(mangaItem.getImageResourceId());

        return convertView;
    }

    private static class ViewHolder {
        TextView titleTextView;
        ImageView coverImageView;
    }
} 