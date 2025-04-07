package com.example.manga.repository;

import android.app.Application;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;

import com.example.manga.database.ChapterDao;
import com.example.manga.database.MangaDao;
import com.example.manga.database.MangaDatabase;
import com.example.manga.model.Chapter;
import com.example.manga.model.Manga;
import com.example.manga.util.MangaFileUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MangaRepository {
    private final MangaDao mangaDao;
    private final ChapterDao chapterDao;
    private final ExecutorService executorService;
    
    public MangaRepository(Application application) {
        MangaDatabase database = MangaDatabase.getDatabase(application);
        mangaDao = database.mangaDao();
        chapterDao = database.chapterDao();
        executorService = Executors.newFixedThreadPool(4);
    }
    
    // 漫画相关操作
    public LiveData<List<Manga>> getAllMangaByLastRead() {
        return mangaDao.getAllMangaByLastRead();
    }
    
    public LiveData<List<Manga>> getFavoriteManga() {
        return mangaDao.getFavoriteManga();
    }
    
    public LiveData<List<Manga>> getAllMangaAlphabetically() {
        return mangaDao.getAllMangaAlphabetically();
    }
    
    public void insertManga(Manga manga) {
        executorService.execute(() -> mangaDao.insert(manga));
    }
    
    public void updateManga(Manga manga) {
        executorService.execute(() -> mangaDao.update(manga));
    }
    
    public void deleteManga(Manga manga) {
        executorService.execute(() -> mangaDao.delete(manga));
    }
    
    public void updateFavoriteStatus(String mangaPath, boolean isFavorite) {
        executorService.execute(() -> mangaDao.updateFavoriteStatus(mangaPath, isFavorite));
    }
    
    public void updateLastReadTime(String mangaPath, long timestamp) {
        executorService.execute(() -> mangaDao.updateLastReadTime(mangaPath, timestamp));
    }
    
    // 章节相关操作
    public LiveData<List<Chapter>> getChaptersByManga(String mangaPath) {
        return chapterDao.getChaptersByManga(mangaPath);
    }
    
    public LiveData<List<Chapter>> getAllChapters() {
        return chapterDao.getAllChapters();
    }
    
    public LiveData<List<Chapter>> getRecentChapters() {
        return chapterDao.getRecentChapters();
    }
    
    public void insertChapter(Chapter chapter) {
        executorService.execute(() -> chapterDao.insert(chapter));
    }
    
    public void updateChapter(Chapter chapter) {
        executorService.execute(() -> chapterDao.update(chapter));
    }
    
    public void updateReadProgress(String chapterPath, int page, long timestamp) {
        executorService.execute(() -> {
            android.util.Log.d("MangaRepository", "Updating read progress for chapter: " + chapterPath 
                    + ", page: " + page + ", timestamp: " + timestamp);
            chapterDao.updateReadProgress(chapterPath, page, timestamp);
        });
    }
    
    // 文件系统操作
    public void scanMangaDirectory(String directoryPath, final Callback<List<Manga>> callback) {
        executorService.execute(() -> {
            try {
                android.util.Log.d("MangaRepository", "开始扫描漫画目录: " + directoryPath);
                
                if (directoryPath == null || directoryPath.isEmpty()) {
                    android.util.Log.e("MangaRepository", "无效的目录路径: null或空");
                    if (callback != null) {
                        callback.onComplete(new java.util.ArrayList<>());
                    }
                    return;
                }
                
                // 扫描文件系统获取漫画列表
                List<Manga> mangaList = MangaFileUtils.scanMangaDirectory(directoryPath);
                
                if (mangaList == null) {
                    android.util.Log.e("MangaRepository", "扫描返回null结果，创建空列表");
                    mangaList = new java.util.ArrayList<>();
                    if (callback != null) {
                        callback.onComplete(mangaList);
                    }
                    return;
                }
                
                android.util.Log.d("MangaRepository", "扫描到 " + mangaList.size() + " 本漫画");
                
                // 保存扫描到的漫画到数据库
                for (Manga manga : mangaList) {
                    try {
                        if (manga == null || manga.getPath() == null) {
                            android.util.Log.e("MangaRepository", "忽略无效的漫画对象");
                            continue;
                        }
                        
                        // 检查数据库中是否已存在该漫画
                        Manga existingManga = null;
                        try {
                            existingManga = mangaDao.getMangaByPath(manga.getPath());
                        } catch (Exception e) {
                            android.util.Log.e("MangaRepository", "获取现有漫画时出错: " + e.getMessage(), e);
                        }
                        
                        if (existingManga != null) {
                            // 更新现有漫画的信息，但保留用户设置如收藏状态和阅读历史
                            manga.setFavorite(existingManga.isFavorite());
                            manga.setLastReadTime(existingManga.getLastReadTime());
                            manga.setTotalChapters(existingManga.getTotalChapters());
                            try {
                                mangaDao.update(manga);
                                android.util.Log.d("MangaRepository", "更新现有漫画: " + manga.getTitle());
                            } catch (Exception e) {
                                android.util.Log.e("MangaRepository", "更新漫画时出错: " + e.getMessage(), e);
                            }
                        } else {
                            // 插入新漫画
                            try {
                                mangaDao.insert(manga);
                                android.util.Log.d("MangaRepository", "添加新漫画: " + manga.getTitle());
                            } catch (Exception e) {
                                android.util.Log.e("MangaRepository", "插入漫画时出错: " + e.getMessage(), e);
                            }
                        }
                        
                        // 扫描并保存章节信息
                        List<Chapter> chapters = null;
                        try {
                            chapters = MangaFileUtils.getChapters(manga.getPath());
                        } catch (Exception e) {
                            android.util.Log.e("MangaRepository", "获取章节时出错: " + e.getMessage(), e);
                            chapters = new java.util.ArrayList<>();
                        }
                        
                        if (chapters == null) {
                            android.util.Log.e("MangaRepository", "章节列表为null，创建空列表");
                            chapters = new java.util.ArrayList<>();
                        }
                        
                        for (Chapter chapter : chapters) {
                            try {
                                if (chapter == null || chapter.getPath() == null) {
                                    android.util.Log.e("MangaRepository", "忽略无效的章节对象");
                                    continue;
                                }
                                
                                // 检查数据库中是否已存在该章节
                                Chapter existingChapter = null;
                                try {
                                    existingChapter = chapterDao.getChapterByPath(chapter.getPath());
                                } catch (Exception e) {
                                    android.util.Log.e("MangaRepository", "获取现有章节时出错: " + e.getMessage(), e);
                                }
                                
                                if (existingChapter != null) {
                                    // 更新现有章节的信息，但保留阅读进度
                                    chapter.setLastReadPage(existingChapter.getLastReadPage());
                                    chapter.setLastReadTime(existingChapter.getLastReadTime());
                                    try {
                                        chapterDao.update(chapter);
                                    } catch (Exception e) {
                                        android.util.Log.e("MangaRepository", "更新章节时出错: " + e.getMessage(), e);
                                    }
                                } else {
                                    // 插入新章节
                                    try {
                                        chapterDao.insert(chapter);
                                    } catch (Exception e) {
                                        android.util.Log.e("MangaRepository", "插入章节时出错: " + e.getMessage(), e);
                                    }
                                }
                            } catch (Exception e) {
                                android.util.Log.e("MangaRepository", "处理章节时出错: " + e.getMessage(), e);
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.e("MangaRepository", "处理漫画时出错: " + e.getMessage(), e);
                    }
                }
                
                android.util.Log.d("MangaRepository", "漫画目录扫描完成");
                
                if (callback != null) {
                    callback.onComplete(mangaList);
                }
            } catch (Exception e) {
                android.util.Log.e("MangaRepository", "扫描漫画目录时发生异常: " + e.getMessage(), e);
                if (callback != null) {
                    callback.onComplete(new java.util.ArrayList<>());
                }
            }
        });
    }
    
    public void getChapterPages(String chapterPath, final Callback<List<String>> callback) {
        executorService.execute(() -> {
            List<String> pages = MangaFileUtils.getChapterPages(chapterPath);
            if (callback != null) {
                callback.onComplete(pages);
            }
        });
    }
    
    public void getLastReadChapter(String mangaPath, final Callback<Chapter> callback) {
        executorService.execute(() -> {
            Chapter chapter = chapterDao.getLastReadChapterForManga(mangaPath);
            if (callback != null) {
                callback.onComplete(chapter);
            }
        });
    }
    
    public void getChapterByPath(String chapterPath, final Callback<Chapter> callback) {
        executorService.execute(() -> {
            Chapter chapter = chapterDao.getChapterByPath(chapterPath);
            android.util.Log.d("MangaRepository", "Getting chapter by path: " + chapterPath 
                    + ", result: " + (chapter != null ? "found" : "not found"));
            if (callback != null) {
                callback.onComplete(chapter);
            }
        });
    }
    
    // 回调接口
    public interface Callback<T> {
        void onComplete(T result);
    }
} 