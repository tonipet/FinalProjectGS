package com.example.gamemonitoringapp;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MyService extends Service {
    private Map<String, Long> appStartTimes = new HashMap<>();
    private Map<String, Long> appUsageDurations = new HashMap<>();
    private Map<String, Long> lastNotificationTimes = new HashMap<>(); // To store the last notification time per app
    private Map<String, Long> lastRunTimes = new HashMap<>();
    private static final String CHANNEL_ID = "ForegroundServiceChannel";
    private Handler handler = new Handler();
    private FirebaseFirestore db;
    private PackageManager packageManager;
    private FirebaseAuth mAuth;
    private DatabaseReference userProfileRef;
    private ItexmoSmsSender smsSender;

    private static final String VONAGE_SMS_API_URL = "https://rest.nexmo.com/sms/json";
    private static final String API_KEY = "b12103ff";
    private static final String API_SECRET = "Welcome12@3";
    private HashSet<String> blockedApps = new HashSet<>(); // Store blocked app package names
    private Map<String, String> lastNotifiedRunTimes = new HashMap<>();
    private Map<String, String> LastNotif = new HashMap<>();
    private Map<String, String> LastNotifRuns = new HashMap<>();


    public interface IsSMSSentCallback {
        void onResult(boolean isSent);
    }
    public interface OnTotalTimeFetchedListener {
        void onTotalTimeFetched(long totalTimeInSeconds, String appUsageDetails);
        void onFailure(String errorMessage);
    }
    @Override
    public void onCreate() {
        userProfileRef = FirebaseDatabase.getInstance().getReference().child("user_profile");
        smsSender = new ItexmoSmsSender();

        super.onCreate();
        createNotificationChannel();
        Notification notification = getNotification();
        startForeground(1, notification);

        db = FirebaseFirestore.getInstance();
        packageManager = getPackageManager();
        mAuth = FirebaseAuth.getInstance();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                insertDataIntoFirestore();
                handler.postDelayed(this, 5000); // 1 minute delay  300000
            }
        }, 5000); // Initial delay of 30 seconds
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {

        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification getNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Game Monitoring Service")
                .setContentText("Monitoring game usage...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();
    }

    private void sendSms(String phoneNumber, String message) {
        SmsManager sms = SmsManager.getDefault();
        ArrayList<String> parts = sms.divideMessage(message);
        sms.sendMultipartTextMessage(phoneNumber, null, parts, null, null);
    }

    private void IsSMSSent(String currentUserId, String currentDate, IsSMSSentCallback callback) {
        String documentName = currentUserId + "_" + currentDate;
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("User_SMS").child(documentName);

        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    callback.onResult(true);
                } else {
                    saveIsSentInDatabase(documentName, true, currentUserId, currentDate, callback);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("MyService", "Database error: " + databaseError.getMessage());
                callback.onResult(false);
            }
        });
    }

    private void saveIsSentInDatabase(String documentName, boolean isSent, String currentUserId, String currentDate, IsSMSSentCallback callback) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("User_SMS");
        Map<String, Object> data = new HashMap<>();
        data.put("isSent", isSent);
        data.put("currentUserId", currentUserId);
        data.put("currentDate", currentDate);

        dbRef.child(documentName).setValue(data)
                .addOnSuccessListener(aVoid -> {
                    Log.d("MyService", "Data saved in database for document: " + documentName);
                    callback.onResult(false);
                })
                .addOnFailureListener(e -> {
                    Log.e("MyService", "Failed to save data in database for document: " + documentName, e);
                    callback.onResult(true);
                });
    }

    private void insertDataIntoFirestore() {
        long startTime = getTodayStartTimeInMillis(); // Start of the day (12:00 AM)
        long endTime = System.currentTimeMillis();     // Current time
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String currentUserId = currentUser != null ? currentUser.getUid() : "unknown_user";

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("usage_GameStatsInfo");

        // Using UsageStatsManager to retrieve usage events
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        UsageEvents usageEvents = usageStatsManager.queryEvents(startTime, endTime);

        Map<String, Long> appUsageDurations = new HashMap<>();
        Map<String, Long> appStartTimes = new HashMap<>(); // To store app start times

        while (usageEvents.hasNextEvent()) {
            UsageEvents.Event event = new UsageEvents.Event();
            usageEvents.getNextEvent(event);

            String packageName = event.getPackageName();
            long timeStamp = event.getTimeStamp();

            try {
                ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
                if (isGameApp(appInfo)) {

                    Log.d("MyService", "Processing UsageStats for app: " + packageName + ", Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(timeStamp)));

                    if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                        // App entered foreground

                        appStartTimes.put(packageName, timeStamp); // Store start time
                    } else if (event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                        // App entered background

                        Long startTimeForApp = appStartTimes.remove(packageName); // Remove and get the start time
                        if (startTimeForApp != null) {
                            long usageDuration = timeStamp - startTimeForApp;
                            appUsageDurations.put(packageName, appUsageDurations.getOrDefault(packageName, 0L) + usageDuration);
                            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                            long accumulatedUsage = appUsageDurations.getOrDefault(packageName, 0L) + usageDuration;
                            // Store the last time the app was in the foreground
                            lastRunTimes.put(packageName, timeStamp); // Store the last run time

                            String formattedStartTime = timeFormat.format(new Date(startTimeForApp)); // Format start time
                            String formattedEndTime = timeFormat.format(new Date(timeStamp)); // Format end time
                            String totalDuration = formatDuration(usageDuration);
                            // Now insert usage time to Firestore
                            insertUsageTimeToFirestore(currentUserId, currentDate, getAppName(packageName), packageName, formattedStartTime, formattedEndTime,totalDuration);


                            userProfileRef.child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    // Check if the user profile exists
                                    if (dataSnapshot.exists()) {
                                        // Fetch allowed usage time from Firebase
                                        String noOfHoursStr = dataSnapshot.child("noofhours").getValue(String.class);
                                        String CPNumber = dataSnapshot.child("phone").getValue(String.class);
                                        String studentName = dataSnapshot.child("studentName").getValue(String.class);

                                        fetchAllowedUsageTime(currentUserId, new OnAllowedTimeFetchedListener() {
                                            @Override
                                            public void onAllowedTimeFetched(long allowedTimeInSeconds, Date startTime, Date endTime) {
                                                long currentTimeMillis = System.currentTimeMillis();
                                                Calendar calendar = Calendar.getInstance();

                                                // Get the last run time for this app
                                                Long lastRunTime = lastRunTimes.get(packageName); // Get the last run time for this app
                                                String lastRunFormatted = lastRunTime != null ? timeFormat.format(new Date(lastRunTime)) : "None";

                                                // Get the last notified run time for comparison
                                                String previousNotifiedRunTime = lastNotifiedRunTimes.get(packageName);

                                                // Check if the current time is within the allowed period
                                                boolean isWithinAllowedPeriod = true;
                                                if (startTime != null && endTime != null) {
                                                    Calendar startCalendar = Calendar.getInstance();
                                                    startCalendar.setTime(startTime);
                                                    long startSeconds = startCalendar.get(startCalendar.HOUR_OF_DAY) * 3600 +
                                                            startCalendar.get(startCalendar.MINUTE) * 60 +
                                                            startCalendar.get(startCalendar.SECOND);

                                                    Calendar endCalendar = Calendar.getInstance();
                                                    endCalendar.setTime(endTime);
                                                    long endSeconds = endCalendar.get(Calendar.HOUR_OF_DAY) * 3600 +
                                                            endCalendar.get(Calendar.MINUTE) * 60 +
                                                            endCalendar.get(Calendar.SECOND);
                                                    Calendar lastRunCalendar = Calendar.getInstance();
                                                    if (!lastRunFormatted.equals("None")) {
                                                        try {
                                                            // Parse lastRunFormatted back to a Date object using the same format as timeFormat
                                                            Date parsedDate = timeFormat.parse(lastRunFormatted);
                                                            lastRunCalendar.setTime(parsedDate); // Set the parsed date in the calendar
                                                            long lastRunSeconds = lastRunCalendar.get(Calendar.HOUR_OF_DAY) * 3600 +
                                                                    lastRunCalendar.get(Calendar.MINUTE) * 60 +
                                                                    lastRunCalendar.get(Calendar.SECOND);
                                                            isWithinAllowedPeriod = (lastRunSeconds >= startSeconds && lastRunSeconds <= endSeconds);
                                                        } catch (ParseException e) {
                                                            e.printStackTrace(); // Handle exception if parsing fails
                                                        }
                                                    }






                                                }

                                                // Only proceed if lastRunFormatted is different from the previous notification and usage condition is met
                                                if ((!lastRunFormatted.equals(previousNotifiedRunTime)) &&
                                                        (!isWithinAllowedPeriod)) {

                                                    // Get the last notification time for this app
                                                    long lastNotificationTime = lastNotificationTimes.getOrDefault(packageName, 0L);
                                                    long NotifcurrentTimeMillis = System.currentTimeMillis();
                                                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());


                                                    String formattedTime = timeFormat.format(new Date(NotifcurrentTimeMillis));
                                                    // Check if 5 seconds have passed since the last notification for this app
                                                    if (NotifcurrentTimeMillis - lastNotificationTime >= 1 * 5000) { // 5-second interval for testing  10 * 60 * 1000
                                                        String message = "This is to notify that "+studentName+" is playing " + getAppName(packageName) +
                                                                ".\n Last run time: " + lastRunFormatted + ".\n This is outside allowed period you give.";
                                                        Log.d("MyService", message);

                                                        // Uncomment to send SMS notification
                                                     SentSMSitextmo(CPNumber, message);
                                                         insertUsageNotification(currentUserId,  formattedTime, currentDate,getAppName(packageName));
                                                        // Update last notification time and last notified run time for this specific app
                                                        lastNotificationTimes.put(packageName, System.currentTimeMillis());
                                                        lastNotifiedRunTimes.put(packageName, lastRunFormatted);
                                                    }
                                                }
                                            }
                                        });
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    Log.e("MyService", "Error fetching user profile: " + databaseError.getMessage());
                                }
                            });




                        }
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }





        // Output usage durations
        StringBuilder usageDetails = new StringBuilder();
        for (Map.Entry<String, Long> entry : appUsageDurations.entrySet()) {
            String appName = entry.getKey();
            long totalTimeInForegroundMillis = entry.getValue();
            String totalTimeFormatted = formatTime(totalTimeInForegroundMillis);

            try {
                ApplicationInfo appInfo = packageManager.getApplicationInfo(appName, 0);
                String packageName = appInfo.packageName;
                String applicationName = getAppName(packageName);
                String base64Logo = getBase64AppLogo(packageName);

                String documentName = currentUserId + "_" + applicationName + "_" + currentDate;

                // Query Firestore to check if the document already exists
                dbRef.child(documentName).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        // Document already exists, update only the other details
                        Map<String, Object> updateData = new HashMap<>();
                        updateData.put("totalTimeInForeground", totalTimeFormatted);
                        updateData.put("date", currentDate);
                        updateData.put("userId", currentUserId);
                        updateData.put("userIdDate", currentUserId + "_" + currentDate);

                        dbRef.child(documentName).updateChildren(updateData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("MyService", "Data updated in Firestore for app: " + applicationName);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("MyService", "Failed to update data in Firestore for app: " + applicationName, e);
                                });
                    } else {
                        // Document does not exist, proceed with insertion including the logo
                        Map<String, Object> usageData = new HashMap<>();
                        usageData.put("appName", applicationName);
                        usageData.put("totalTimeInForeground", totalTimeFormatted);
                        usageData.put("date", currentDate);
                        usageData.put("userId", currentUserId);
                        usageData.put("userIdDate", currentUserId + "_" + currentDate);
                        usageData.put("appLogo", base64Logo);

                        dbRef.child(documentName).setValue(usageData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("MyService", "Data inserted into Firestore for app: " + applicationName);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("MyService", "Failed to insert data into Firestore for app: " + applicationName, e);
                                });
                    }
                });

                usageDetails.append(applicationName).append(": ").append(totalTimeFormatted).append(", ");

            } catch (PackageManager.NameNotFoundException e) {
                Log.e("MyService", "Failed to get package name for app: " + appName, e);
            }
        }

        Log.d("MyService", "Usage Details: " + usageDetails.toString());

        if (appUsageDurations.isEmpty()) {
            Log.d("MyService", "No app usage data to insert into Firestore.");
        }

        checkAndSendSMS(currentUserId, currentDate);
    }


    private void fetchAllowedUsageTime(String currentUserId, OnAllowedTimeFetchedListener listener) {
        userProfileRef.child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String noOfHoursStr = dataSnapshot.child("noofhours").getValue(String.class);
                    String endTimeStr = dataSnapshot.child("endTime").getValue(String.class);
                    String startTimeStr = dataSnapshot.child("startTime").getValue(String.class);

                    try {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

                        // Parse start and end times
                        Date startTimeDate = startTimeStr != null ? dateFormat.parse(startTimeStr) : null;
                        Date endTimeDate = endTimeStr != null ? dateFormat.parse(endTimeStr) : null;

                        long allowedTimeInSeconds = noOfHoursStr != null ? (long) (Double.parseDouble(noOfHoursStr) * 3600) : Long.MAX_VALUE;

                        listener.onAllowedTimeFetched(allowedTimeInSeconds, startTimeDate, endTimeDate);
                    } catch (NumberFormatException | ParseException e) {
                        Log.e("MyService", "Failed to parse time limits", e);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("MyService", "Database error: " + databaseError.getMessage());
            }
        });
    }

    public interface OnAllowedTimeFetchedListener {
        void onAllowedTimeFetched(long allowedTimeInSeconds, Date startTime, Date endTime);
    }


    private void insertUsageNotification(String userId, String CurrentDate, String NotifTime, String AppName) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("usage_GameStatsNotifInfoDetails");

        // Create a unique document name based on the user, app, and date
        String documentName = userId + "_" + AppName + "_" +CurrentDate+ "_" + NotifTime ; // Add start time to make it unique

        // Create a map to store the start and end times
        Map<String, Object> usageData = new HashMap<>();
        usageData.put("UID", userId);
        usageData.put("endTime", CurrentDate);
        usageData.put("date", NotifTime);
        usageData.put("userId", userId);
        usageData.put("appName", AppName);

        dbRef.child(documentName).setValue(usageData)
                .addOnSuccessListener(aVoid -> Log.d("MyService", "Start and end times inserted for app: " + AppName))
                .addOnFailureListener(e -> Log.e("MyService", "Failed to insert data for app: " + AppName, e));



    }
    // Function to insert the usage time data (start and end times)
    private void insertUsageTimeToFirestore(String userId, String date, String appName, String packageName, String startTime, String endTime, String TotalTime) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("usage_GameStatsInfoDetails");

        // Create a unique document name based on the user, app, and date
        String documentName = userId + "_" + appName + "_" + date + "_" + startTime; // Add start time to make it unique

        // Create a map to store the start and end times
        Map<String, Object> usageData = new HashMap<>();
        usageData.put("startTime", startTime);
        usageData.put("endTime", endTime);
        usageData.put("date", date);
        usageData.put("userId", userId);
        usageData.put("appName", appName);
        usageData.put("packageName", packageName);
        usageData.put("TotalTime", TotalTime);



        // Insert or update the data in Firestore
        dbRef.child(documentName).setValue(usageData)
                .addOnSuccessListener(aVoid -> Log.d("MyService", "Start and end times inserted for app: " + appName))
                .addOnFailureListener(e -> Log.e("MyService", "Failed to insert data for app: " + appName, e));
    }

    // Optional method to format total duration into HH:mm:ss format
    private String formatDuration(long durationInMillis) {
        int hours = (int) (durationInMillis / (1000 * 60 * 60));
        int minutes = (int) ((durationInMillis % (1000 * 60 * 60)) / (1000 * 60));
        int seconds = (int) ((durationInMillis % (1000 * 60)) / 1000);
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }



    private boolean isMidnight() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        return hour == 0;
    }
    private void checkAndSendSMS(String currentUserId, String currentDate) {
        userProfileRef.child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String noOfHoursStr = dataSnapshot.child("noofhours").getValue(String.class);
                    String CPNumber = dataSnapshot.child("phone").getValue(String.class);
                    String studentName = dataSnapshot.child("studentName").getValue(String.class);

                    if (noOfHoursStr != null) {
                        double noOfHours;
                        try {
                            noOfHours = Double.parseDouble(noOfHoursStr);
                        } catch (NumberFormatException e) {
                            Log.e("MyService", "Failed to parse noOfHours: " + noOfHoursStr, e);
                            return; // Exit if the value is not a valid number
                        }

                        long totalUsageTimeForDay = convertHoursToSeconds(noOfHours);
                        String previousNotifiedRunTime = LastNotifRuns.get("LastNotifRUns");
                        fetchTotalApplicationTime(new OnTotalTimeFetchedListener() {
                            @Override
                            public void onTotalTimeFetched(long totalTimeInSeconds, String fetchTotalApplicationTime) {
                                // Check if the actual usage exceeds the allowed time
                                String Totaltimeapp = formatTotalTimeSMS(totalTimeInSeconds);
                                if (totalUsageTimeForDay < totalTimeInSeconds) {

                                    IsSMSSent(currentUserId, currentDate, new IsSMSSentCallback() {
                                        @Override
                                        public void onResult(boolean isSent) {
                                         /*   if (!isSent) {*/
                                            Log.e("MyService", "Total previousNotifiedRunTime: " + previousNotifiedRunTime);
                                            Log.e("MyService", "Total Totaltimeapp: " + Totaltimeapp);

                                            if(previousNotifiedRunTime == null || !previousNotifiedRunTime.equals(Totaltimeapp)){
                                                // Construct the SMS message
                                                String message = "This is to notify that " + studentName +
                                                        " has exceeded the allowed usage time.\n" +
                                                        formatTotalTimeSMS(totalTimeInSeconds) +
                                                        "\nList of Games:\n" + fetchTotalApplicationTime;



                                                Log.e("MyService", "Total Notif: " + message);
                                                // Send the SMS using the itextmo service
                                                //Toast.makeText(MyService.this, "sent sent sent", Toast.LENGTH_SHORT).show();
                                               SentSMSitextmo(CPNumber, message);

                                            }

                                            LastNotifRuns.put("LastNotifRUns", formatTotalTimeSMS(totalTimeInSeconds));
                                        }
                                     /*   }*/
                                    });
                                }
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                // Handle the error
                                Toast.makeText(MyService.this, "Failed to fetch data: " + errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("MyService", "Failed to load profile data: " + databaseError.getMessage());
                Toast.makeText(MyService.this, "Failed to load profile data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String formatTotalTimeSMS(long totalTimeSeconds) {
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
            return String.format("Total Usage: %d year/s, %d month/s, %d day/s, %d hour/s, %d minute/s, %d second/s", years, months, days, hours, minutes, seconds);
        } else if (months > 0) {
            return String.format("Total Usage: %d month/s, %d day/s, %d hour/s, %d minute/s, %d second/s", months, days, hours, minutes, seconds);
        } else if (days > 0) {
            return String.format("Total Usage: %d day/s, %d hours, %d minute/s, %d second/s", days, hours, minutes, seconds);
        } else {
            return String.format("Total Usage: %d hour/s, %d minute/s, %d second/s", hours, minutes, seconds);
        }
    }


    private long convertHoursToSeconds(double hours) {
        return (long) (hours * 3600);
    }
    private long getTodayStartTimeInMillis() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private boolean isGameApp(ApplicationInfo appInfo) {
        return (appInfo.flags & ApplicationInfo.FLAG_IS_GAME) != 0 ||
                appInfo.category == ApplicationInfo.CATEGORY_GAME;
    }

    private String getAppName(String packageName) {
        try {
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
            return (String) packageManager.getApplicationLabel(appInfo);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "Unknown App";
        }
    }

    private String getBase64AppLogo(String packageName) {
        try {
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
            Drawable appIcon = packageManager.getApplicationIcon(appInfo);
            Bitmap bitmap;

            // Handle different drawable types
            if (appIcon instanceof BitmapDrawable) {
                bitmap = ((BitmapDrawable) appIcon).getBitmap();
            } else {
                bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                appIcon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                appIcon.draw(canvas);
            }

            // Resize the bitmap to 16x16 pixels for minimal size
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 16, 16, false);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // Use JPEG format with lower quality for better compression
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream); // Adjust quality as needed
            byte[] byteArray = outputStream.toByteArray();

            // Log sizes
            Log.d("MyService", "Original size: " + bitmap.getByteCount());
            Log.d("MyService", "Base64 size: " + byteArray.length);

            return Base64.encodeToString(byteArray, Base64.DEFAULT);

        } catch (PackageManager.NameNotFoundException e) {
            Log.e("MyService", "Package not found: " + packageName, e);
        } catch (Exception e) {
            Log.e("MyService", "Error retrieving app logo for package: " + packageName, e);
        }
        return ""; // Return empty if any failure occurs
    }




    private void fetchTotalApplicationTime(OnTotalTimeFetchedListener listener) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference dbRef = database.getReference("usage_GameStatsInfo");
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Get current user's UID from Firebase Authentication
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = currentUser != null ? currentUser.getUid() : null;

        Query query = dbRef.orderByChild("userIdDate").equalTo(uid + "_" + currentDate);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long totalApplicationTime = 0;  // Accumulator for total time in foreground
                StringBuilder resultBuilder = new StringBuilder();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String totalTimeInForeground = snapshot.child("totalTimeInForeground").getValue(String.class);
                    String appName = snapshot.child("appName").getValue(String.class);


                    if (totalTimeInForeground != null) {
                        totalApplicationTime += parseTimeToSeconds(totalTimeInForeground);
                    }
                    resultBuilder.append(appName)
                            .append(": ")
                            .append(totalTimeInForeground)
                            .append("\n");
                }

                // Pass the result to the listener
                if (listener != null) {
                    listener.onTotalTimeFetched(totalApplicationTime, resultBuilder.toString());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Pass the error message to the listener
                if (listener != null) {
                    listener.onFailure(databaseError.getMessage());
                }
            }
        });
    }

    private long parseTimeToSeconds(String timeString) {
        String[] timeParts = timeString.split(":");
        long hours = Long.parseLong(timeParts[0]);
        long minutes = Long.parseLong(timeParts[1]);
        long seconds = Long.parseLong(timeParts[2]);
        return (hours * 3600) + (minutes * 60) + seconds;
    }


    private String formatTime(long timeInMillis) {
        long seconds = timeInMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes % 60, seconds % 60);
    }

    private void SentSMSitextmo(String to, String message) {
        // Ensure recipient and message are not empty
        if (to == null || to.isEmpty() || message == null || message.isEmpty()) {
            Log.d("MyService", "Recipient or message cannot be empty");
            return;
        }

        // Call the sendSms method from ItexmoSmsSender
        smsSender.sendSms(to, message, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                // Use Log.d to log failure
                Log.d("MyService", "Failed to send SMS", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    // Use Log.d to log success
                    Log.d("MyService", "SMS sent: " + responseBody);
                } else {
                    // Use Log.d to log failure with response code
                    Log.d("MyService", "Failed: " + response.code());
                }
            }
        });
    }


}
