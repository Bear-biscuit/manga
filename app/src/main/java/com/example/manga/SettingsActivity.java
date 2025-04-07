package com.example.manga;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class SettingsActivity extends AppCompatActivity {
    public static final String PREFS_NAME = "MangaSettings";
    public static final String KEY_THEME = "theme";
    public static final String KEY_AUTO_HIDE = "auto_hide";
    public static final String KEY_VOLUME_NAV = "volume_nav";

    private SharedPreferences preferences;
    private RadioGroup themeRadioGroup;
    private CheckBox autoHideCheckBox;
    private CheckBox volumeNavCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // 初始化视图
        themeRadioGroup = findViewById(R.id.theme_radio_group);
        autoHideCheckBox = findViewById(R.id.auto_hide_controls);
        volumeNavCheckBox = findViewById(R.id.volume_key_navigation);
        
        // 加载保存的设置
        loadSettings();
        
        // 设置主题切换监听器
        themeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int theme = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            if (checkedId == R.id.theme_dark) {
                theme = AppCompatDelegate.MODE_NIGHT_YES;
            } else if (checkedId == R.id.theme_light) {
                theme = AppCompatDelegate.MODE_NIGHT_NO;
            }
            AppCompatDelegate.setDefaultNightMode(theme);
            preferences.edit().putInt(KEY_THEME, theme).apply();
        });
        
        // 设置自动隐藏控制栏监听器
        autoHideCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean(KEY_AUTO_HIDE, isChecked).apply();
        });
        
        // 设置音量键翻页监听器
        volumeNavCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean(KEY_VOLUME_NAV, isChecked).apply();
        });
    }
    
    private void loadSettings() {
        // 加载主题设置
        int theme = preferences.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        int radioId = R.id.theme_default;
        if (theme == AppCompatDelegate.MODE_NIGHT_YES) {
            radioId = R.id.theme_dark;
        } else if (theme == AppCompatDelegate.MODE_NIGHT_NO) {
            radioId = R.id.theme_light;
        }
        themeRadioGroup.check(radioId);
        
        // 加载自动隐藏设置
        boolean autoHide = preferences.getBoolean(KEY_AUTO_HIDE, true);
        autoHideCheckBox.setChecked(autoHide);
        
        // 加载音量键翻页设置
        boolean volumeNav = preferences.getBoolean(KEY_VOLUME_NAV, false);
        volumeNavCheckBox.setChecked(volumeNav);
    }
} 