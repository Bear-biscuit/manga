package com.example.manga;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.GestureDetectorCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.manga.R;
import com.example.manga.SettingsActivity;
import com.example.manga.model.Chapter;
import com.example.manga.model.Manga;
import com.example.manga.viewmodel.MangaViewModel;
import com.github.chrisbanes.photoview.PhotoView;
import com.example.manga.util.ToastUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReaderActivity extends AppCompatActivity {

    private static final String TAG = "ReaderActivity";
    private View topControlsLayout;
    private View bottomControlsLayout;
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
    
    // 翻页相关变量
    private float startX;
    private float startY;
    private static final float SWIPE_THRESHOLD = 100;
    private boolean processingTouch = false;
    private boolean isAnimating = false;

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
            ToastUtil.showShort(this, "加载漫画失败：缺少必要参数");
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
        if (topControlsLayout != null && bottomControlsLayout != null) {
            // 读取自动隐藏设置
            SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE);
            boolean autoHide = prefs.getBoolean(SettingsActivity.KEY_AUTO_HIDE, true);
            
            // 如果控制栏当前可见且启用了自动隐藏，设置定时器隐藏它
            if ((topControlsLayout.getVisibility() == View.VISIBLE || 
                 bottomControlsLayout.getVisibility() == View.VISIBLE) && autoHide) {
                imageView.removeCallbacks(this::hideControls);
                imageView.postDelayed(this::hideControls, 3000);
            }
        }
    }

    private void applyTheme() {
        // 从SharedPreferences读取主题设置
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE);
        
        // 应用夜间模式
        int theme = prefs.getInt(SettingsActivity.KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(theme);
        
        // 应用颜色主题
        int colorTheme = prefs.getInt(SettingsActivity.KEY_COLOR_THEME, SettingsActivity.COLOR_THEME_BLUE);
        
        switch (colorTheme) {
            case SettingsActivity.COLOR_THEME_RED:
                setTheme(R.style.Theme_Manga_Red);
                break;
            case SettingsActivity.COLOR_THEME_GREEN:
                setTheme(R.style.Theme_Manga_Green);
                break;
            case SettingsActivity.COLOR_THEME_DARK:
                setTheme(R.style.Theme_Manga_Dark);
                break;
            case SettingsActivity.COLOR_THEME_BLUE:
            default:
                setTheme(R.style.Theme_Manga_Blue);
                break;
        }
    }

    private void setupControls() {
        topControlsLayout = findViewById(R.id.topControlsLayout);
        bottomControlsLayout = findViewById(R.id.bottomControlsLayout);
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

        // 设置基本触摸和点击事件
        setupTouchEvents();
        
        // 设置返回按钮
        btnBack.setOnClickListener(v -> finish());
        
        // 设置设置按钮
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            // 记录当前状态，以便在返回时恢复
            intent.putExtra("manga", manga);
            intent.putExtra("chapter", chapter);
            intent.putExtra("currentPage", currentPage);
            // 使用 startActivityForResult 而不是 startActivity
            startActivityForResult(intent, 1);
        });
        
        // 修改上一页按钮为上一章按钮
        btnPrev.setText("上一章");
        btnPrev.setOnClickListener(v -> loadPreviousChapter());
        
        // 修改下一页按钮为下一章按钮
        btnNext.setText("下一章");
        btnNext.setOnClickListener(v -> loadNextChapter());
        
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
    
    /**
     * 设置触摸和点击事件
     */
    private void setupTouchEvents() {
        // 分离触摸事件和点击事件
        imageView.setOnTouchListener((v, event) -> {
            // 如果正在进行动画，忽略所有触摸事件
            if (isAnimating) {
                return true;
            }
            
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int leftRegion = screenWidth / 4;              // 左侧25%区域
            int rightRegion = screenWidth * 3 / 4;         // 右侧25%区域开始位置
            
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX = event.getX();
                    startY = event.getY();
                    processingTouch = false;
                    break;
                    
                case MotionEvent.ACTION_MOVE:
                    float moveX = event.getX();
                    float moveY = event.getY();
                    float deltaX = moveX - startX;
                    float deltaY = moveY - startY;
                    
                    // 如果水平移动距离大于垂直移动距离且超过阈值，处理为滑动翻页
                    if (!processingTouch && Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > SWIPE_THRESHOLD) {
                        processingTouch = true;
                        if (deltaX > 0) {
                            // 向右滑动，上一页
                            if (currentPage > 0) {
                                loadPage(currentPage - 1);
                                return true;
                            }
                        } else {
                            // 向左滑动，下一页
                            if (currentPage < totalPages - 1) {
                                loadPage(currentPage + 1);
                                return true;
                            }
                        }
                    }
                    break;
                    
                case MotionEvent.ACTION_UP:
                    // 如果未被识别为滑动且移动距离小，则视为点击
                    if (!processingTouch && Math.abs(event.getX() - startX) < 50 && Math.abs(event.getY() - startY) < 50) {
                        // 获取点击的X坐标，确定是左侧、中间还是右侧
                        float x = event.getX();
                        
                        if (x < leftRegion) {
                            // 点击左侧区域，翻到上一页
                            Log.d(TAG, "Clicked on left region");
                            if (currentPage > 0) {
                                loadPage(currentPage - 1);
                            } else {
                                // 已经是第一页，尝试加载上一章
                                loadPreviousChapter();
                            }
                            return true;
                        } else if (x > rightRegion) {
                            // 点击右侧区域，翻到下一页
                            Log.d(TAG, "Clicked on right region");
                            if (currentPage < totalPages - 1) {
                                loadPage(currentPage + 1);
                            } else {
                                // 已经是最后一页，尝试加载下一章
                                loadNextChapter();
                            }
                            return true;
                        } else {
                            // 点击中间区域，显示/隐藏控制栏
                            Log.d(TAG, "Clicked on center region");
                            toggleControls();
                            return true;
                        }
                    }
                    break;
            }
            
            // 传递给 PhotoView 处理缩放等
            return imageView.onTouchEvent(event);
        });
    }
    
    private void loadChapterImages() {
        try {
            // 获取章节文件夹路径
            File chapterDir = new File(chapter.getPath());
            if (!chapterDir.exists() || !chapterDir.isDirectory()) {
                ToastUtil.showShort(this, "章节目录不存在");
                finish();
                return;
            }
            
            // 获取所有图片文件
            File[] files = chapterDir.listFiles((dir, name) -> 
                    name.toLowerCase().endsWith(".jpg") || 
                    name.toLowerCase().endsWith(".jpeg") || 
                    name.toLowerCase().endsWith(".png"));
            
            if (files == null || files.length == 0) {
                ToastUtil.showShort(this, "章节内未找到图片");
                finish();
                return;
            }
            
            // 排序文件
            Arrays.sort(files, (f1, f2) -> f1.getName().compareTo(f2.getName()));
            
            // 清空列表并添加所有图片
            imageFilePaths.clear();
            for (File file : files) {
                imageFilePaths.add(file.getAbsolutePath());
            }
            
            // 设置总页数
            totalPages = imageFilePaths.size();
            
            // 更新章节对象中的总页数
            chapter.setTotalPages(totalPages);
            
            // 设置SeekBar最大值
            seekBar.setMax(totalPages - 1);
            
            // 获取上次阅读的页码
            int lastReadPage = chapter.getLastReadPage();
            Log.d(TAG, "Chapter info - title: " + chapter.getTitle() 
                    + ", lastReadPage: " + lastReadPage 
                    + ", totalPages: " + totalPages);
            
            // 检查页码是否有效
            if (lastReadPage >= 0 && lastReadPage < totalPages) {
                // 加载上次阅读的页码
                Log.d(TAG, "Loading last read page: " + lastReadPage);
                loadPage(lastReadPage);
            } else {
                // 如果页码无效，从第一页开始
                Log.d(TAG, "Invalid lastReadPage, starting from page 0");
                loadPage(0);
            }
            
            // 更新阅读进度
            viewModel.updateReadProgress(chapter.getPath(), currentPage);
            viewModel.updateMangaLastReadTime(manga.getPath());
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading chapter images: " + e.getMessage(), e);
            ToastUtil.showShort(this, "加载图片时出错: " + e.getMessage());
            finish();
        }
    }

    private void toggleControls() {
        Log.d(TAG, "toggleControls called");
        if (topControlsLayout.getVisibility() == View.VISIBLE ||
            bottomControlsLayout.getVisibility() == View.VISIBLE) {
            hideControls();
        } else {
            showControls();
        }
    }

    private void showControls() {
        Log.d(TAG, "showControls called");
        // 简化控制栏显示逻辑
        topControlsLayout.setVisibility(View.VISIBLE);
        bottomControlsLayout.setVisibility(View.VISIBLE);
        
        // 如果启用了自动隐藏，3秒后自动隐藏
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE);
        boolean autoHide = prefs.getBoolean(SettingsActivity.KEY_AUTO_HIDE, true);
        if (autoHide) {
            imageView.removeCallbacks(this::hideControls);
            imageView.postDelayed(this::hideControls, 3000);
        }
    }

    private void hideControls() {
        Log.d(TAG, "hideControls called");
        // 简化控制栏隐藏逻辑
        topControlsLayout.setVisibility(View.GONE);
        bottomControlsLayout.setVisibility(View.GONE);
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
        if (pageIndex < 0 || pageIndex >= totalPages || isAnimating) {
            return;
        }
        
        // 如果是滑动翻页，添加动画效果
        if (pageIndex != currentPage) {
            animatePageChange(pageIndex);
        } else {
            updatePage(pageIndex);
        }
    }
    
    // 更新页面，不带动画
    private void updatePage(int pageIndex) {
        currentPage = pageIndex;
        
        try {
            // 加载图片并显示
            File imageFile = new File(imageFilePaths.get(pageIndex));
            if (imageFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                imageView.setImageBitmap(bitmap);
            } else {
                ToastUtil.showShort(this, "无法加载图片：文件不存在");
            }
            
            // 更新页面信息
            tvPageInfo.setText(getString(R.string.page_number, currentPage + 1, totalPages));
            
            // 更新 SeekBar 位置
            seekBar.setProgress(currentPage);
            
            // 更新阅读进度
            viewModel.updateReadProgress(chapter.getPath(), pageIndex);
            
            // 预加载相邻页面
            preloadAdjacentPages(pageIndex);
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading image: " + e.getMessage(), e);
            ToastUtil.showShort(this, "加载图片时出错: " + e.getMessage());
        }
    }
    
    /**
     * 预加载相邻页面的图片
     */
    private void preloadAdjacentPages(int currentPageIndex) {
        // 在后台线程中执行预加载，避免阻塞UI
        new Thread(() -> {
            try {
                // 预加载下一页
                if (currentPageIndex + 1 < totalPages) {
                    String nextPagePath = imageFilePaths.get(currentPageIndex + 1);
                    BitmapFactory.decodeFile(nextPagePath);
                    Log.d(TAG, "预加载下一页: " + nextPagePath);
                }
                
                // 预加载上一页
                if (currentPageIndex - 1 >= 0) {
                    String prevPagePath = imageFilePaths.get(currentPageIndex - 1);
                    BitmapFactory.decodeFile(prevPagePath);
                    Log.d(TAG, "预加载上一页: " + prevPagePath);
                }
            } catch (Exception e) {
                Log.e(TAG, "预加载图片出错: " + e.getMessage(), e);
            }
        }).start();
    }
    
    // 添加页面切换动画
    private void animatePageChange(int newPageIndex) {
        if (isAnimating) {
            return;
        }
        
        isAnimating = true;
        
        // 确定动画方向：左滑还是右滑
        final boolean isNextPage = newPageIndex > currentPage;
        
        // 准备新的图片
        try {
            File imageFile = new File(imageFilePaths.get(newPageIndex));
            if (!imageFile.exists()) {
                ToastUtil.showShort(this, "无法加载图片：文件不存在");
                isAnimating = false;
                return;
            }
            
            final Bitmap newBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            final PhotoView nextImageView = new PhotoView(this);
            nextImageView.setImageBitmap(newBitmap);
            
            // 将nextImageView添加到布局中并设置在当前图片之上
            View container = findViewById(R.id.readerContainer);
            if (container instanceof androidx.constraintlayout.widget.ConstraintLayout) {
                androidx.constraintlayout.widget.ConstraintLayout layout = 
                        (androidx.constraintlayout.widget.ConstraintLayout) container;
                
                // 设置nextImageView布局参数
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params = 
                        new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT,
                                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT);
                
                params.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
                params.bottomToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
                params.leftToLeft = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
                params.rightToRight = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
                
                // 设置初始位置（屏幕外）
                nextImageView.setTranslationX(isNextPage ? container.getWidth() : -container.getWidth());
                
                // 添加到布局
                layout.addView(nextImageView, params);
                
                // 当前图片滑出动画
                imageView.animate()
                        .translationX(isNextPage ? -container.getWidth() : container.getWidth())
                        .setDuration(300)
                        .setInterpolator(new AccelerateInterpolator())
                        .start();
                
                // 新图片滑入动画
                nextImageView.animate()
                        .translationX(0)
                        .setDuration(300)
                        .setInterpolator(new DecelerateInterpolator())
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                // 动画结束后，更新当前页信息
                                currentPage = newPageIndex;
                                
                                // 从布局中移除临时的nextImageView
                                layout.removeView(nextImageView);
                                
                                // 更新原始imageView的内容和位置
                                imageView.setTranslationX(0);
                                imageView.setImageBitmap(newBitmap);
                                
                                // 更新页面信息
                                tvPageInfo.setText(getString(R.string.page_number, currentPage + 1, totalPages));
                                
                                // 更新 SeekBar 位置
                                seekBar.setProgress(currentPage);
                                
                                // 更新阅读进度
                                viewModel.updateReadProgress(chapter.getPath(), newPageIndex);
                                
                                // 预加载相邻页面
                                preloadAdjacentPages(newPageIndex);
                                
                                // 动画完成
                                isAnimating = false;
                            }
                        })
                        .start();
            } else {
                // 如果容器不是ConstraintLayout，直接更新页面不使用动画
                isAnimating = false;
                updatePage(newPageIndex);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error during page animation: " + e.getMessage(), e);
            ToastUtil.showShort(this, "加载图片时出错: " + e.getMessage());
            isAnimating = false;
            updatePage(newPageIndex);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            // 设置页面返回后，重新应用主题和布局
            applyTheme();
            recreate();
        }
    }

    // 加载上一章
    private void loadPreviousChapter() {
        if (manga != null && chapter != null) {
            viewModel.getPreviousChapter(manga.getPath(), chapter.getChapterNumber(), previousChapter -> {
                if (previousChapter != null) {
                    // 保存当前章节的阅读进度
                    viewModel.updateReadProgress(chapter.getPath(), currentPage);
                    
                    // 切换到上一章
                    runOnUiThread(() -> {
                        ToastUtil.showShort(ReaderActivity.this, 
                                "正在加载上一章: " + previousChapter.getTitle());
                        
                        // 更新当前章节并重新加载
                        chapter = previousChapter;
                        tvChapterTitle.setText(chapter.getTitle());
                        loadChapterImages();
                    });
                } else {
                    runOnUiThread(() -> 
                            ToastUtil.showShort(ReaderActivity.this, "已经是第一章"));
                }
            });
        }
    }
    
    // 加载下一章
    private void loadNextChapter() {
        if (manga != null && chapter != null) {
            viewModel.getNextChapter(manga.getPath(), chapter.getChapterNumber(), nextChapter -> {
                if (nextChapter != null) {
                    // 保存当前章节的阅读进度
                    viewModel.updateReadProgress(chapter.getPath(), currentPage);
                    
                    // 切换到下一章
                    runOnUiThread(() -> {
                        ToastUtil.showShort(ReaderActivity.this, 
                                "正在加载下一章: " + nextChapter.getTitle());
                        
                        // 更新当前章节并重新加载
                        chapter = nextChapter;
                        tvChapterTitle.setText(chapter.getTitle());
                        loadChapterImages();
                    });
                } else {
                    runOnUiThread(() -> 
                            ToastUtil.showShort(ReaderActivity.this, "已经是最后一章"));
                }
            });
        }
    }
} 