package com.example.gamemonitoringapp;

import android.app.AppOpsManager;
import android.app.DatePickerDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import com.github.mikephil.charting.data.Entry;

public class MainActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;

    private ListView usageStatsListView;
    private UsageStatsAdapter adapter;
    private EditText fromDateEditText, toDateEditText;
    private Calendar calendar;
    private static final long REFRESH_INTERVAL_MS = 5000; // 5 seconds
    private DatabaseReference dbRef;
    private int year, month, day;
    private BarChart barChart;
    private LineChart lineChart;
    private TextView totalusagelabel;
    private FirebaseAuth mAuth;


    private static final int REQUEST_CODE_ENABLE_ADMIN = 1;


    @Override
    public void onStart() {
        super.onStart();
        performProfileCheck();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Always perform the profile check when returning to this activity
        performProfileCheck();
    }

    private void performProfileCheck() {
        // Get current user's UID from Firebase Authentication
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = currentUser != null ? currentUser.getUid() : null;

        if (currentUser != null) {
            // Use SharedPreferences to manage redirection state
            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
            boolean isRedirected = prefs.getBoolean("isRedirected", false);

            // Check if we need to perform the profile check
            if (!isRedirected) {
                checkUserProfile(uid);
            }
        } else {
            // Handle the case when there is no current user
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        }
    }

    private void checkUserProfile(String uid) {
        DatabaseReference userProfileRef = FirebaseDatabase.getInstance().getReference().child("user_profile");

        userProfileRef.orderByChild("uid").equalTo(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();

                Intent intent;
                if (dataSnapshot.exists()) {
                    // Redirect to HomeActivity if the profile exists
                    intent = new Intent(MainActivity.this, MainActivity.class); // Replace HomeActivity with your actual main content activity
                } else {
                    // Redirect to ProfileActivity for user agreement
                    intent = new Intent(MainActivity.this, ProfileActivity.class);
                }

                // Update SharedPreferences to mark as redirected
                editor.putBoolean("isRedirected", true);
                editor.apply();

                // Start the intent with flags to clear the activity stack
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish(); // Close MainActivity
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Error checking user agreement: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                finish(); // Close the activity to avoid getting stuck
            }
        });
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fromDateEditText = findViewById(R.id.from_date_picker);
        toDateEditText = findViewById(R.id.to_date_picker);
        totalusagelabel = findViewById(R.id.total_usage_label);
        calendar = Calendar.getInstance();
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH);
        day = calendar.get(Calendar.DAY_OF_MONTH);
        ImageButton searchButton = findViewById(R.id.search_button);
        fromDateEditText.setOnClickListener(v -> showDatePickerDialog(fromDateEditText));
        toDateEditText.setOnClickListener(v -> showDatePickerDialog(toDateEditText));
        // Initialize views
        usageStatsListView = findViewById(R.id.usage_stats_list_view);
        String todayDate = String.format("%02d/%02d/%04d", day, month + 1, year);
        fromDateEditText.setText(todayDate);
        toDateEditText.setText(todayDate);

        lineChart = findViewById(R.id.lineChart);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.open, R.string.close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(new NavigationItemSelectedListener(this, drawerLayout));
        NavigationHeaderUpdater headerUpdater = new NavigationHeaderUpdater(this);
        headerUpdater.updateNavigationHeader();



        // Check and request usage stats permission if necessary
        if (!isUsageStatsPermissionGranted()) {
            requestUsageStatsPermission();
        } else {
            startPeriodicRefresh(); // Start the periodic UI refresh
        }

        searchButton.setOnClickListener(v -> {

            String fromDate = formatDate(fromDateEditText.getText().toString().trim());
            String toDate = formatDate(toDateEditText.getText().toString().trim());

            initializeFirebase(fromDate, toDate);

        });






        // Setup ListView item click listener
        setupListViewClickListener();

        // Start the foreground service
        startForegroundService();

        // Schedule the periodic work
        schedulePeriodicWork();

        // Initialize Firebase and fetch data
        initializeFirebase("", "");

    }


    public static long calculateDaysBetweenDates(String fromDate, String toDate) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

        try {
            Date from = dateFormat.parse(fromDate);
            Date to = dateFormat.parse(toDate);

            long diffInMillies = Math.abs(to.getTime() - from.getTime());
            long diff = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);

            return diff + 1 ;
        } catch (ParseException e) {
            e.printStackTrace();
            return -1; // Returning -1 to indicate an error
        }
    }
    public static int getDaysDifference(Date fromDate,Date toDate)
    {
        if(fromDate==null||toDate==null)
            return 0;

        return (int)( (toDate.getTime() - fromDate.getTime()) / (1000 * 60 * 60 * 24));
    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_ENABLE_ADMIN) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Device Admin: enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Device Admin: enabling failed", Toast.LENGTH_SHORT).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    private void showDatePickerDialog(final EditText editText) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, monthOfYear, dayOfMonth) -> {
                    // Update the EditText with the selected date
                    String selectedDate = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year;
                    editText.setText(selectedDate);
                },
                year, month, day);
        datePickerDialog.show();
    }

    private void initializeFirebase(String fromDate, String toDate) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        dbRef = database.getReference("usage_GameStatsInfo");

        // Get current user's UID from Firebase Authentication
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = currentUser != null ? currentUser.getUid() : null;

        Query query = dbRef.orderByChild("userId").equalTo(uid);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<GameUsageData> gameUsageDataList = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    GameUsageData usageData = snapshot.getValue(GameUsageData.class);
                    if (usageData != null) {
                        gameUsageDataList.add(usageData);
                    }
                }

                List<GameUsageData> filteredList = filterByDateRange(gameUsageDataList, fromDate, toDate);

                List<GameUsageData> aggregatedList = aggregateByAppName(filteredList); // Aggregate by app name
                updateUI(aggregatedList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Failed to fetch data: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }




    private List<GameUsageData> aggregateByAppName(List<GameUsageData> dataList) {
        List<GameUsageData> aggregatedList = new ArrayList<>();
        Map<String, GameUsageData> appMap = new HashMap<>();

        for (GameUsageData data : dataList) {
            String appName = data.getAppName();
            if (appMap.containsKey(appName)) {
                GameUsageData existingData = appMap.get(appName);
                long totalTimeInSeconds = parseTimeToSeconds(existingData.getTotalTimeInForeground()) + parseTimeToSeconds(data.getTotalTimeInForeground());
                existingData.setTotalTimeInForeground(formatTimeFromSeconds(totalTimeInSeconds));
            } else {
                appMap.put(appName, data);
            }
        }

        // Convert map values to list
        aggregatedList.addAll(appMap.values());
        return aggregatedList;
    }

    private String formatTimeFromSeconds(long totalTimeInSeconds) {
        long hours = totalTimeInSeconds / 3600;
        long minutes = (totalTimeInSeconds % 3600) / 60;
        long seconds = totalTimeInSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }

    private String formatDate(String dateString) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = inputFormat.parse(dateString);
            if (date != null) {
                return outputFormat.format(date);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "";
    }

    private List<GameUsageData> filterByDateRange(List<GameUsageData> dataList, String fromDate, String toDate) {
        List<GameUsageData> filteredList = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Get current date if fromDate is null or empty
        if (fromDate == null || fromDate.isEmpty()) {
            fromDate = sdf.format(new Date());
        }

        // Get current date if toDate is null or empty
        if (toDate == null || toDate.isEmpty()) {
            toDate = sdf.format(new Date());
        }

        try {
            Date from = sdf.parse(fromDate);
            Date to = sdf.parse(toDate);

            for (GameUsageData data : dataList) {
                String dateString = data.getDate();
                if (dateString != null && !dateString.isEmpty()) {
                    try {
                        Date dataDate = sdf.parse(dateString);
                        if (dataDate != null && !dataDate.before(from) && !dataDate.after(to)) {
                            filteredList.add(data);
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return filteredList;
    }

    private void setupListViewClickListener() {
        usageStatsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Handle item click if needed
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up tasks if necessary
    }


    private long daysInclusive(String startDateStr, String endDateStr) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        Date startDate = sdf.parse(startDateStr);
        Date endDate = sdf.parse(endDateStr);

        long diffInMillies = Math.abs(endDate.getTime() - startDate.getTime());
        return TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS) + 1;
    }
    private void updateUI(List<GameUsageData> gameUsageDataList) {
        // Calculate days between dates
        String fromDateStr = fromDateEditText.getText().toString().trim();
        String toDateStr = toDateEditText.getText().toString().trim();
        long daysBetween = calculateDaysBetweenDates(fromDateStr, toDateStr);

        // Update ListView adapter
        updateListView(gameUsageDataList, daysBetween);

        // Prepare data for line chart
        List<Entry> entries = prepareChartData(gameUsageDataList, daysBetween);
        setupLineChart(entries);

        // Display total usage
        long totalUsageSeconds = calculateTotalUsage(gameUsageDataList);
        totalusagelabel.setText(formatTotalTime(totalUsageSeconds));
    }

    private void updateListView(List<GameUsageData> gameUsageDataList, long daysBetween) {
        if (adapter == null) {
            adapter = new UsageStatsAdapter(this, gameUsageDataList, daysBetween);
            usageStatsListView.setAdapter(adapter);
        } else {
            adapter.setUsageStatsList(gameUsageDataList, daysBetween);
        }
    }

    private List<Entry> prepareChartData(List<GameUsageData> dataList, long daysBetween) {
        List<Entry> entries = new ArrayList<>();
        int index = 0;

        // Ensure that dataList is not empty
        if (dataList != null && !dataList.isEmpty()) {
            for (GameUsageData data : aggregateByAppName(dataList)) {
                long totalTimeSeconds = parseTimeToSeconds(data.getTotalTimeInForeground());
                float percentage = (float) (totalTimeSeconds * 100.0 / (86400 * daysBetween)); // 86400 seconds in a day
                entries.add(new Entry(index++, percentage));
            }
        }

        // Check if entries are empty
        if (entries.isEmpty()) {
            // Handle empty entries if needed
        }

        return entries;
    }

    private void setupLineChart(List<Entry> entries) {
        if (entries.isEmpty()) {
            // Handle empty entries (e.g., show a message or clear the chart)
            lineChart.clear();
            return;
        }
        LineDataSet dataSet = new LineDataSet(entries, "Usage Stats");
        dataSet.setColor(getResources().getColor(R.color.black));
        dataSet.setValueTextColor(getResources().getColor(R.color.black));
        dataSet.setValueTextSize(10f);
        dataSet.setLineWidth(2.5f);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        YAxis yAxisLeft = lineChart.getAxisLeft();
        yAxisLeft.setAxisMinimum(0f);
        yAxisLeft.setAxisMaximum(100f);
        yAxisLeft.setValueFormatter(new PercentFormatter());

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter());
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setLabelRotationAngle(-90f);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(getResources().getColor(R.color.black));

        lineChart.getDescription().setEnabled(false);
        lineChart.invalidate(); // Refresh chart
    }

    private long calculateTotalUsage(List<GameUsageData> gameUsageDataList) {
        long totalUsageSeconds = 0;
        for (GameUsageData data : aggregateByAppName(gameUsageDataList)) {
            totalUsageSeconds += parseTimeToSeconds(data.getTotalTimeInForeground());
        }
        return totalUsageSeconds;
    }

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
            return String.format("Total Usage: %d year/s, %d month/s, %d day/s, %d hour/s, %d minute/s, %d second/s", years, months, days, hours, minutes, seconds);
        } else if (months > 0) {
            return String.format("Total Usage: %d month/s, %d day/s, %d hour/s, %d minute/s, %d second/s", months, days, hours, minutes, seconds);
        } else if (days > 0) {
            return String.format("Total Usage: %d day/s, %d hours, %d minute/s, %d second/s", days, hours, minutes, seconds);
        } else {
            return String.format("Total Usage: %d hour/s, %d minute/s, %d second/s", hours, minutes, seconds);
        }
    }
