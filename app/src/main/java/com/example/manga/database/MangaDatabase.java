package com.example.manga.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.manga.model.Chapter;
import com.example.manga.model.Manga;

@Database(entities = {Manga.class, Chapter.class}, version = 2, exportSchema = false)
public abstract class MangaDatabase extends RoomDatabase {
    
    public abstract MangaDao mangaDao();
    public abstract ChapterDao chapterDao();
    
    private static volatile MangaDatabase INSTANCE;
    
    public static MangaDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (MangaDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            MangaDatabase.class,
                            "manga_database"
                    )
                    .addMigrations(MIGRATION_1_2)
                    .build();
                }
            }
        }
        return INSTANCE;
    }
    
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // 添加 totalChapters 列
            database.execSQL("ALTER TABLE manga_table ADD COLUMN totalChapters INTEGER NOT NULL DEFAULT 0");
        }
    };
} 