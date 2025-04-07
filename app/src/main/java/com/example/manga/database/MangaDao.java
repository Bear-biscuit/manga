package com.example.manga.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.manga.model.Manga;

import java.util.List;

@Dao
public interface MangaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Manga manga);
    
    @Update
    void update(Manga manga);
    
    @Delete
    void delete(Manga manga);
    
    @Query("SELECT * FROM manga_table WHERE path = :path")
    Manga getMangaByPath(String path);
    
    @Query("SELECT * FROM manga_table ORDER BY lastReadTime DESC")
    LiveData<List<Manga>> getAllMangaByLastRead();
    
    @Query("SELECT * FROM manga_table WHERE isFavorite = 1 ORDER BY title ASC")
    LiveData<List<Manga>> getFavoriteManga();
    
    @Query("SELECT * FROM manga_table ORDER BY title ASC")
    LiveData<List<Manga>> getAllMangaAlphabetically();
    
    @Query("UPDATE manga_table SET isFavorite = :isFavorite WHERE path = :path")
    void updateFavoriteStatus(String path, boolean isFavorite);
    
    @Query("UPDATE manga_table SET lastReadTime = :timestamp WHERE path = :path")
    void updateLastReadTime(String path, long timestamp);
} 