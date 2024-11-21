package com.example.gamemonitoringapp;

import android.app.AppOpsManager;
import android.content.Context;
import android.os.Build;
import androidx.annotation.RequiresApi;

public class Utils {
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static boolean isUsageStatsPermissionGranted(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }
}
