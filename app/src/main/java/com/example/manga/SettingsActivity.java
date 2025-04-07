package com.example.manga;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";
    
    public static final String PREFS_NAME = "MangaSettings";
    public static final String KEY_THEME = "theme";
    public static final String KEY_COLOR_THEME = "color_theme";
    public static final String KEY_AUTO_HIDE = "auto_hide";
    public static final String KEY_VOLUME_NAV = "volume_nav";
    
    // 颜色主题常量
    public static final int COLOR_THEME_BLUE = 0;
    public static final int COLOR_THEME_RED = 1;
    public static final int COLOR_THEME_GREEN = 2;
    public static final int COLOR_THEME_DARK = 3;

    private SharedPreferences preferences;
    private RadioGroup themeRadioGroup;
    private RadioGroup colorRadioGroup;
    private CheckBox autoHideCheckBox;
    private CheckBox volumeNavCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            Log.d(TAG, "onCreate 开始");
            super.onCreate(savedInstanceState);
            
            // 应用当前主题设置
            applyCurrentTheme();
            
            // 设置布局
            setContentView(R.layout.activity_settings);
            
            preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            
            // 初始化视图
            initViews();
            
            // 设置返回按钮
            setupActionBar();
            
            // 加载保存的设置
            loadSettings();
            
            // 设置监听器
            setupListeners();
            
            Log.d(TAG, "onCreate 完成");
            
        } catch (Exception e) {
            Log.e(TAG, "onCreate 过程中出错: " + e.getMessage(), e);
            Toast.makeText(this, "初始化设置页面失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void initViews() {
        try {
            themeRadioGroup = findViewById(R.id.theme_radio_group);
            colorRadioGroup = findViewById(R.id.color_radio_group);
            autoHideCheckBox = findViewById(R.id.auto_hide_controls);
            volumeNavCheckBox = findViewById(R.id.volume_key_navigation);
            
            if (themeRadioGroup == null) Log.e(TAG, "主题选择组未找到");
            if (colorRadioGroup == null) Log.e(TAG, "颜色选择组未找到");
            if (autoHideCheckBox == null) Log.e(TAG, "自动隐藏控件未找到");
            if (volumeNavCheckBox == null) Log.e(TAG, "音量键导航控件未找到");
        } catch (Exception e) {
            Log.e(TAG, "初始化视图出错: " + e.getMessage(), e);
            Toast.makeText(this, "初始化视图失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void setupActionBar() {
        try {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle(R.string.settings);
            }
        } catch (Exception e) {
            Log.e(TAG, "设置动作栏出错: " + e.getMessage(), e);
        }
    }
    
    private void loadSettings() {
        try {
            // 加载主题设置
            if (themeRadioGroup != null) {
                int theme = preferences.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                int radioId = R.id.theme_default;
                if (theme == AppCompatDelegate.MODE_NIGHT_YES) {
                    radioId = R.id.theme_dark;
                } else if (theme == AppCompatDelegate.MODE_NIGHT_NO) {
                    radioId = R.id.theme_light;
                }
                themeRadioGroup.check(radioId);
            }
            
            // 加载颜色主题
            if (colorRadioGroup != null) {
                int colorTheme = preferences.getInt(KEY_COLOR_THEME, COLOR_THEME_BLUE);
                int colorRadioId = R.id.color_blue;
                if (colorTheme == COLOR_THEME_RED) {
                    colorRadioId = R.id.color_red;
                } else if (colorTheme == COLOR_THEME_GREEN) {
                    colorRadioId = R.id.color_green;
                } else if (colorTheme == COLOR_THEME_DARK) {
                    colorRadioId = R.id.color_dark;
                }
                colorRadioGroup.check(colorRadioId);
            }
            
            // 加载自动隐藏设置
            if (autoHideCheckBox != null) {
                boolean autoHide = preferences.getBoolean(KEY_AUTO_HIDE, true);
                autoHideCheckBox.setChecked(autoHide);
            }
            
            // 加载音量键翻页设置
            if (volumeNavCheckBox != null) {
                boolean volumeNav = preferences.getBoolean(KEY_VOLUME_NAV, false);
                volumeNavCheckBox.setChecked(volumeNav);
            }
        } catch (Exception e) {
            Log.e(TAG, "加载设置出错: " + e.getMessage(), e);
        }
    }
    
    private void setupListeners() {
        try {
            // 设置主题模式监听器
            if (themeRadioGroup != null) {
                themeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
                    try {
                        int theme = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                        if (checkedId == R.id.theme_dark) {
                            theme = AppCompatDelegate.MODE_NIGHT_YES;
                        } else if (checkedId == R.id.theme_light) {
                            theme = AppCompatDelegate.MODE_NIGHT_NO;
                        }
                        
                        // 保存设置
                        preferences.edit().putInt(KEY_THEME, theme).apply();
                        
                        // 应用设置
                        AppCompatDelegate.setDefaultNightMode(theme);
                        
                        Toast.makeText(this, "主题模式已更改", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e(TAG, "应用主题模式出错: " + e.getMessage(), e);
                    }
                });
            }
            
            // 设置颜色主题监听器
            if (colorRadioGroup != null) {
                colorRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
                    try {
                        int colorTheme = COLOR_THEME_BLUE;
                        if (checkedId == R.id.color_red) {
                            colorTheme = COLOR_THEME_RED;
                        } else if (checkedId == R.id.color_green) {
                            colorTheme = COLOR_THEME_GREEN;
                        } else if (checkedId == R.id.color_dark) {
                            colorTheme = COLOR_THEME_DARK;
                        }
                        
                        // 保存设置
                        preferences.edit().putInt(KEY_COLOR_THEME, colorTheme).apply();
                        
                        // 应用设置
                        applyColorTheme(this, colorTheme);
                        
                        // 重新创建活动以应用新主题
                        recreate();
                        
                        Toast.makeText(this, "颜色主题已更改", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e(TAG, "应用颜色主题出错: " + e.getMessage(), e);
                    }
                });
            }
            
            // 设置自动隐藏控制栏监听器
            if (autoHideCheckBox != null) {
                autoHideCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    try {
                        preferences.edit().putBoolean(KEY_AUTO_HIDE, isChecked).apply();
                        Toast.makeText(this, isChecked ? "已开启自动隐藏控制栏" : "已关闭自动隐藏控制栏", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e(TAG, "保存自动隐藏设置出错: " + e.getMessage(), e);
                    }
                });
            }
            
            // 设置音量键翻页监听器
            if (volumeNavCheckBox != null) {
                volumeNavCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    try {
                        preferences.edit().putBoolean(KEY_VOLUME_NAV, isChecked).apply();
                        Toast.makeText(this, isChecked ? "已开启音量键翻页" : "已关闭音量键翻页", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e(TAG, "保存音量键翻页设置出错: " + e.getMessage(), e);
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "设置监听器出错: " + e.getMessage(), e);
        }
    }
    
    private void applyCurrentTheme() {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            
            // 应用夜间模式
            int theme = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            AppCompatDelegate.setDefaultNightMode(theme);
            
            // 应用颜色主题
            int colorTheme = prefs.getInt(KEY_COLOR_THEME, COLOR_THEME_BLUE);
            applyColorTheme(this, colorTheme);
        } catch (Exception e) {
            Log.e(TAG, "应用当前主题出错: " + e.getMessage(), e);
        }
    }
    
    public static void applyColorTheme(AppCompatActivity activity, int colorTheme) {
        switch (colorTheme) {
            case COLOR_THEME_RED:
                activity.setTheme(R.style.Theme_Manga_Red);
                break;
            case COLOR_THEME_GREEN:
                activity.setTheme(R.style.Theme_Manga_Green);
                break;
            case COLOR_THEME_DARK:
                activity.setTheme(R.style.Theme_Manga_Dark);
                break;
            case COLOR_THEME_BLUE:
            default:
                activity.setTheme(R.style.Theme_Manga_Blue);
                break;
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
    
    @Override
    public void onBackPressed() {
        // 设置结果并返回
        setResult(RESULT_OK);
        super.onBackPressed();
    }
} 