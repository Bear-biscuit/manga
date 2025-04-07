package com.example.manga.model;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "manga_table")
public class Manga implements Parcelable {
    @PrimaryKey
    @NonNull
    private String path; // 漫画文件夹的完整路径作为唯一标识
    private String title; // 漫画标题
    private String coverPath; // 封面图片路径
    private long lastReadTime; // 最后阅读时间
    private boolean isFavorite; // 是否收藏
    private int totalChapters; // 总章节数

    public Manga(@NonNull String path, String title, String coverPath) {
        this.path = path;
        this.title = title;
        this.coverPath = coverPath;
        this.lastReadTime = 0;
        this.isFavorite = false;
        this.totalChapters = 0;
    }
    
    // Parcelable 实现
    protected Manga(Parcel in) {
        path = in.readString();
        title = in.readString();
        coverPath = in.readString();
        lastReadTime = in.readLong();
        isFavorite = in.readByte() != 0;
        totalChapters = in.readInt();
    }
    
    public static final Creator<Manga> CREATOR = new Creator<Manga>() {
        @Override
        public Manga createFromParcel(Parcel in) {
            return new Manga(in);
        }

        @Override
        public Manga[] newArray(int size) {
            return new Manga[size];
        }
    };

    @NonNull
    public String getPath() {
        return path;
    }

    public void setPath(@NonNull String path) {
        this.path = path;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCoverPath() {
        return coverPath;
    }

    public void setCoverPath(String coverPath) {
        this.coverPath = coverPath;
    }

    public long getLastReadTime() {
        return lastReadTime;
    }

    public void setLastReadTime(long lastReadTime) {
        this.lastReadTime = lastReadTime;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public int getTotalChapters() {
        return totalChapters;
    }

    public void setTotalChapters(int totalChapters) {
        this.totalChapters = totalChapters;
    }
    
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(path);
        dest.writeString(title);
        dest.writeString(coverPath);
        dest.writeLong(lastReadTime);
        dest.writeByte((byte) (isFavorite ? 1 : 0));
        dest.writeInt(totalChapters);
    }
} 