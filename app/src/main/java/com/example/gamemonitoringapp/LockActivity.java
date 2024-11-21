package com.example.gamemonitoringapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class LockActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock);

        TextView lockMessage = findViewById(R.id.lock_message);
        Button unlockButton = findViewById(R.id.unlock_button);

        Intent intent = getIntent();
        String packageName = intent.getStringExtra("PACKAGE_NAME");
        lockMessage.setText("You have reached your time limit for " + packageName);

        unlockButton.setOnClickListener(v -> finish());
    }
}
