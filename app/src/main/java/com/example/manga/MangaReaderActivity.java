package com.example.manga;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.example.manga.adapter.MangaPageAdapter;
import com.example.manga.model.Chapter;
import com.example.manga.viewmodel.MangaViewModel;

import java.util.List;

public class MangaReaderActivity extends AppCompatActivity {

    private static final String TAG = "MangaReaderActivity";
    private static final String EXTRA_CHAPTER_PATH = "com.example.manga.CHAPTER_PATH";
    private static final String EXTRA_MANGA_PATH = "com.example.manga.MANGA_PATH";
    private static final String EXTRA_CHAPTER_TITLE = "com.example.manga.CHAPTER_TITLE";
    private static final String EXTRA_CHAPTER_NUMBER = "com.example.manga.CHAPTER_NUMBER";
    
    private MangaViewModel viewModel;
    private ViewPager2 viewPager;
    private MangaPageAdapter adapter;
    private SeekBar pageSeekBar;
    private TextView pageNumberText;
    private Button prevButton, nextButton;
    
    private Chapter currentChapter;
    private List<String> pageList;
    private int totalPages;
    private int currentPage;
    private String chapterPath;

    public static Intent newIntent(Context context, Chapter chapter) {
        Intent intent = new Intent(context, MangaReaderActivity.class);
        intent.putExtra(EXTRA_CHAPTER_PATH, chapter.getPath());
        intent.putExtra(EXTRA_MANGA_PATH, chapter.getMangaPath());
        intent.putExtra(EXTRA_CHAPTER_TITLE, chapter.getTitle());
        intent.putExtra(EXTRA_CHAPTER_NUMBER, chapter.getChapterNumber());
        Log.d("MangaReaderActivity", "Creating intent with chapter path: " + chapter.getPath()
                + ", manga path: " + chapter.getMangaPath());
        return intent;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_manga_reader);
            
            // 初始化视图
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            
            viewPager = findViewById(R.id.view_pager);
            pageSeekBar = findViewById(R.id.page_seekbar);
            pageNumberText = findViewById(R.id.page_number_text);
            prevButton = findViewById(R.id.btn_prev_page);
            nextButton = findViewById(R.id.btn_next_page);
            
            // 获取章节路径和其他信息
            Intent intent = getIntent();
            chapterPath = intent.getStringExtra(EXTRA_CHAPTER_PATH);
            String mangaPath = intent.getStringExtra(EXTRA_MANGA_PATH);
            String chapterTitle = intent.getStringExtra(EXTRA_CHAPTER_TITLE);
            int chapterNumber = intent.getIntExtra(EXTRA_CHAPTER_NUMBER, 0);
            
            Log.d(TAG, "Received intent with chapter path: " + chapterPath
                    + ", manga path: " + mangaPath);
            
