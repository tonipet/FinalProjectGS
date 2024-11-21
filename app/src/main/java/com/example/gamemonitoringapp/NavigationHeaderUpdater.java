package com.example.gamemonitoringapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.android.material.navigation.NavigationView;

public class NavigationHeaderUpdater {

    private final Context context;

    public NavigationHeaderUpdater(Context context) {
        this.context = context;
    }

    public void updateNavigationHeader() {
        NavigationView navigationView = ((AppCompatActivity) context).findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);

        ImageView profileImage = headerView.findViewById(R.id.nav_user);


        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference userProfileRef = FirebaseDatabase.getInstance().getReference("user_profile").child(userId);

            userProfileRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String base64Image = dataSnapshot.child("profileImage").getValue(String.class);
                        if (base64Image != null) {
                            byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            profileImage.setImageBitmap(decodedBitmap);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Handle errors
                }
            });
        }
    }
}
