package com.example.gamemonitoringapp;

import android.content.Context;
import android.content.Intent;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class NavigationItemSelectedListener implements NavigationView.OnNavigationItemSelectedListener {

    private final Context context;
    private final DrawerLayout drawerLayout;

    public NavigationItemSelectedListener(Context context, DrawerLayout drawerLayout) {
        this.context = context;
        this.drawerLayout = drawerLayout;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.navigation_home) {
            context.startActivity(new Intent(context, MainActivity.class));
        } else if (id == R.id.navigation_profile) {
            context.startActivity(new Intent(context, ProfileActivity.class));
        } else if (id == R.id.logout) {
            context.startActivity(new Intent(context,LogoutActivity.class));
    }


        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }




}

