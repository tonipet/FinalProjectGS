package com.example.gamemonitoringapp;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;

import java.util.Calendar;
import java.util.List;

public class UsageStatsUtil {

    public static List<UsageStats> getUsageStats(Context context, long startTime, long endTime) {
        UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        return usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime);
    }

    public static long getTodayStartTimeInMillis() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        seconds = seconds % 60;
        minutes = minutes % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
