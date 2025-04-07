package com.example.manga.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.manga.model.Chapter;

import java.util.List;

@Dao
public interface ChapterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Chapter chapter);
    
    @Update
    void update(Chapter chapter);
    
    @Delete
    void delete(Chapter chapter);
    
    @Query("SELECT * FROM chapter_table WHERE path = :path")
    Chapter getChapterByPath(String path);
    
    @Query("SELECT * FROM chapter_table WHERE mangaPath = :mangaPath ORDER BY chapterNumber ASC")
    LiveData<List<Chapter>> getChaptersByManga(String mangaPath);
    
    @Query("SELECT * FROM chapter_table ORDER BY mangaPath, chapterNumber ASC")
    LiveData<List<Chapter>> getAllChapters();
    
    @Query("SELECT * FROM chapter_table ORDER BY lastReadTime DESC LIMIT 10")
    LiveData<List<Chapter>> getRecentChapters();
    
    @Query("UPDATE chapter_table SET lastReadPage = :page, lastReadTime = :timestamp WHERE path = :path")
    void updateReadProgress(String path, int page, long timestamp);
    
    @Query("SELECT * FROM chapter_table WHERE mangaPath = :mangaPath ORDER BY lastReadTime DESC LIMIT 1")
    Chapter getLastReadChapterForManga(String mangaPath);
    
    @Query("SELECT * FROM chapter_table WHERE mangaPath = :mangaPath AND chapterNumber < :currentChapterNumber ORDER BY chapterNumber DESC LIMIT 1")
    Chapter getPreviousChapter(String mangaPath, int currentChapterNumber);
    
    @Query("SELECT * FROM chapter_table WHERE mangaPath = :mangaPath AND chapterNumber > :currentChapterNumber ORDER BY chapterNumber ASC LIMIT 1")
    Chapter getNextChapter(String mangaPath, int currentChapterNumber);
} 