            if (chapterPath == null) {
                Toast.makeText(this, R.string.error_loading_manga, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            
            // 设置标题
            if (chapterTitle != null) {
                getSupportActionBar().setTitle(chapterTitle);
            }
            
            // 初始化ViewModel
            viewModel = new ViewModelProvider(this).get(MangaViewModel.class);
            
            // 初始化适配器
            adapter = new MangaPageAdapter(this);
            viewPager.setAdapter(adapter);
            
            // 设置翻页监听
            viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    updatePageDisplay(position);
                    // 保存阅读进度
                    if (currentChapter != null) {
                        Log.d(TAG, "Updating read progress for chapter: " + currentChapter.getTitle() 
                                + ", position: " + position);
                        viewModel.updateReadProgress(chapterPath, position);
                    } else {
                        Log.e(TAG, "Cannot update read progress: currentChapter is null, but chapterPath is: " + chapterPath);
                        // 尝试使用chapterPath来更新进度
                        viewModel.updateReadProgress(chapterPath, position);
                    }
                }
            });
            
            // 设置SeekBar监听
            pageSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && pageList != null && !pageList.isEmpty()) {
                        viewPager.setCurrentItem(progress, false);
                    }
                }
                
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
            
            // 设置按钮点击事件
            prevButton.setOnClickListener(v -> {
                if (currentPage > 0) {
                    viewPager.setCurrentItem(currentPage - 1, true);
                }
            });
            
            nextButton.setOnClickListener(v -> {
                if (currentPage < totalPages - 1) {
                    viewPager.setCurrentItem(currentPage + 1, true);
                }
            });
            
            // 加载章节数据
            loadChapter();
            
        } catch (Exception e) {
            Log.e(TAG, "onCreate error: " + e.getMessage(), e);
            Toast.makeText(this, "阅读器初始化失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void loadChapter() {
        // 获取章节页面
        viewModel.getChapterPages(chapterPath, pageList -> {
            if (pageList != null && !pageList.isEmpty()) {
                this.pageList = pageList;
                totalPages = pageList.size();
                
                // 更新适配器
                adapter.setPageList(pageList);
                
                // 设置SeekBar
                pageSeekBar.setMax(totalPages - 1);
                
                // 从Intent中获取额外信息来创建Chapter对象，以防数据库查询失败
                String mangaPath = getIntent().getStringExtra(EXTRA_MANGA_PATH);
                String chapterTitle = getIntent().getStringExtra(EXTRA_CHAPTER_TITLE);
                int chapterNumber = getIntent().getIntExtra(EXTRA_CHAPTER_NUMBER, 0);
                
                // 如果有足够信息，手动创建一个Chapter对象以防数据库查询失败
                if (chapterPath != null && mangaPath != null && chapterTitle != null) {
                    currentChapter = new Chapter(chapterPath, mangaPath, chapterTitle, chapterNumber);
                    currentChapter.setTotalPages(totalPages);
                    Log.d(TAG, "Created temporary Chapter object from intent extras");
                }
                
                // 从数据库获取章节信息，包括上次阅读位置
                queryChapterFromDatabase();
            } else {
                Toast.makeText(this, R.string.error_loading_manga, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }
    
    private void queryChapterFromDatabase() {
        try {
            // 使用chapterDao直接获取章节，避免使用LiveData导致的延迟
            viewModel.getChapterByPath(chapterPath, chapter -> {
                if (chapter != null) {
                    currentChapter = chapter;
                    String dbMangaPath = chapter.getMangaPath();
                    Log.d(TAG, "Found chapter in database via direct query: " + chapter.getTitle() 
                            + " with manga path: " + dbMangaPath
                            + ", lastReadPage: " + chapter.getLastReadPage()
                            + ", lastReadTime: " + chapter.getLastReadTime());
                    
                    getSupportActionBar().setTitle(chapter.getTitle());
                    
                    // 如果有上次阅读记录，跳转到对应页面
                    if (chapter.getLastReadTime() > 0 && chapter.getLastReadPage() >= 0 && chapter.getLastReadPage() < totalPages) {
                        Log.d(TAG, "Jumping to last read page: " + chapter.getLastReadPage());
                        viewPager.setCurrentItem(chapter.getLastReadPage(), false);
                    } else {
                        Log.d(TAG, "No previous reading record or invalid page, starting from page 0");
                        updatePageDisplay(0);
                    }
                } else {
                    // 如果无法通过直接查询找到章节，尝试使用LiveData查询
                    Log.d(TAG, "Chapter not found via direct query, trying LiveData query");
                    viewModel.getChapters(null).observe(this, chapters -> {
                        if (chapters != null) {
                            for (Chapter ch : chapters) {
                                if (ch.getPath().equals(chapterPath)) {
                                    currentChapter = ch;
                                    Log.d(TAG, "Found chapter in database via LiveData: " + ch.getTitle() 
                                            + ", lastReadPage: " + ch.getLastReadPage());
                                    
                                    getSupportActionBar().setTitle(ch.getTitle());
                                    
                                    // 如果有上次阅读记录，跳转到对应页面
                                    if (ch.getLastReadTime() > 0 && ch.getLastReadPage() >= 0 && ch.getLastReadPage() < totalPages) {
                                        Log.d(TAG, "Jumping to last read page: " + ch.getLastReadPage());
                                        viewPager.setCurrentItem(ch.getLastReadPage(), false);
                                    } else {
                                        Log.d(TAG, "No previous reading record or invalid page, starting from page 0");
                                        updatePageDisplay(0);
                                    }
                                    return;
                                }
                            }
                            loadChapterFallback();
                        } else {
                            loadChapterFallback();
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error querying chapter from database: " + e.getMessage(), e);
            loadChapterFallback();
        }
    }
    
    private void loadChapterFallback() {
        // 尝试从路径中获取章节标题
        String chapterTitle = "";
        try {
            String[] pathParts = chapterPath.split("/");
            if (pathParts.length > 0) {
                chapterTitle = pathParts[pathParts.length - 1];
            }
        } catch (Exception e) {
            chapterTitle = "未知章节";
            Log.e(TAG, "Error parsing chapter title: " + e.getMessage(), e);
        }
        
        getSupportActionBar().setTitle(chapterTitle);
        updatePageDisplay(0);
    }
    
    private void updatePageDisplay(int position) {
        currentPage = position;
        
        // 更新页码显示
        pageNumberText.setText(getString(R.string.page_number, position + 1, totalPages));
        
        // 更新SeekBar
        pageSeekBar.setProgress(position);
        
        // 更新按钮状态
        prevButton.setEnabled(position > 0);
        nextButton.setEnabled(position < totalPages - 1);
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 