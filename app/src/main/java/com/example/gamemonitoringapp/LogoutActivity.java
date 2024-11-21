package com.example.gamemonitoringapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LogoutActivity  extends AppCompatActivity {
    private EditText emailEditText, passwordEditText;
    private DrawerLayout drawerLayout;
    private Button loginButton;
    private TextView createAccountTextView,forgotPasswordTextView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ActionBarDrawerToggle toggle;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        Toolbar toolbar = findViewById(R.id.toolbar);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        drawerLayout = findViewById(R.id.drawer_layout);
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        loginButton = findViewById(R.id.loginButton);
        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(new NavigationItemSelectedListener(this, drawerLayout));
        NavigationHeaderUpdater headerUpdater = new NavigationHeaderUpdater(this);
        headerUpdater.updateNavigationHeader();


        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(LogoutActivity.this, "Enter Email Address", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(LogoutActivity.this, "Enter password", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            logout();
                        } else {
                            Toast.makeText(LogoutActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                        }
                    });
        });





    }
    private void logout() {



        FirebaseAuth.getInstance().signOut();
        stopService(new Intent(this, MyService.class));
        // Navigate to login activity
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        this.startActivity(intent);
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
    }


}
