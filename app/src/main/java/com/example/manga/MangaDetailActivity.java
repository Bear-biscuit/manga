package com.example.manga;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.manga.adapter.ChapterAdapter;
import com.example.manga.model.Chapter;
import com.example.manga.model.Manga;
import com.example.manga.viewmodel.MangaViewModel;
import com.google.android.material.tabs.TabLayout;

import java.io.File;

public class MangaDetailActivity extends AppCompatActivity implements ChapterAdapter.OnChapterClickListener {

    private static final String TAG = "MangaDetailActivity";
    private static final String EXTRA_MANGA_PATH = "com.example.manga.MANGA_PATH";
    
    private String mangaPath;
    private MangaViewModel viewModel;
    private ChapterAdapter chapterAdapter;
    
    private ImageView coverImageView;
    private TextView descriptionTextView;
    private Button favoriteButton;
    private Button continueReadingButton;
    private RecyclerView chapterRecyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private TabLayout tabLayout;
    
    private Manga currentManga;
    private boolean isFavorite = false;

    public static Intent newIntent(Context context, Manga manga) {
        Intent intent = new Intent(context, MangaDetailActivity.class);
        intent.putExtra(EXTRA_MANGA_PATH, manga.getPath());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_manga_detail);

            // 初始化视图
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            
            coverImageView = findViewById(R.id.detail_manga_cover);
            descriptionTextView = findViewById(R.id.detail_manga_description);
            favoriteButton = findViewById(R.id.btn_favorite);
            continueReadingButton = findViewById(R.id.btn_continue_reading);
            
            // 默认禁用继续阅读按钮，直到检查阅读历史
            continueReadingButton.setEnabled(false);
            continueReadingButton.setAlpha(0.5f);
            
            chapterRecyclerView = findViewById(R.id.chapter_recycler_view);
            progressBar = findViewById(R.id.progress_bar);
            emptyView = findViewById(R.id.empty_view);
            tabLayout = findViewById(R.id.tab_layout);
            
            // 设置RecyclerView
            chapterRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            chapterAdapter = new ChapterAdapter(this, this);
            chapterRecyclerView.setAdapter(chapterAdapter);
            
