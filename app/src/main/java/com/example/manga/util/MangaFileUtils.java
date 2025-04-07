package com.example.manga.util;

import android.content.Context;
import android.util.Log;

import com.example.manga.model.Chapter;
import com.example.manga.model.Manga;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MangaFileUtils {
    private static final String TAG = "MangaFileUtils";
    private static final Pattern CHAPTER_PATTERN = Pattern.compile("第(\\d+)章");
    private static final Pattern CHAPTER_NUMBER_PATTERN = Pattern.compile("\\d+");
    private static final String[] SUPPORTED_IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp", ".gif"};

    // 扫描指定目录下的所有漫画
    public static List<Manga> scanMangaDirectory(String directoryPath) {
        List<Manga> mangaList = new ArrayList<>();
        
        try {
            Log.d(TAG, "Scanning manga directory: " + directoryPath);
            
            if (directoryPath == null || directoryPath.isEmpty()) {
                Log.e(TAG, "Invalid directory path: null or empty");
                return mangaList;
            }
            
            File directory = new File(directoryPath);
            if (!directory.exists()) {
                Log.e(TAG, "Manga directory does not exist: " + directoryPath);
                return mangaList;
            }
            
            if (!directory.isDirectory()) {
                Log.e(TAG, "Path is not a directory: " + directoryPath);
                return mangaList;
            }
            
            if (!directory.canRead()) {
                Log.e(TAG, "Cannot read directory (permission denied): " + directoryPath);
                return mangaList;
            }
            
            File[] mangaFolders = directory.listFiles();
            Log.d(TAG, "Files in directory: " + (mangaFolders != null ? mangaFolders.length : 0));
            
            if (mangaFolders == null || mangaFolders.length == 0) {
                Log.e(TAG, "No files found in: " + directoryPath);
                return mangaList;
            }
            
            // 过滤出目录
            List<File> mangaDirectories = new ArrayList<>();
            for (File file : mangaFolders) {
                if (file != null && file.exists() && file.isDirectory()) {
                    mangaDirectories.add(file);
                    Log.d(TAG, "Found potential manga directory: " + file.getName());
                } else if (file != null) {
                    Log.d(TAG, "Skipping non-directory file: " + file.getName());
                }
            }
            
            if (mangaDirectories.isEmpty()) {
                Log.e(TAG, "No manga folders found in: " + directoryPath);
                return mangaList;
            }
            
            for (File mangaFolder : mangaDirectories) {
                try {
                    Log.d(TAG, "Processing manga folder: " + mangaFolder.getName());
                    String coverPath = findCoverImage(mangaFolder.getAbsolutePath());
                    Log.d(TAG, "Cover path: " + (coverPath != null ? coverPath : "null"));
                    
                    Manga manga = new Manga(
                            mangaFolder.getAbsolutePath(),
                            mangaFolder.getName(),
                            coverPath
                    );
                    // 设置章节数量
                    int chapterCount = countChapters(mangaFolder.getAbsolutePath());
                    manga.setTotalChapters(chapterCount);
                    
                    mangaList.add(manga);
                } catch (Exception e) {
                    // 单个漫画处理错误不应该影响其他漫画
                    Log.e(TAG, "Error processing manga folder: " + mangaFolder.getPath(), e);
                }
            }
            
            Log.d(TAG, "Total manga found: " + mangaList.size());
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error scanning manga directory: " + directoryPath, e);
        }
        
        return mangaList;
    }
    
    // 计算章节数量
    private static int countChapters(String mangaPath) {
        if (mangaPath == null || mangaPath.isEmpty()) {
            return 0;
        }
        
        try {
            File mangaFolder = new File(mangaPath);
            if (!mangaFolder.exists() || !mangaFolder.isDirectory()) {
                return 0;
            }
            
            File[] chapterFolders = mangaFolder.listFiles(File::isDirectory);
            if (chapterFolders == null) {
                return 0;
            }
            
            int validChapters = 0;
            for (File folder : chapterFolders) {
                if (countPages(folder.getAbsolutePath()) > 0) {
                    validChapters++;
                }
            }
            
            return validChapters;
        } catch (Exception e) {
            Log.e(TAG, "Error counting chapters: " + e.getMessage(), e);
            return 0;
        }
    }
    
    // 获取漫画的所有章节
    public static List<Chapter> getChapters(String mangaPath) {
        List<Chapter> chapters = new ArrayList<>();
        
        Log.d(TAG, "Getting chapters for manga: " + mangaPath);
        
        File mangaFolder = new File(mangaPath);
        if (!mangaFolder.exists() || !mangaFolder.isDirectory()) {
            Log.e(TAG, "Invalid manga path: " + mangaPath);
            return chapters;
        }
        
        File[] chapterFolders = mangaFolder.listFiles(File::isDirectory);
        if (chapterFolders == null || chapterFolders.length == 0) {
            Log.e(TAG, "No chapter folders found in: " + mangaPath);
            return chapters;
        }
        
        Log.d(TAG, "Found " + chapterFolders.length + " potential chapter folders");
        
        // 按章节排序
        Arrays.sort(chapterFolders, new ChapterComparator());
        
        for (int i = 0; i < chapterFolders.length; i++) {
            File chapterFolder = chapterFolders[i];
            String chapterTitle = chapterFolder.getName();
            int chapterNumber = extractChapterNumber(chapterTitle, i + 1);
            
            Log.d(TAG, "Processing chapter: " + chapterTitle + " with number: " + chapterNumber);
            
            // 只有包含图片的文件夹才会被添加为章节
            int pageCount = countPages(chapterFolder.getAbsolutePath());
            if (pageCount > 0) {
                Chapter chapter = new Chapter(
                        chapterFolder.getAbsolutePath(),
                        mangaPath,
                        chapterTitle,
                        chapterNumber
                );
                
                // 设置总页数
                chapter.setTotalPages(pageCount);
                
                chapters.add(chapter);
                Log.d(TAG, "Added chapter with " + pageCount + " pages");
            } else {
                Log.d(TAG, "Skipping folder with no images: " + chapterTitle);
            }
        }
        
        Log.d(TAG, "Total chapters found: " + chapters.size());
        return chapters;
    }
    
    // 获取章节中的所有图片
    public static List<String> getChapterPages(String chapterPath) {
        List<String> pages = new ArrayList<>();
        
        File chapterFolder = new File(chapterPath);
        if (!chapterFolder.exists() || !chapterFolder.isDirectory()) {
            Log.e(TAG, "Invalid chapter path: " + chapterPath);
            return pages;
        }
        
        File[] imageFiles = chapterFolder.listFiles(file -> {
            if (file.isFile()) {
                String name = file.getName().toLowerCase();
                for (String ext : SUPPORTED_IMAGE_EXTENSIONS) {
                    if (name.endsWith(ext)) {
                        return true;
                    }
                }
            }
            return false;
        });
        
        if (imageFiles == null || imageFiles.length == 0) {
            Log.e(TAG, "No image files found in: " + chapterPath);
            return pages;
        }
        
        // 按文件名排序
        Arrays.sort(imageFiles, Comparator.comparing(File::getName));
        
        for (File imageFile : imageFiles) {
            pages.add(imageFile.getAbsolutePath());
        }
        
        return pages;
    }
    
    // 从章节名称中提取章节号
    private static int extractChapterNumber(String chapterTitle, int defaultNumber) {
        // 首先尝试标准格式 "第X章"
        Matcher matcher = CHAPTER_PATTERN.matcher(chapterTitle);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                Log.e(TAG, "Failed to parse chapter number from: " + chapterTitle, e);
            }
        }
        
        // 尝试直接从文件夹名称中提取数字
        matcher = CHAPTER_NUMBER_PATTERN.matcher(chapterTitle);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group());
            } catch (NumberFormatException e) {
                Log.e(TAG, "Failed to parse number from: " + chapterTitle, e);
            }
        }
        
        // 最后尝试特殊格式，例如"chapter X"或"ch X"
        String lowerTitle = chapterTitle.toLowerCase();
        if (lowerTitle.contains("chapter") || lowerTitle.contains("ch")) {
            String[] parts = lowerTitle.replace("chapter", " ").replace("ch", " ").split("\\s+");
            for (String part : parts) {
                if (part.matches("\\d+")) {
                    try {
                        return Integer.parseInt(part);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Failed to parse number from part: " + part, e);
                    }
                }
            }
        }
        
        return defaultNumber;
    }
    
    // 查找封面图片
    private static String findCoverImage(String mangaPath) {
        Log.d(TAG, "Finding cover image for manga: " + mangaPath);
        
        File mangaFolder = new File(mangaPath);
        if (!mangaFolder.exists() || !mangaFolder.isDirectory()) {
            Log.e(TAG, "Manga folder doesn't exist or is not a directory: " + mangaPath);
            return null;
        }
        
        // 首先查找章节文件夹中的图片
        File[] chapterFolders = mangaFolder.listFiles(File::isDirectory);
        
        if (chapterFolders != null && chapterFolders.length > 0) {
            Log.d(TAG, "Found " + chapterFolders.length + " chapter folders");
            
            // 按章节排序
            Arrays.sort(chapterFolders, new ChapterComparator());
            
            // 获取第一个章节的第一张图片作为封面
            File firstChapter = chapterFolders[0];
            Log.d(TAG, "Looking for images in first chapter: " + firstChapter.getName());
            
            File[] imageFiles = firstChapter.listFiles(file -> {
                if (file.isFile()) {
                    String name = file.getName().toLowerCase();
                    for (String ext : SUPPORTED_IMAGE_EXTENSIONS) {
                        if (name.endsWith(ext)) {
                            return true;
                        }
                    }
                }
                return false;
            });
            
            if (imageFiles != null && imageFiles.length > 0) {
                Log.d(TAG, "Found " + imageFiles.length + " images in first chapter");
                
                // 按文件名排序
                Arrays.sort(imageFiles, Comparator.comparing(File::getName));
                return imageFiles[0].getAbsolutePath();
            } else {
                Log.d(TAG, "No images found in first chapter");
            }
        } else {
            Log.d(TAG, "No chapter folders found in manga folder");
        }
        
        // 如果章节中没有找到图片，查找漫画文件夹中的图片
        Log.d(TAG, "Looking for images directly in manga folder");
        File[] mangaImages = mangaFolder.listFiles(file -> {
            if (file.isFile()) {
                String name = file.getName().toLowerCase();
                for (String ext : SUPPORTED_IMAGE_EXTENSIONS) {
                    if (name.endsWith(ext)) {
                        return true;
                    }
                }
            }
            return false;
        });
        
        if (mangaImages != null && mangaImages.length > 0) {
            Log.d(TAG, "Found " + mangaImages.length + " images in manga folder");
            
            // 按文件名排序
            Arrays.sort(mangaImages, Comparator.comparing(File::getName));
            return mangaImages[0].getAbsolutePath();
        } else {
            Log.d(TAG, "No images found in manga folder");
        }
        
        // 如果没有找到任何图片，返回null
        Log.e(TAG, "No cover image found for manga: " + mangaPath);
        return null;
    }
    
    // 统计章节中的页数
    private static int countPages(String chapterPath) {
        File chapterFolder = new File(chapterPath);
        File[] imageFiles = chapterFolder.listFiles(file -> {
            if (file.isFile()) {
                String name = file.getName().toLowerCase();
                for (String ext : SUPPORTED_IMAGE_EXTENSIONS) {
                    if (name.endsWith(ext)) {
                        return true;
                    }
                }
            }
            return false;
        });
        
        return imageFiles != null ? imageFiles.length : 0;
    }
    
    // 章节文件夹比较器
    private static class ChapterComparator implements Comparator<File> {
        @Override
        public int compare(File f1, File f2) {
            String name1 = f1.getName();
            String name2 = f2.getName();
            
            int num1 = extractChapterNumber(name1, 0);
            int num2 = extractChapterNumber(name2, 0);
            
            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
            
            // 如果章节号相同，按名称排序
            return name1.compareTo(name2);
        }
    }
} 