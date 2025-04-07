package com.example.manga.util;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.StringRes;

import com.example.manga.R;

/**
 * 自定义Toast工具类，用于显示不带图标的纯文本消息
 */
public class ToastUtil {
    
    /**
     * 显示不带图标的纯文本短Toast消息
     * @param context 上下文
     * @param message 消息内容
     */
    public static void showShort(Context context, String message) {
        show(context, message, Toast.LENGTH_SHORT);
    }
    
    /**
     * 显示不带图标的纯文本短Toast消息（使用资源ID）
     * @param context 上下文
     * @param messageResId 消息资源ID
     */
    public static void showShort(Context context, @StringRes int messageResId) {
        if (context == null) return;
        show(context, context.getString(messageResId), Toast.LENGTH_SHORT);
    }
    
    /**
     * 显示不带图标的纯文本长Toast消息
     * @param context 上下文
     * @param message 消息内容
     */
    public static void showLong(Context context, String message) {
        show(context, message, Toast.LENGTH_LONG);
    }
    
    /**
     * 显示不带图标的纯文本长Toast消息（使用资源ID）
     * @param context 上下文
     * @param messageResId 消息资源ID
     */
    public static void showLong(Context context, @StringRes int messageResId) {
        if (context == null) return;
        show(context, context.getString(messageResId), Toast.LENGTH_LONG);
    }
    
    /**
     * 显示不带图标的纯文本Toast消息
     * @param context 上下文
     * @param message 消息内容
     * @param duration 显示时长
     */
    private static void show(Context context, String message, int duration) {
        if (context == null) return;
        
        try {
            // 创建一个自定义布局的Toast
            Toast toast = new Toast(context);
            
            // 使用纯文本布局
            View view = LayoutInflater.from(context).inflate(R.layout.custom_toast, null);
            TextView textView = view.findViewById(R.id.toast_text);
            textView.setText(message);
            
            // 设置Toast参数
            toast.setView(view);
            toast.setDuration(duration);
            toast.setGravity(Gravity.CENTER, 0, 0);
            
            // 显示Toast
            toast.show();
        } catch (Exception e) {
            // 如果自定义Toast失败，回退到系统Toast
            Toast.makeText(context, message, duration).show();
        }
    }
} 