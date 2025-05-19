package com.example.anew;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import com.cloudinary.Cloudinary;

public class EditProfileActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private EditText firstNameEditText, lastNameEditText;
    private ImageView profileImageView;
    private Button saveButton, pickImageButton;
    private Uri selectedImageUri;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set status bar color to blue
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.your_color));
        }
        setContentView(R.layout.activity_edit_profile);

        firstNameEditText = findViewById(R.id.editFirstName);
        lastNameEditText = findViewById(R.id.editLastName);
        profileImageView = findViewById(R.id.editProfileImageView);
        saveButton = findViewById(R.id.saveProfileButton);
        pickImageButton = findViewById(R.id.pickImageButton);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Load current user info
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid()).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    firstNameEditText.setText(doc.getString("firstName"));
                    lastNameEditText.setText(doc.getString("lastName"));
                    String profilePictureUrl = doc.getString("profilePictureUrl");
                    if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
                        Glide.with(this).load(profilePictureUrl).circleCrop().into(profileImageView);
                    }
                }
            });
        }

        pickImageButton.setOnClickListener(v -> pickImage());

        saveButton.setOnClickListener(v -> saveProfile());

        initCloudinary();
    }
    private void initCloudinary() {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dulcec599");
        config.put("api_key", "863532565741378");
        config.put("api_secret", "tTMJlyTk4Za_IvbpSHhWpEbaNz0");
        MediaManager.init(this, config);
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            Glide.with(this).load(selectedImageUri).circleCrop().into(profileImageView);
        }
    }

    private void saveProfile() {
        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();
        if (firstName.isEmpty()) {
            Toast.makeText(this, "First name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUser == null) return;
        if (selectedImageUri != null) {
            // Upload new profile picture
            MediaManager.get().upload(selectedImageUri)
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {}

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {}

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String imageUrl = resultData.get("secure_url").toString();
                            updateFirestoreProfile(firstName, lastName, imageUrl);  // Store in Firestore
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            Toast.makeText(EditProfileActivity.this, "Upload error: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {}
                    })
                    .dispatch();

        } else {
            // No new image, just update name
            updateFirestoreProfile(firstName, lastName, null);
        }
    }

    private void updateFirestoreProfile(String firstName, String lastName, String profilePictureUrl) {
        String uid = currentUser.getUid();
        db.collection("users").document(uid).update(
                "firstName", firstName,
                "lastName", lastName,
                "profilePictureUrl", profilePictureUrl != null ? profilePictureUrl : currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : null
        ).addOnSuccessListener(aVoid -> {
            // Update Firebase Auth profile
            UserProfileChangeRequest.Builder profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(firstName + " " + lastName);
            if (profilePictureUrl != null) {
                profileUpdates.setPhotoUri(Uri.parse(profilePictureUrl));
            }
            currentUser.updateProfile(profileUpdates.build()).addOnCompleteListener(task -> {
                Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(EditProfileActivity.this, ProfileActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
} 