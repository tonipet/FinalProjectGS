package com.example.gamemonitoringapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

public class UserAgreementActivity extends AppCompatActivity {

    private static final String TAG = "UserAgreementActivity";
    private DatabaseReference db;
    private FirebaseAuth mAuth;

    private CheckBox agreeCheckBox;
    private Button agreeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_useragreement);

        db = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        agreeCheckBox = findViewById(R.id.agreeCheckbox);
        agreeButton = findViewById(R.id.agreeButton);

        agreeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (agreeCheckBox.isChecked()) {
                    FirebaseUser currentUser = mAuth.getCurrentUser();
                    if (currentUser != null) {
                        String uid = currentUser.getUid();
                        boolean isChecked = agreeCheckBox.isChecked();

                        // Create a new child under 'user_agreements' with a unique key generated by push()
                        DatabaseReference userAgreementsRef = db.child("user_agreements").push();

                        // Map to store agreement data
                        Map<String, Object> agreementData = new HashMap<>();
                        agreementData.put("uid", uid);
                        agreementData.put("isChecked", isChecked);
                        agreementData.put("timestamp", ServerValue.TIMESTAMP); // Use ServerValue.TIMESTAMP for timestamp

                        // Set data to database
                        userAgreementsRef.setValue(agreementData)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        startActivity(new Intent(UserAgreementActivity.this, MainActivity.class));
                                        Toast.makeText(UserAgreementActivity.this, "Agreement added successfully!", Toast.LENGTH_SHORT).show();
                                        finish(); // Finish activity after agreement is successfully added
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(UserAgreementActivity.this, "Failed to add agreement: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        // Handle case where user is not logged in
                        Toast.makeText(UserAgreementActivity.this, "User not logged in", Toast.LENGTH_SHORT).show();
                        // Redirect or handle as appropriate
                    }
                } else {
                    // Handle case where agreement checkbox is not checked
                    Toast.makeText(UserAgreementActivity.this, "Please accept the terms and conditions by clicking the checkbox", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Method to generate agreement text

}