            // 获取漫画路径
            mangaPath = getIntent().getStringExtra(EXTRA_MANGA_PATH);
            if (mangaPath == null) {
                Toast.makeText(this, R.string.error_loading_manga, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            
            // 初始化ViewModel
            viewModel = new ViewModelProvider(this).get(MangaViewModel.class);
            
            // 加载漫画详情
            loadMangaDetails();
            
            // 设置按钮点击事件
            favoriteButton.setOnClickListener(v -> {
                if (currentManga != null) {
                    isFavorite = !isFavorite;
                    viewModel.setFavorite(mangaPath, isFavorite);
                    updateFavoriteButton();
                    
                    int messageId = isFavorite ? R.string.added_to_favorites : R.string.removed_from_favorites;
                    Toast.makeText(this, messageId, Toast.LENGTH_SHORT).show();
                }
            });
            
            continueReadingButton.setOnClickListener(v -> {
                if (currentManga != null) {
                    // 查找最后阅读的章节
                    Log.d(TAG, "Reading button clicked for manga: " + currentManga.getTitle() + ", path: " + mangaPath);
                    
                    try {
                        viewModel.getLastReadChapter(mangaPath, chapter -> {
                            if (chapter != null && chapter.getLastReadTime() > 0) {
                                Log.d(TAG, "Found last read chapter: " + chapter.getTitle() 
                                        + ", path: " + chapter.getPath()
                                        + ", last read page: " + chapter.getLastReadPage()
                                        + ", last read time: " + chapter.getLastReadTime());
                                
                                // 检查章节路径是否存在
                                File chapterDir = new File(chapter.getPath());
                                if (chapterDir.exists() && chapterDir.isDirectory()) {
                                    startReaderActivity(chapter);
                                } else {
                                    Log.e(TAG, "Last read chapter directory does not exist: " + chapter.getPath());
                                    openFirstChapter();
                                }
                            } else {
                                Log.d(TAG, "No reading history found, opening first chapter");
                                openFirstChapter();
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error getting last read chapter: " + e.getMessage(), e);
                        openFirstChapter();
                    }
                }
            });
            
            // 添加标签
            tabLayout.addTab(tabLayout.newTab().setText(R.string.chapter));
            
            // 加载章节列表
            loadChapters();
            
        } catch (Exception e) {
            Log.e(TAG, "onCreate error: " + e.getMessage(), e);
            Toast.makeText(this, "详情页面初始化失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void loadMangaDetails() {
        try {
            viewModel.getAllManga().observe(this, mangaList -> {
                if (mangaList != null) {
                    for (Manga manga : mangaList) {
                        if (manga.getPath().equals(mangaPath)) {
                            currentManga = manga;
                            displayMangaDetails(manga);
                            
                            // 检查是否有阅读历史，更新按钮文本
                            checkReadingHistory();
                            
                            break;
                        }
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error loading manga details: " + e.getMessage(), e);
        }
    }
    
    private void displayMangaDetails(Manga manga) {
        // 设置标题
        getSupportActionBar().setTitle(manga.getTitle());
        
        // 加载封面图片
        if (manga.getCoverPath() != null) {
            Glide.with(this)
                    .load(new File(manga.getCoverPath()))
                    .placeholder(R.drawable.ic_manga_placeholder)
                    .error(R.drawable.ic_manga_placeholder)
                    .into(coverImageView);
        } else {
            coverImageView.setImageResource(R.drawable.ic_manga_placeholder);
        }
        
        // 设置描述（使用文件夹名称）
        descriptionTextView.setText(manga.getTitle());
        
        // 更新收藏按钮状态
        isFavorite = manga.isFavorite();
        updateFavoriteButton();
    }
    
    private void updateFavoriteButton() {
        favoriteButton.setText(isFavorite ? R.string.remove_from_favorites : R.string.add_to_favorites);
    }
    
    private void loadChapters() {
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        
        viewModel.getChapters(mangaPath).observe(this, chapters -> {
            progressBar.setVisibility(View.GONE);
            
            if (chapters != null && !chapters.isEmpty()) {
                chapterAdapter.setChapterList(chapters);
                emptyView.setVisibility(View.GONE);
                chapterRecyclerView.setVisibility(View.VISIBLE);
            } else {
                emptyView.setVisibility(View.VISIBLE);
                chapterRecyclerView.setVisibility(View.GONE);
            }
        });
    }
    
    private void startReaderActivity(Chapter chapter) {
        // 更新漫画的最后阅读时间
        viewModel.updateMangaLastReadTime(mangaPath);
        
        // 启动阅读器 - 使用新的 ReaderActivity
        Intent intent = new Intent(this, ReaderActivity.class);
        intent.putExtra("manga", currentManga);
        intent.putExtra("chapter", chapter);
        startActivity(intent);
    }

    @Override
    public void onChapterClick(Chapter chapter) {
        Log.d(TAG, "Chapter clicked: " + chapter.getTitle() 
                + ", path: " + chapter.getPath()
                + ", manga path: " + chapter.getMangaPath()
                + ", manga folder path: " + mangaPath);
                
        // 确保章节路径存在且包含图片
        if (isValidChapter(chapter)) {
            startReaderActivity(chapter);
        } else {
            Toast.makeText(this, "章节无效或不包含图片: " + chapter.getTitle(), Toast.LENGTH_LONG).show();
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // 辅助方法：打开第一章
    private void openFirstChapter() {
        viewModel.getChapters(mangaPath).observe(this, chapters -> {
            try {
                if (chapters != null && !chapters.isEmpty()) {
                    boolean foundValidChapter = false;
                    
                    for (Chapter chapter : chapters) {
                        // 确保章节路径存在且包含图片
                        if (isValidChapter(chapter)) {
                            Log.d(TAG, "Opening valid chapter: " + chapter.getTitle());
                            // 重置阅读页数为0，确保从第一页开始
                            chapter.setLastReadPage(0);
                            startReaderActivity(chapter);
                            foundValidChapter = true;
                            break;
                        } else {
                            Log.d(TAG, "Skipping invalid chapter: " + chapter.getTitle());
                        }
                    }
                    
                    if (!foundValidChapter) {
                        Log.e(TAG, "No valid chapters found for manga: " + mangaPath);
                        Toast.makeText(this, "没有找到有效章节", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "No chapters found for manga: " + mangaPath);
                    Toast.makeText(this, "没有找到章节", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error opening first chapter: " + e.getMessage(), e);
                Toast.makeText(this, "打开章节失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
    
    // 检查章节是否有效（目录存在且包含图片）
    private boolean isValidChapter(Chapter chapter) {
        File chapterDir = new File(chapter.getPath());
        if (!chapterDir.exists() || !chapterDir.isDirectory()) {
            Log.e(TAG, "Chapter directory does not exist: " + chapter.getPath());
            return false;
        }
        
        // 检查章节文件夹中是否包含图片文件
        File[] imageFiles = chapterDir.listFiles(file -> {
            if (file.isFile()) {
                String name = file.getName().toLowerCase();
                return name.endsWith(".jpg") || name.endsWith(".jpeg") || 
                       name.endsWith(".png") || name.endsWith(".webp") || 
                       name.endsWith(".gif");
            }
            return false;
        });
        
        boolean hasImages = (imageFiles != null && imageFiles.length > 0);
        Log.d(TAG, "Chapter " + chapter.getTitle() + " contains " + 
                (hasImages ? imageFiles.length + " images" : "no images"));
        return hasImages;
    }

    // 检查阅读历史并更新按钮文本
    private void checkReadingHistory() {
        viewModel.getLastReadChapter(mangaPath, chapter -> {
            boolean hasReadingHistory = (chapter != null && chapter.getLastReadTime() > 0);
            
            // 在UI线程更新按钮状态
            runOnUiThread(() -> {
                if (hasReadingHistory) {
                    Log.d(TAG, "Has reading history, enabling continue reading button");
                    continueReadingButton.setText(R.string.continue_reading);
                    continueReadingButton.setEnabled(true);
                } else {
                    Log.d(TAG, "No reading history, disabling continue reading button");
                    continueReadingButton.setEnabled(false);
                    continueReadingButton.setAlpha(0.5f); // 半透明显示禁用状态
                }
            });
        });
    }
} 