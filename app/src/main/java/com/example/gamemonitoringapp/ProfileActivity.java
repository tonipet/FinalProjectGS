package com.example.gamemonitoringapp;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private static final int PICK_IMAGE_REQUEST = 1;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private Uri profileImageUri;
    private ImageView profileImage;
    private DatabaseReference userProfileRef;
    private DatabaseReference sectionRef;
    private StorageReference storageReference;
    private TextView emailTextView, phoneNo, parentName, NotoNofity, student,passwordEditText;
    private Spinner gender, section;
    private String selectedSectionId;
    private TextInputEditText startTimeEditText, endTimeEditText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        passwordEditText = findViewById(R.id.password);
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        mAuth = FirebaseAuth.getInstance();
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        emailTextView = findViewById(R.id.emailTextView);
        phoneNo = findViewById(R.id.phoneNo);
        parentName = findViewById(R.id.parentName);
        section = findViewById(R.id.section);
        NotoNofity = findViewById(R.id.hoursToNotify);
        gender = findViewById(R.id.gender);
        student = findViewById(R.id.StudentName);
        startTimeEditText = findViewById(R.id.startTime);
        endTimeEditText = findViewById(R.id.endTime);
        // profileImage = findViewById(R.id.profileImage);
        MaterialButton registerButton = findViewById(R.id.registerButton);

        // Initialize Firebase Database and Storage references
        userProfileRef = FirebaseDatabase.getInstance().getReference().child("user_profile");
        sectionRef = FirebaseDatabase.getInstance().getReference().child("user_section"); // Reference to sections in Firebase
        storageReference = FirebaseStorage.getInstance().getReference();

        // Initialize the gender dropdown
        String[] genders = new String[]{"Male", "Female"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, genders);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gender.setAdapter(genderAdapter);

        // Initialize the section dropdown
        ArrayAdapter<String> sectionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        sectionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        section.setAdapter(sectionAdapter);
        populateSectionDropdown(sectionAdapter);

        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(new NavigationItemSelectedListener(this, drawerLayout));
        NavigationHeaderUpdater headerUpdater = new NavigationHeaderUpdater(this);
        headerUpdater.updateNavigationHeader();

        loadProfileData();
        // profileImage.setOnClickListener(v -> openFileChooser());

        // Set register button click listener
        registerButton.setOnClickListener(v -> saveProfileData());
        // Set on focus change listener to show time picker dialog
        startTimeEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    showTimePickerDialog(startTimeEditText); // Show time picker when focused
                }
            }
        });

        endTimeEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    showTimePickerDialog(endTimeEditText); // Show time picker when focused
                }
            }
        });
    }
    private void showTimePickerDialog(final TextInputEditText timeEditText) {
        // Get the current time
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        // Create and show the TimePickerDialog
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                ProfileActivity.this,
                android.R.style.Theme_Holo_Light_Dialog_MinWidth,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        // Format the time to HH:mm format
                        String time = String.format("%02d:%02d", hourOfDay, minute);
                        // Set the formatted time to the TextInputEditText
                        timeEditText.setText(time);
                    }
                },
                hour,
                minute,
                true // 24-hour format
        );
        timePickerDialog.show();
    }

    private void populateSectionDropdown(ArrayAdapter<String> adapter) {
        sectionRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> sectionNames = new ArrayList<>();
                Map<String, String> sectionIdMap = new HashMap<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String id = (String) snapshot.child("UID").getValue(); // The ID of the section
                    String sectionName = (String) snapshot.child("sectionName").getValue();
                    if (sectionName != null) {
                        sectionNames.add(sectionName);
                        sectionIdMap.put(sectionName, id);
                    }
                }
                adapter.clear();
                adapter.addAll(sectionNames);
                adapter.notifyDataSetChanged();

                // Use setOnItemSelectedListener instead of setOnItemClickListener
                section.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        String selectedSectionName = (String) parent.getItemAtPosition(position);
                        selectedSectionId = sectionIdMap.get(selectedSectionName);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        // Handle the case when no item is selected if needed
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("ProfileActivity", "Failed to load sections: " + databaseError.getMessage());
                Toast.makeText(ProfileActivity.this, "Failed to load sections", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProfileData() {






        String password = passwordEditText.getText().toString().trim();
        String email = emailTextView.getText().toString().trim();
        String phone = phoneNo.getText().toString().trim();
        String parent = parentName.getText().toString().trim();
        String sectionText = (String) section.getSelectedItem(); // Updated to get selected item from Spinner
        String NoofhoursText = NotoNofity.getText().toString().trim();
        String genderText = (String) gender.getSelectedItem(); // Updated to get selected item from Spinner
        String studentText = student.getText().toString().trim();
        // Get start and end times from the EditTexts
        String startTime = startTimeEditText.getText().toString().trim();
        String endTime = endTimeEditText.getText().toString().trim();


        if (email.isEmpty() || phone.isEmpty() || parent.isEmpty() || sectionText.isEmpty() || genderText.isEmpty() || studentText.isEmpty() || password.isEmpty() || startTime.isEmpty() || endTime.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser currentUser = mAuth.getCurrentUser();
                        if (currentUser != null) {
                            String userId = currentUser.getUid();
                            Map<String, Object> profileData = new HashMap<>();
                            profileData.put("email", email);
                            profileData.put("phone", phone);
                            profileData.put("parentName", parent);
                            profileData.put("studentName", studentText);
                            profileData.put("sectionId", selectedSectionId); // Save the section ID
                            profileData.put("noofhours", NoofhoursText);
                            profileData.put("gender", genderText);
                            profileData.put("uid", userId);
                            profileData.put("startTime", startTime); // Save start time
                            profileData.put("endTime", endTime); // Save end time

                            // Add the Base64-encoded profile image
                            // if (profileImageUri != null) {
                            //     String base64Image = getBase64Image(profileImageUri);
                            //     if (base64Image != null) {
                            //         profileData.put("profileImage", base64Image);
                            //     }
                            // }

                            userProfileRef.child(userId).setValue(profileData).addOnCompleteListener(UserProfiletask -> {
                                if (UserProfiletask.isSuccessful()) {
                                    Toast.makeText(ProfileActivity.this, "Profile data saved successfully", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(ProfileActivity.this, "Failed to save profile data", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {
                        Toast.makeText(ProfileActivity.this, "Password Incorrect.", Toast.LENGTH_SHORT).show();
                    }
                });




    }

    private String getBase64Image(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void displayUserEmail() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String email = user.getEmail();
            if (email != null) {
                emailTextView.setText(email);
            }
        }
    }

    private void loadProfileData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            userProfileRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        Map<String, Object> profileData = (Map<String, Object>) dataSnapshot.getValue();
                        if (profileData != null) {
                            String email = (String) profileData.get("email");
                            String phone = (String) profileData.get("phone");
                            String parent = (String) profileData.get("parentName");
                            String sectionId = (String) profileData.get("sectionId"); // This should be the UID of the section
                            String NoofhoursText = (String) profileData.get("noofhours");
                            String genderText = (String) profileData.get("gender");
                            String studentText = (String) profileData.get("studentName");
                            String startTime = (String) profileData.get("startTime"); // Load start time
                            String endTime = (String) profileData.get("endTime"); // Load end time

                            emailTextView.setText(email);
                            phoneNo.setText(phone);
                            parentName.setText(parent);
                            NotoNofity.setText(NoofhoursText);
                            gender.setSelection(((ArrayAdapter<String>) gender.getAdapter()).getPosition(genderText));
                            student.setText(studentText);
                            // Set the start and end times
                            startTimeEditText.setText(startTime);
                            endTimeEditText.setText(endTime);

                            if (sectionId != null) {
                                sectionRef.orderByChild("UID").equalTo(sectionId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                            String sectionName = (String) snapshot.child("sectionName").getValue();
                                            if (sectionName != null) {
                                                section.setSelection(((ArrayAdapter<String>) section.getAdapter()).getPosition(sectionName));
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                        Log.e("ProfileActivity", "Failed to load section name: " + databaseError.getMessage());
                                    }
                                });
                            }
                        }
                    }else{
                        displayUserEmail();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("ProfileActivity", "Failed to load profile data: " + databaseError.getMessage());
                }
            });
        }
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            profileImageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), profileImageUri);
                profileImage.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
