package com.example.gamemonitoringapp;

public class GameUsageData {
    private String appName;
    private String totalTimeInForeground;
    private String appLogo; // Base64 encoded logo
    private String date;

    public GameUsageData() {
        // Default constructor required for calls to DataSnapshot.getValue(GameUsageData.class)
    }

    // Constructor with correct parameter name for dateTransaction
    public GameUsageData(String appName, String totalTimeInForeground, String appLogo, String date) {
        this.appName = appName;
        this.totalTimeInForeground = totalTimeInForeground;
        this.appLogo = appLogo;
        this.date = date; // Correct assignment here
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getTotalTimeInForeground() {
        return totalTimeInForeground;
    }

    public void setTotalTimeInForeground(String totalTimeInForeground) {
        this.totalTimeInForeground = totalTimeInForeground;
    }

    public String getAppLogo() {
        return appLogo;
    }

    public void setAppLogo(String appLogo) {
        this.appLogo = appLogo;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}


