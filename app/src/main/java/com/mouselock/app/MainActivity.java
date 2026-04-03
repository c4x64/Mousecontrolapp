package com.mouselock.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {

    private static final int REQUEST_CODE_OVERLAY = 1001;
    private static final int SHIZUKU_REQUEST_CODE = 1002;

    private final Shizuku.OnRequestPermissionResultListener shizukuPermissionListener =
        (requestCode, grantResult) -> {
            if (requestCode == SHIZUKU_REQUEST_CODE) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Shizuku permission granted", Toast.LENGTH_SHORT).show();
                    startMouseService();
                } else {
                    Toast.makeText(this, "Shizuku permission denied", Toast.LENGTH_LONG).show();
                }
            }
        };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setStatusBarColor(0xFF171717);
        getWindow().setNavigationBarColor(0xFF171717);

        // Register listener early
        Shizuku.addRequestPermissionResultListener(shizukuPermissionListener);

        Button startServiceBtn = findViewById(R.id.startServiceBtn);
        startServiceBtn.setOnClickListener(v -> checkAllPermissions());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener);
    }

    private void checkAllPermissions() {
        // Step 1: overlay permission
        if (!Settings.canDrawOverlays(this)) {
            new AlertDialog.Builder(this)
                .setTitle("Overlay Permission Required")
                .setMessage("Needed to show the lock button on screen.")
                .setPositiveButton("Grant", (d, w) -> {
                    startActivityForResult(
                        new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName())),
                        REQUEST_CODE_OVERLAY);
                })
                .setNegativeButton("Cancel", null)
                .show();
            return;
        }

        // Step 2: Shizuku running?
        try {
            if (!Shizuku.pingBinder()) {
                new AlertDialog.Builder(this)
                    .setTitle("Shizuku Not Running")
                    .setMessage("Please start Shizuku first, then tap START again.")
                    .setPositiveButton("OK", null)
                    .show();
                return;
            }
        } catch (Exception e) {
            Toast.makeText(this, "Shizuku not available: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        // Step 3: Shizuku permission
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            if (Shizuku.shouldShowRequestPermissionRationale()) {
                new AlertDialog.Builder(this)
                    .setTitle("Shizuku Permission Required")
                    .setMessage("This app needs Shizuku permission to control the mouse.")
                    .setPositiveButton("Request", (d, w) ->
                        Shizuku.requestPermission(SHIZUKU_REQUEST_CODE))
                    .setNegativeButton("Cancel", null)
                    .show();
            } else {
                Shizuku.requestPermission(SHIZUKU_REQUEST_CODE);
            }
            return;
        }

        // All good
        startMouseService();
    }

    private void startMouseService() {
        try {
            startForegroundService(new Intent(this, MouseService.class));
            Toast.makeText(this, "Mouse Lock Service Started", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to start service: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
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
