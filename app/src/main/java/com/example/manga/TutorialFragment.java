package com.example.manga;

import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * 教程片段，向用户展示如何使用漫画阅读器和漫画文件夹的结构
 */
public class TutorialFragment extends Fragment {

    private static final String DEFAULT_MANGA_FOLDER = Environment.getExternalStorageDirectory() + "/manga";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tutorial, container, false);
        
        // 设置漫画文件夹路径
        TextView tvFolderPath = view.findViewById(R.id.tvFolderPath);
        tvFolderPath.setText(getString(R.string.manga_folder_path, DEFAULT_MANGA_FOLDER));
        
        // 设置"明白了"按钮点击事件
        Button btnGotIt = view.findViewById(R.id.btnGotIt);
        btnGotIt.setOnClickListener(v -> {
            // 关闭教程，返回到主屏幕
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadLibraryFragment();
            }
        });
        
        return view;
    }

    /**
     * 创建一个新的教程片段实例
     * @return 新的TutorialFragment实例
     */
    public static TutorialFragment newInstance() {
        return new TutorialFragment();
    }
} 