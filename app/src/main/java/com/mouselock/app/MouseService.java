package com.mouselock.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

public class MouseService extends Service {
    
    static {
        System.loadLibrary("mouselock");
    }

    private WindowManager windowManager;
    private View overlayView;
    private boolean isLocked = false;
    
    // Native methods
    private native void nativeStartLock(String shPath);
    private native void nativeStopLock();
    private native void nativeSetLocked(boolean locked);

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, createNotification());
        createOverlay();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Initialize native with Shizuku shell path
        String shizukuPath = ShizukuHelper.getShizukuShellPath(this);
        nativeStartLock(shizukuPath);
        return START_STICKY;
    }

    private void createOverlay() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 50;
        params.y = 100;

        LayoutInflater inflater = LayoutInflater.from(this);
        overlayView = inflater.inflate(R.layout.overlay_layout, null);

        // Apply glassmorphism blur effect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            overlayView.setRenderEffect(
                RenderEffect.createBlurEffect(25f, 25f, Shader.TileMode.CLAMP)
            );
        }

        Button lockButton = overlayView.findViewById(R.id.lockButton);
        lockButton.setOnClickListener(v -> toggleLock(lockButton));

        windowManager.addView(overlayView, params);
    }

    private void toggleLock(Button button) {
        isLocked = !isLocked;
        nativeSetLocked(isLocked);
        
        if (isLocked) {
            button.setText("LOCKED");
            button.setBackgroundResource(R.drawable.glow_border);
        } else {
            button.setText("UNLOCKED");
            button.setBackgroundResource(R.drawable.normal_border);
        }
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                "mouse_lock_channel",
                "Mouse Lock Service",
                NotificationManager.IMPORTANCE_LOW
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }

    private Notification createNotification() {
        return new Notification.Builder(this, "mouse_lock_channel")
                .setContentTitle("Mouse Lock Active")
                .setContentText("Service running in background")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        nativeStopLock();
        if (overlayView != null) {
            windowManager.removeView(overlayView);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}