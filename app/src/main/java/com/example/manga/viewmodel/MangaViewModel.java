package com.example.manga.viewmodel;

import android.app.Application;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.manga.model.Chapter;
import com.example.manga.model.Manga;
import com.example.manga.repository.MangaRepository;

import java.util.List;

public class MangaViewModel extends AndroidViewModel {
    private final MangaRepository repository;
    private final MutableLiveData<Boolean> isLoading;
    private final MutableLiveData<String> errorMessage;
    private static final String DEFAULT_MANGA_FOLDER = Environment.getExternalStorageDirectory() + "/manga";
    
    public MangaViewModel(@NonNull Application application) {
        super(application);
        repository = new MangaRepository(application);
        isLoading = new MutableLiveData<>(false);
        errorMessage = new MutableLiveData<>();
    }
    
    // 获取所有漫画
    public LiveData<List<Manga>> getAllManga() {
        return repository.getAllMangaAlphabetically();
    }
    
    // 获取最近阅读的漫画
    public LiveData<List<Manga>> getRecentManga() {
        return repository.getAllMangaByLastRead();
    }
    
    // 获取收藏的漫画
    public LiveData<List<Manga>> getFavoriteManga() {
        return repository.getFavoriteManga();
    }
    
    // 获取特定漫画的章节
    public LiveData<List<Chapter>> getChapters(String mangaPath) {
        if (mangaPath == null) {
            // 当mangaPath为null时，返回所有章节
            return repository.getAllChapters();
        }
        return repository.getChaptersByManga(mangaPath);
    }
    
    // 获取最近阅读的章节
    public LiveData<List<Chapter>> getRecentChapters() {
        return repository.getRecentChapters();
    }
    
    // 设置收藏状态
    public void setFavorite(String mangaPath, boolean isFavorite) {
        repository.updateFavoriteStatus(mangaPath, isFavorite);
    }
    
    // 更新阅读进度
    public void updateReadProgress(String chapterPath, int page) {
        long timestamp = System.currentTimeMillis();
        Log.d("MangaViewModel", "Updating read progress - chapter: " + chapterPath 
                + ", page: " + page + ", time: " + timestamp);
        repository.updateReadProgress(chapterPath, page, timestamp);
    }
    
    // 更新漫画的最后阅读时间
    public void updateMangaLastReadTime(String mangaPath) {
        long timestamp = System.currentTimeMillis();
        repository.updateLastReadTime(mangaPath, timestamp);
    }
    
    // 扫描漫画目录
    public void scanMangaDirectory(String directoryPath) {
        if (directoryPath == null || directoryPath.isEmpty()) {
            Log.e("MangaViewModel", "无效的目录路径: 为空");
            errorMessage.postValue("无效的目录路径");
            isLoading.postValue(false);
            return;
        }
        
        isLoading.setValue(true);
        
        Log.d("MangaViewModel", "开始扫描目录: " + directoryPath);
        
        try {
            repository.scanMangaDirectory(directoryPath, mangaList -> {
                isLoading.postValue(false);
                
                if (mangaList == null) {
                    Log.e("MangaViewModel", "扫描返回空结果，目录: " + directoryPath);
                    errorMessage.postValue("扫描目录时出错");
                    return;
                }
                
                if (mangaList.isEmpty()) {
                    Log.e("MangaViewModel", "目录中未找到漫画: " + directoryPath);
                    errorMessage.postValue("未找到漫画，请确保目录包含漫画文件夹");
                } else {
                    Log.d("MangaViewModel", "在目录 " + directoryPath + " 中找到 " + mangaList.size() + " 本漫画");
                }
            });
        } catch (Exception e) {
            Log.e("MangaViewModel", "扫描漫画目录时出错: " + e.getMessage(), e);
            errorMessage.postValue("扫描目录时出错: " + e.getMessage());
            isLoading.postValue(false);
        }
    }
    
    // 获取章节的所有页面
    public void getChapterPages(String chapterPath, MangaRepository.Callback<List<String>> callback) {
        repository.getChapterPages(chapterPath, callback);
    }
    
    // 获取漫画的最后阅读章节
    public void getLastReadChapter(String mangaPath, MangaRepository.Callback<Chapter> callback) {
        repository.getLastReadChapter(mangaPath, callback);
    }
    
    // 获取默认漫画文件夹
    public String getDefaultMangaFolder() {
        return DEFAULT_MANGA_FOLDER;
    }
    
    // 获取加载状态
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    // 获取错误消息
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    // 通过路径直接获取章节
    public void getChapterByPath(String chapterPath, MangaRepository.Callback<Chapter> callback) {
        repository.getChapterByPath(chapterPath, callback);
    }
    
    // 获取上一章
    public void getPreviousChapter(String mangaPath, int currentChapterNumber, MangaRepository.Callback<Chapter> callback) {
        repository.getPreviousChapter(mangaPath, currentChapterNumber, callback);
    }
    
    // 获取下一章
    public void getNextChapter(String mangaPath, int currentChapterNumber, MangaRepository.Callback<Chapter> callback) {
        repository.getNextChapter(mangaPath, currentChapterNumber, callback);
    }
} 