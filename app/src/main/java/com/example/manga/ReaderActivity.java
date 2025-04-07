package com.example.manga;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;

import com.example.manga.R;
import com.example.manga.SettingsActivity;
import com.example.manga.model.Chapter;
import com.example.manga.model.Manga;
import com.example.manga.viewmodel.MangaViewModel;
import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ReaderActivity extends AppCompatActivity {

    private static final String TAG = "ReaderActivity";
    private View controlsLayout;
    private PhotoView imageView;
    private SeekBar seekBar;
    private TextView tvPageInfo;
    private Button btnPrev;
    private Button btnNext;
    private ImageButton btnBack;
    private TextView tvChapterTitle;
    private ImageButton btnSettings;
    private int currentPage = 0;
    private int totalPages = 0;
    
    private Manga manga;
    private Chapter chapter;
    private List<String> imageFilePaths = new ArrayList<>();
    private MangaViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 应用主题
        applyTheme();
        
        setContentView(R.layout.activity_reader);
        
        // 初始化ViewModel
        viewModel = new ViewModelProvider(this).get(MangaViewModel.class);
        
        // 获取传递的参数
        manga = getIntent().getParcelableExtra("manga");
        chapter = getIntent().getParcelableExtra("chapter");
        
        if (manga == null || chapter == null) {
            Toast.makeText(this, "加载漫画失败：缺少必要参数", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // 设置UI和监听器
        setupControls();
        
        // 确保控制栏初始状态为隐藏
        hideControls();
        
        // 加载当前章节的图片
        loadChapterImages();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 在页面恢复时重新应用主题设置
        applyTheme();
        
        // 确保控制栏状态正确
        if (controlsLayout != null) {
            // 读取自动隐藏设置
            SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE);
            boolean autoHide = prefs.getBoolean(SettingsActivity.KEY_AUTO_HIDE, true);
            
            // 如果控制栏当前可见且启用了自动隐藏，设置定时器隐藏它
            if (controlsLayout.getVisibility() == View.VISIBLE && autoHide) {
                imageView.removeCallbacks(this::hideControls);
                imageView.postDelayed(this::hideControls, 3000);
            }
        }
    }

    private void applyTheme() {
        // 从SharedPreferences读取主题设置
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE);
        int theme = prefs.getInt(SettingsActivity.KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(theme);
    }

    private void setupControls() {
        controlsLayout = findViewById(R.id.controlsLayout);
        imageView = findViewById(R.id.imageView);
        seekBar = findViewById(R.id.seekBar);
        tvPageInfo = findViewById(R.id.tvPageInfo);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        btnBack = findViewById(R.id.btnBack);
        tvChapterTitle = findViewById(R.id.tvChapterTitle);
        btnSettings = findViewById(R.id.btnSettings);
        
        // 设置章节标题
        tvChapterTitle.setText(chapter.getTitle());

        // 设置点击事件
        imageView.setOnClickListener(v -> toggleControls());
        
        // 设置返回按钮
        btnBack.setOnClickListener(v -> finish());
        
        // 设置设置按钮
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });
        
        // 设置上一页按钮
        btnPrev.setOnClickListener(v -> {
            if (currentPage > 0) {
                loadPage(currentPage - 1);
            }
        });
        
        // 设置下一页按钮
        btnNext.setOnClickListener(v -> {
            if (currentPage < totalPages - 1) {
                loadPage(currentPage + 1);
            }
        });
        
        // 设置进度条监听
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && progress >= 0 && progress < totalPages) {
                    loadPage(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // 设置自动隐藏控制栏
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE);
        boolean autoHide = prefs.getBoolean(SettingsActivity.KEY_AUTO_HIDE, true);
        if (autoHide) {
            imageView.postDelayed(this::hideControls, 3000);
        }

        // 设置音量键翻页
        boolean useVolumeKeys = prefs.getBoolean(SettingsActivity.KEY_VOLUME_NAV, false);
        if (useVolumeKeys) {
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
        }
    }
    
    private void loadChapterImages() {
        try {
            // 获取章节文件夹路径
            File chapterDir = new File(chapter.getPath());
            if (!chapterDir.exists() || !chapterDir.isDirectory()) {
                Toast.makeText(this, "章节目录不存在", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            
            // 获取所有图片文件
            File[] files = chapterDir.listFiles((dir, name) -> 
                    name.toLowerCase().endsWith(".jpg") || 
                    name.toLowerCase().endsWith(".jpeg") || 
                    name.toLowerCase().endsWith(".png"));
            
            if (files == null || files.length == 0) {
                Toast.makeText(this, "章节内未找到图片", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            
            // 清空列表并添加所有图片
            imageFilePaths.clear();
            for (File file : files) {
                imageFilePaths.add(file.getAbsolutePath());
            }
            
            // 设置总页数
            totalPages = imageFilePaths.size();
            
            // 设置SeekBar最大值
            seekBar.setMax(totalPages - 1);
            
            // 读取并显示第一页
            loadPage(0);
            
            // 更新阅读进度
            viewModel.updateReadProgress(chapter.getPath(), 0);
            viewModel.updateMangaLastReadTime(manga.getPath());
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading chapter images: " + e.getMessage(), e);
            Toast.makeText(this, "加载图片时出错: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void toggleControls() {
        if (controlsLayout.getVisibility() == View.VISIBLE) {
            hideControls();
        } else {
            showControls();
        }
    }

    private void showControls() {
        controlsLayout.setVisibility(View.VISIBLE);
        // 如果启用了自动隐藏，3秒后自动隐藏
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE);
        boolean autoHide = prefs.getBoolean(SettingsActivity.KEY_AUTO_HIDE, true);
        if (autoHide) {
            imageView.removeCallbacks(this::hideControls);
            imageView.postDelayed(this::hideControls, 3000);
        }
    }

    private void hideControls() {
        controlsLayout.setVisibility(View.GONE);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE);
        boolean useVolumeKeys = prefs.getBoolean(SettingsActivity.KEY_VOLUME_NAV, false);
        
        if (useVolumeKeys) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                    if (currentPage > 0) {
                        loadPage(currentPage - 1);
                        return true;
                    }
                    break;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    if (currentPage < totalPages - 1) {
                        loadPage(currentPage + 1);
                        return true;
                    }
                    break;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
    
    // 加载指定页面
    private void loadPage(int pageIndex) {
        if (pageIndex < 0 || pageIndex >= totalPages) {
            return;
        }
        
        currentPage = pageIndex;
        
        try {
            // 加载图片并显示
            File imageFile = new File(imageFilePaths.get(pageIndex));
            if (imageFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                imageView.setImageBitmap(bitmap);
            } else {
                Toast.makeText(this, "无法加载图片：文件不存在", Toast.LENGTH_SHORT).show();
            }
            
            // 更新页面信息
            tvPageInfo.setText(getString(R.string.page_number, currentPage + 1, totalPages));
            
            // 更新 SeekBar 位置
            seekBar.setProgress(currentPage);
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading image: " + e.getMessage(), e);
            Toast.makeText(this, "加载图片时出错: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
} 