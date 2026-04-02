package com.mouselock.app;

import android.content.Context;

public class ShizukuHelper {
    public static String getShizukuShellPath(Context context) {
        return "/data/user/" + android.os.Process.myUserHandle().hashCode() +
               "/moe.shizuku.privileged.api/files/rish";
    }
}