//    private void updateUI(List<GameUsageData> gameUsageDataList) {
//        // Calculate days between dates
//        String fromDateStr = fromDateEditText.getText().toString().trim();
//        String toDateStr = toDateEditText.getText().toString().trim();
//        long daysBetween = calculateDaysBetweenDates(fromDateStr, toDateStr);
//
//        // Update ListView adapter
//        if (adapter == null) {
//            adapter = new UsageStatsAdapter(this, gameUsageDataList, daysBetween);
//            usageStatsListView.setAdapter(adapter);
//        } else {
//            adapter.setUsageStatsList(gameUsageDataList, daysBetween);
//        }
//
//        // Aggregate data by app name
//        List<GameUsageData> aggregatedList = aggregateByAppName(gameUsageDataList);
//
//        // Prepare data for bar chart
//        ArrayList<BarEntry> entries = new ArrayList<>();
//        ArrayList<String> labels = new ArrayList<>();
//        int index = 0;
//
//        for (GameUsageData data : aggregatedList) {
//            // Calculate percentage of usage for each app
//            long totalTimeSeconds = parseTimeToSeconds(data.getTotalTimeInForeground());
//            float percentage = (float) (totalTimeSeconds * 100.0 / (86400 * daysBetween)); // 86400 seconds in a day
//
//            entries.add(new BarEntry(index++, percentage));
//            labels.add(trimLabel(data.getAppName())); // Trimming label if too long
//        }
//
//        // Setup BarDataSet
//        BarDataSet dataSet = new BarDataSet(entries, "App Usage Percentage");
//        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
//        dataSet.setValueTextColor(getResources().getColor(R.color.black));
//        dataSet.setValueTextSize(10f);
//
//        // Setup BarData and apply to chart
//        BarData barData = new BarData(dataSet);
//        barChart.setData(barData);
//
//        // Customize Y-axis
//        YAxis yAxisLeft = barChart.getAxisLeft();
//        yAxisLeft.setAxisMinimum(0f); // Start at 0%
//        yAxisLeft.setAxisMaximum(100f); // End at 100%
//        yAxisLeft.setValueFormatter(new PercentFormatter());
//
//        // Customize X-axis labels
//        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
//       // barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM); // Position labels at bottom
//
//        // Rotate X-axis labels for better readability (optional)
//        barChart.getXAxis().setLabelRotationAngle(-45f); // Rotate labels by -45 degrees
//
//        // Set text size and color for X-axis labels
//        barChart.getXAxis().setTextSize(10f);
//        barChart.getXAxis().setTextColor(getResources().getColor(R.color.black));
//
//        // Refresh chart
//        barChart.invalidate();
//    }

    private String trimLabel(String label) {
        final int MAX_LABEL_LENGTH = 15; // Adjust this according to your UI constraints
        if (label.length() > MAX_LABEL_LENGTH) {
            return label.substring(0, MAX_LABEL_LENGTH - 3) + "..."; // Trim and add ellipsis
        } else {
            return label;
        }
    }





    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start periodic refresh
                startPeriodicRefresh();
            } else {
                Toast.makeText(this, "Permission denied for usage stats", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private boolean isUsageStatsPermissionGranted() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void requestUsageStatsPermission() {
        startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
    }

    private void startForegroundService() {
        Intent serviceIntent = new Intent(this, MyService.class);
        startService(serviceIntent);
    }

    private void schedulePeriodicWork() {
        Constraints constraints = new Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiresCharging(true)
                .build();

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(MyWorker.class, 16, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "usage_stats_refresh",
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
        );
    }

    private void startPeriodicRefresh() {
        // Example: You might want to refresh every 5 seconds
        // You can adjust the interval according to your needs
        // Here's an example of how you might implement it:
//        Runnable runnable = new Runnable() {
//            @Override
//            public void run() {
//                String fromDate = formatDate(fromDateEditText.getText().toString().trim());
//                String toDate = formatDate(toDateEditText.getText().toString().trim());
//                initializeFirebase(fromDate, toDate); // Refresh data
//                usageStatsListView.postDelayed(this, REFRESH_INTERVAL_MS);
//            }
//        };
//        usageStatsListView.postDelayed(runnable, REFRESH_INTERVAL_MS);
    }

    private long parseTimeToSeconds(String timeString) {
        String[] timeParts = timeString.split(":");
        long hours = Long.parseLong(timeParts[0]);
        long minutes = Long.parseLong(timeParts[1]);
        long seconds = Long.parseLong(timeParts[2]);
        return (hours * 3600) + (minutes * 60) + seconds;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

