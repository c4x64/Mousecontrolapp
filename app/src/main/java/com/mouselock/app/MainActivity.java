package com.mouselock.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {
    
    private static final int REQUEST_CODE_OVERLAY = 1001;
    private static final int REQUEST_CODE_SHIZUKU = 1002;

    private final Shizuku.OnRequestPermissionResultListener requestPermissionResultListener = 
        (requestCode, grantResult) -> {
            if (grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Shizuku permission granted", Toast.LENGTH_SHORT).show();
                checkAllPermissions();
            } else {
                Toast.makeText(this, "Shizuku permission denied", Toast.LENGTH_LONG).show();
            }
        };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Force dark theme
        getWindow().setStatusBarColor(0xFF171717);
        getWindow().setNavigationBarColor(0xFF171717);

        Button startServiceBtn = findViewById(R.id.startServiceBtn);
        startServiceBtn.setOnClickListener(v -> checkAllPermissions());

        Shizuku.addRequestPermissionResultListener(requestPermissionResultListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener);
    }

    private void checkAllPermissions() {
        // Check overlay permission
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_CODE_OVERLAY);
            return;
        }

        // Check Shizuku permission
        if (Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            if (Shizuku.shouldShowRequestPermissionRationale()) {
                Toast.makeText(this, "Shizuku permission required for mouse control", 
                    Toast.LENGTH_LONG).show();
            }
            Shizuku.requestPermission(REQUEST_CODE_SHIZUKU);
            return;
        }

        // All permissions granted, start service
        startMouseService();
    }

    private void startMouseService() {
        Intent serviceIntent = new Intent(this, MouseService.class);
        startForegroundService(serviceIntent);
        Toast.makeText(this, "Mouse Lock Service Started", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OVERLAY) {
            if (Settings.canDrawOverlays(this)) {
                checkAllPermissions();
            } else {
                Toast.makeText(this, "Overlay permission required", Toast.LENGTH_LONG).show();
            }
        }
    }
}