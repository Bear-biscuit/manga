package com.example.manga;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.manga.adapter.MangaGridAdapter;
import com.example.manga.model.Manga;
import com.example.manga.viewmodel.MangaViewModel;
import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, MangaGridAdapter.OnMangaClickListener {

    private static final String TAG = "MainActivity";
    private static final int STORAGE_PERMISSION_CODE = 100;
    private static final String DEFAULT_MANGA_FOLDER = Environment.getExternalStorageDirectory() + "/manga";
    
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private RecyclerView mangaRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private MangaGridAdapter mangaAdapter;
    private MangaViewModel viewModel;
    
    private final ActivityResultLauncher<Intent> folderPickerLauncher = 
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        try {
                            // 获取真实路径
                            String path = uri.getPath();
                            Log.d(TAG, "Original selected path URI: " + uri.toString());
                            Log.d(TAG, "Original selected path: " + path);
                            
                            // 处理DocumentFile路径
                            if (path.startsWith("/tree/")) {
                                path = path.replace("/tree/", "");
                            }
                            
                            // 处理一些特殊路径
                            if (path.contains(":")) {
                                String[] parts = path.split(":");
                                if (parts.length > 1) {
                                    if (parts[0].equals("primary")) {
                                        // 主存储
                                        path = Environment.getExternalStorageDirectory() + "/" + parts[1];
                                    } else {
                                        // SD卡或其他外部存储
                                        path = "/storage/" + parts[0] + "/" + parts[1];
                                    }
                                }
                            }
                            
                            Log.d(TAG, "Processed folder path: " + path);
                            
                            // 检查路径是否存在且可读
                            File directory = new File(path);
                            if (!directory.exists()) {
                                Toast.makeText(MainActivity.this, "目录不存在: " + path, Toast.LENGTH_LONG).show();
                                return;
                            }
                            
                            if (!directory.isDirectory()) {
                                Toast.makeText(MainActivity.this, "所选路径不是目录: " + path, Toast.LENGTH_LONG).show();
                                return;
                            }
                            
                            if (!directory.canRead()) {
                                Toast.makeText(MainActivity.this, "无法读取目录(权限被拒绝): " + path, Toast.LENGTH_LONG).show();
                                return;
                            }
                            
                            viewModel.scanMangaDirectory(path);
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing selected folder: " + e.getMessage(), e);
                            Toast.makeText(MainActivity.this, "处理所选文件夹时出错: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            // 应用所选主题
            applyTheme();
            
            setContentView(R.layout.activity_main);
            
            // 设置toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            
            // 初始化视图
            drawerLayout = findViewById(R.id.drawer_layout);
            navigationView = findViewById(R.id.nav_view);
            progressBar = findViewById(R.id.progress_bar);
            emptyView = findViewById(R.id.empty_view);
            mangaRecyclerView = findViewById(R.id.manga_recycler_view);
            swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
            
            // 设置下拉刷新
            setupSwipeRefresh();
            
            // 设置抽屉导航
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawerLayout, toolbar, R.string.app_name, R.string.app_name);
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();
            navigationView.setNavigationItemSelectedListener(this);
            
            // 设置RecyclerView
            mangaRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
            mangaAdapter = new MangaGridAdapter(this, this);
            mangaRecyclerView.setAdapter(mangaAdapter);
            
            // 初始化ViewModel
            viewModel = new ViewModelProvider(this).get(MangaViewModel.class);
            
            // 观察数据变化
            viewModel.getAllManga().observe(this, mangaList -> {
                if (mangaList != null && !mangaList.isEmpty()) {
                    mangaAdapter.setMangaList(mangaList);
                    showEmptyView(false);
                } else {
                    showEmptyView(true);
                }
            });
            
            // 观察加载状态
            viewModel.getIsLoading().observe(this, isLoading -> {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                // 如果数据加载完成，停止刷新动画
                if (!isLoading && swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                if (isLoading) {
                    showEmptyView(false);
                }
            });
            
            // 观察错误消息
            viewModel.getErrorMessage().observe(this, errorMessage -> {
                if (errorMessage != null && !errorMessage.isEmpty()) {
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                }
            });
            
            // 检查权限并扫描漫画目录
            checkPermissionAndScanManga();
            
        } catch (Exception e) {
            Log.e(TAG, "onCreate error: " + e.getMessage(), e);
            Toast.makeText(this, "初始化失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void applyTheme() {
        // 从SharedPreferences读取主题设置
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE);
        
        // 应用夜间模式
        int theme = prefs.getInt(SettingsActivity.KEY_THEME, androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(theme);
        
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
    
    @Override
    protected void onResume() {
        super.onResume();
        // 页面恢复时重新检查主题设置，以便在设置页面修改后能够应用
        applyTheme();
        
        // 如果数据正在加载，确保显示加载状态
        if (viewModel != null && viewModel.getIsLoading().getValue() != null && viewModel.getIsLoading().getValue()) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }
    
    private void showEmptyView(boolean show) {
        emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        mangaRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }
    
    private void checkPermissionAndScanManga() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11及以上使用新的存储权限API
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                Toast.makeText(this, R.string.permission_required, Toast.LENGTH_LONG).show();
                startActivity(intent);
            } else {
                scanMangaDirectory();
            }
        } else {
            // Android 10及以下使用旧的存储权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 
                        STORAGE_PERMISSION_CODE);
            } else {
                scanMangaDirectory();
            }
        }
    }
    
    private void scanMangaDirectory() {
        String currentFolder = viewModel.getCurrentMangaFolder().getValue();
        if (currentFolder == null || currentFolder.isEmpty()) {
            currentFolder = DEFAULT_MANGA_FOLDER;
        }
        
        Log.d(TAG, "Scanning manga directory: " + currentFolder);
        
        // 检查目录是否存在
        File directory = new File(currentFolder);
        if (!directory.exists()) {
            Log.e(TAG, "Manga directory does not exist: " + currentFolder);
            Toast.makeText(this, "漫画目录不存在，请选择有效目录", Toast.LENGTH_LONG).show();
            showEmptyView(true);
            if (swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
            
            // 提示用户需要选择有效目录
            new android.os.Handler().postDelayed(() -> {
                // 提示用户选择新目录
                Toast.makeText(this, "请选择包含漫画的目录", Toast.LENGTH_LONG).show();
                selectMangaFolder();
            }, 1000);
            return;
        }
        
        if (!directory.isDirectory()) {
            Log.e(TAG, "Path is not a directory: " + currentFolder);
            Toast.makeText(this, "所选路径不是目录，请选择有效目录", Toast.LENGTH_LONG).show();
            showEmptyView(true);
            if (swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
            return;
        }
        
        if (!directory.canRead()) {
            Log.e(TAG, "Cannot read directory (permission denied): " + currentFolder);
            Toast.makeText(this, "无法读取目录(权限被拒绝)，请确保应用有存储权限", Toast.LENGTH_LONG).show();
            showEmptyView(true);
            if (swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
            return;
        }
        
        // 检查目录是否为空
        File[] files = directory.listFiles();
        if (files == null || files.length == 0) {
            Log.e(TAG, "Directory is empty: " + currentFolder);
            Toast.makeText(this, "目录为空，请选择包含漫画的目录", Toast.LENGTH_LONG).show();
            showEmptyView(true);
            if (swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
            return;
        }
        
        // 确认目录没有问题后再扫描
        try {
            viewModel.scanMangaDirectory(currentFolder);
        } catch (Exception e) {
            Log.e(TAG, "Error scanning manga directory: " + e.getMessage(), e);
            Toast.makeText(this, "扫描目录时出错: " + e.getMessage(), Toast.LENGTH_LONG).show();
            showEmptyView(true);
            if (swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scanMangaDirectory();
            } else {
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show();
            }
        }
    }
    
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.nav_library) {
            viewModel.getAllManga().removeObservers(this);
            viewModel.getAllManga().observe(this, mangaList -> {
                if (mangaList != null) {
                    mangaAdapter.setMangaList(mangaList);
                    showEmptyView(mangaList.isEmpty());
                }
            });
            setTitle(R.string.all_manga);
        } else if (id == R.id.nav_history) {
            viewModel.getAllManga().removeObservers(this);
            viewModel.getRecentManga().observe(this, mangaList -> {
                if (mangaList != null) {
                    mangaAdapter.setMangaList(mangaList);
                    showEmptyView(mangaList.isEmpty());
                }
            });
            setTitle(R.string.history);
        } else if (id == R.id.nav_favorites) {
            viewModel.getAllManga().removeObservers(this);
            viewModel.getFavoriteManga().observe(this, mangaList -> {
                if (mangaList != null) {
                    mangaAdapter.setMangaList(mangaList);
                    showEmptyView(mangaList.isEmpty());
                }
            });
            setTitle(R.string.favorites);
        } else if (id == R.id.nav_settings) {
            try {
                Log.d(TAG, "正在启动设置页面");
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivityForResult(intent, 100);
                Log.d(TAG, "设置页面启动命令已发送");
            } catch (Exception e) {
                Log.e(TAG, "启动设置页面时出错: " + e.getMessage(), e);
                Toast.makeText(this, "打开设置页面失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else if (id == R.id.nav_select_folder) {
            selectMangaFolder();
        }
        
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
    
    private void selectMangaFolder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                // 需要先获取所有文件访问权限
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                Toast.makeText(this, R.string.permission_required, Toast.LENGTH_LONG).show();
                startActivity(intent);
                return;
            }
        }
        
        // 打开目录选择器
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        folderPickerLauncher.launch(intent);
    }
    
    @Override
    public void onMangaClick(Manga manga) {
        Intent intent = MangaDetailActivity.newIntent(this, manga);
        startActivity(intent);
    }
    
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    // 设置下拉刷新
    private void setupSwipeRefresh() {
        // 设置刷新颜色
        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
                
        // 设置刷新监听器
        swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d(TAG, "下拉刷新触发，重新扫描漫画目录");
            Toast.makeText(MainActivity.this, "正在刷新漫画目录...", Toast.LENGTH_SHORT).show();
            scanMangaDirectory();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            // 设置页面返回后，重新应用主题
            applyTheme();
            // 重新创建活动以应用新主题
            recreate();
        }
    }
} 