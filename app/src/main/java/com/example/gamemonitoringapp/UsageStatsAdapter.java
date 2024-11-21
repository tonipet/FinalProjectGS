package com.example.gamemonitoringapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class UsageStatsAdapter extends BaseAdapter {

    private Context context;
    private List<GameUsageData> usageStatsList;
    private long daysBetween;


    public UsageStatsAdapter(Context context, List<GameUsageData> usageStatsList,long daysBetween) {
        this.context = context;
        this.usageStatsList = usageStatsList;
        this.daysBetween = daysBetween;
    }

    public void setUsageStatsList(List<GameUsageData> usageStatsList,long daysBetween) {
        this.usageStatsList = usageStatsList;
        this.daysBetween = daysBetween;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return usageStatsList.size();
    }

    @Override
    public Object getItem(int position) {
        return usageStatsList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_usage, parent, false);
            holder = new ViewHolder();
            holder.appIconImageView = convertView.findViewById(R.id.app_icon);
            holder.appNameTextView = convertView.findViewById(R.id.app_name_text_view);
            holder.totalTimeInForegroundTextView = convertView.findViewById(R.id.total_time_in_foreground_text_view);
            holder.totalTimeInForegroundBar = convertView.findViewById(R.id.total_time_in_foreground_bar); // Initialize the progress bar
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        GameUsageData usageData = usageStatsList.get(position);
        String appName = usageData.getAppName();
        String totalTimeInForeground = usageData.getTotalTimeInForeground();
        String base64Logo = usageData.getAppLogo();

        holder.appNameTextView.setText(appName);

        if (totalTimeInForeground != null) {
            try {
                long totalTimeSeconds = parseTimeToSeconds(totalTimeInForeground); // Convert time string to total seconds
                long maxTotalTimeSeconds = 86400 * this.daysBetween; // Example: 1 day in seconds, adjust as per your needs

                // Calculate percentage
                double percentage = (double) totalTimeSeconds / maxTotalTimeSeconds * 100.0;

                String totalTimeText =  String.format("%s (%.1f%%)", formatTotalTime(totalTimeSeconds), percentage);

                holder.totalTimeInForegroundTextView.setText(totalTimeText);
                // Format total time text with percentage
//                String totalTimeText = String.format("%s (%.1f%%)", totalTimeInForeground, percentage);
//                holder.totalTimeInForegroundTextView.setText(totalTimeText);

                // Set layout params for the progress bar
                ViewGroup.LayoutParams params = holder.totalTimeInForegroundBar.getLayoutParams();
                params.width = (int) (parent.getWidth() * (percentage / 100.0));
                holder.totalTimeInForegroundBar.setLayoutParams(params);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        } else {
            holder.totalTimeInForegroundTextView.setText(""); // Clear text if totalTimeInForeground is null
        }

        if (base64Logo != null && !base64Logo.isEmpty()) {
            Bitmap decodedBitmap = decodeBase64(base64Logo);
            holder.appIconImageView.setImageBitmap(decodedBitmap);
        } else {
            // holder.appIconImageView.setImageResource(R.drawable.default_app_icon);
        }

        return convertView;
    }

    // Helper method to convert time format "HH:mm:ss" to total seconds
    private long parseTimeToSeconds(String timeString) {
        String[] parts = timeString.split(":");
        if (parts.length == 3) {
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            int seconds = Integer.parseInt(parts[2]);
            return hours * 3600 + minutes * 60 + seconds; // Convert to total seconds
        }
        return 0;
    }

    // Helper method to decode Base64 string to Bitmap
    private Bitmap decodeBase64(String base64String) {
        byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }


    // Helper method to format total time in a readable format including days, months, and years
    private String formatTotalTime(long totalTimeSeconds) {
        long secondsInMinute = 60;
        long secondsInHour = secondsInMinute * 60;
        long secondsInDay = secondsInHour * 24;
        long secondsInMonth = secondsInDay * 30; // Approximation for a month
        long secondsInYear = secondsInDay * 365; // Approximation for a year

        long years = totalTimeSeconds / secondsInYear;
        long months = (totalTimeSeconds % secondsInYear) / secondsInMonth;
        long days = (totalTimeSeconds % secondsInMonth) / secondsInDay;
        long hours = (totalTimeSeconds % secondsInDay) / secondsInHour;
        long minutes = (totalTimeSeconds % secondsInHour) / secondsInMinute;
        long seconds = totalTimeSeconds % secondsInMinute;

        if (years > 0) {
            return String.format("%d year/s, %d month/s, %d day/s, %d hour/s, %d minute/s, %d second/s", years, months, days, hours, minutes, seconds);
        } else if (months > 0) {
            return String.format("%d month/s, %d day/s, %d hour/s, %d minute/s, %d second/s", months, days, hours, minutes, seconds);
        } else if (days > 0) {
            return String.format("%d day/s, %d hours, %d minute/s, %d second/s", days, hours, minutes, seconds);
        } else {
            return String.format("%d hour/s, %d minute/s, %d second/s", hours, minutes, seconds);
        }
    }

    private static class ViewHolder {
        ImageView appIconImageView;
        TextView appNameTextView;
        TextView totalTimeInForegroundTextView;
        View totalTimeInForegroundBar; // Progress bar
    }

}
