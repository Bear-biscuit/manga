package com.example.manga.model;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "chapter_table")
public class Chapter implements Parcelable {
    @PrimaryKey
    @NonNull
    private String path; // 章节文件夹的完整路径作为唯一标识
    private String mangaPath; // 所属漫画的路径
    private String title; // 章节标题
    private int chapterNumber; // 章节号
    private int lastReadPage; // 上次阅读的页码
    private long lastReadTime; // 最后阅读时间
    private int totalPages; // 总页数

    public Chapter(@NonNull String path, String mangaPath, String title, int chapterNumber) {
        this.path = path;
        this.mangaPath = mangaPath;
        this.title = title;
        this.chapterNumber = chapterNumber;
        this.lastReadPage = 0;
        this.lastReadTime = 0;
        this.totalPages = 0;
    }
    
    // Parcelable 实现
    protected Chapter(Parcel in) {
        path = in.readString();
        mangaPath = in.readString();
        title = in.readString();
        chapterNumber = in.readInt();
        lastReadPage = in.readInt();
        lastReadTime = in.readLong();
        totalPages = in.readInt();
    }

    public static final Creator<Chapter> CREATOR = new Creator<Chapter>() {
        @Override
        public Chapter createFromParcel(Parcel in) {
            return new Chapter(in);
        }

        @Override
        public Chapter[] newArray(int size) {
            return new Chapter[size];
        }
    };

    @NonNull
    public String getPath() {
        return path;
    }

    public void setPath(@NonNull String path) {
        this.path = path;
    }

    public String getMangaPath() {
        return mangaPath;
    }

    public void setMangaPath(String mangaPath) {
        this.mangaPath = mangaPath;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getChapterNumber() {
        return chapterNumber;
    }

    public void setChapterNumber(int chapterNumber) {
        this.chapterNumber = chapterNumber;
    }

    public int getLastReadPage() {
        return lastReadPage;
    }

    public void setLastReadPage(int lastReadPage) {
        this.lastReadPage = lastReadPage;
    }

    public long getLastReadTime() {
        return lastReadTime;
    }

    public void setLastReadTime(long lastReadTime) {
        this.lastReadTime = lastReadTime;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
    
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(path);
        dest.writeString(mangaPath);
        dest.writeString(title);
        dest.writeInt(chapterNumber);
        dest.writeInt(lastReadPage);
        dest.writeLong(lastReadTime);
        dest.writeInt(totalPages);
    }
} 