package com.example.anew;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

import android.Manifest;
import android.content.pm.PackageManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore db;
    private EditText emailEditText, passwordEditText, firstNameEditText, lastNameEditText;
    private Button registerButton;
    private TextView loginRedirect;
    private ImageView profilePictureView;
    private Uri selectedImageUri;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri pickedUri = result.getData().getData();
                    selectedImageUri = copyUriToLocalFile(pickedUri);
                    Glide.with(this)
                            .load(selectedImageUri)
                            .circleCrop()
                            .into(profilePictureView);
                }
            });

    private static final int REQUEST_IMAGE_PERMISSION = 1002;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.your_color));
        }
        setContentView(R.layout.activity_register);

        // Initialize Cloudinary
        initCloudinary();

        firebaseAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emailEditText = findViewById(R.id.email_register);
        passwordEditText = findViewById(R.id.password_register);
        firstNameEditText = findViewById(R.id.first_name_register);
        lastNameEditText = findViewById(R.id.last_name_register);
        registerButton = findViewById(R.id.btn_register);
        loginRedirect = findViewById(R.id.login_redirect);
        profilePictureView = findViewById(R.id.profile_picture_register);

        profilePictureView.setOnClickListener(v -> checkImagePermissionAndPick());

        registerButton.setOnClickListener(v -> {
            String email = emailEditText.getText() != null ? emailEditText.getText().toString().trim() : "";
            String password = passwordEditText.getText() != null ? passwordEditText.getText().toString().trim() : "";
            String firstName = firstNameEditText.getText() != null ? firstNameEditText.getText().toString().trim() : "";
            String lastName = lastNameEditText.getText() != null ? lastNameEditText.getText().toString().trim() : "";

            if (email.isEmpty() || password.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
                Toast.makeText(RegisterActivity.this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            registerUser(email, password, firstName, lastName);
        });

        loginRedirect.setOnClickListener(v -> startActivity(new Intent(RegisterActivity.this, LoginActivity.class)));
    }

    private void initCloudinary() {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dulcec599");
        config.put("api_key", "863532565741378");
        config.put("api_secret", "tTMJlyTk4Za_IvbpSHhWpEbaNz0");
        MediaManager.init(this, config);
    }

    private void registerUser(String email, String password, String firstName, String lastName) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String uid = firebaseAuth.getCurrentUser().getUid();

                        // Upload profile picture if selected
                        if (selectedImageUri == null) {
                            createUserDocument(uid, firstName, lastName, email, null);
                            return;
                        }
                        uploadProfilePicture(uid, firstName, lastName, email);
                    } else {
                        Toast.makeText(RegisterActivity.this,
                                "Registration failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void uploadProfilePicture(String uid, String firstName, String lastName, String email) {
        if (selectedImageUri == null) {
            Toast.makeText(this, "No image selected!", Toast.LENGTH_SHORT).show();
            return;
        }

        MediaManager.get().upload(selectedImageUri)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = resultData.get("secure_url").toString();
                        createUserDocument(uid, firstName, lastName, email, imageUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(RegisterActivity.this, 
                            "Upload failed: " + error.getDescription(), 
                            Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }

    private void createUserDocument(String uid, String firstName, String lastName, String email, String profilePictureUrl) {
        User user = new User(uid, email, firstName, lastName, profilePictureUrl);
        db.collection("users").document(uid)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    // Update Firebase Auth profile
                    UserProfileChangeRequest.Builder profileUpdatesBuilder = new UserProfileChangeRequest.Builder()
                            .setDisplayName(firstName + " " + lastName);
                    if (profilePictureUrl != null) {
                        profileUpdatesBuilder.setPhotoUri(Uri.parse(profilePictureUrl));
                    }
                    firebaseAuth.getCurrentUser().updateProfile(profileUpdatesBuilder.build())
                            .addOnCompleteListener(profileTask -> {
                                if (profileTask.isSuccessful()) {
                                    // Send verification email
                                    firebaseAuth.getCurrentUser().sendEmailVerification()
                                            .addOnCompleteListener(verificationTask -> {
                                                if (verificationTask.isSuccessful()) {
                                                    Toast.makeText(RegisterActivity.this,
                                                            "Registration successful! Please verify your email.",
                                                            Toast.LENGTH_LONG).show();
                                                    firebaseAuth.signOut();
                                                    finish();
                                                } else {
                                                    Toast.makeText(RegisterActivity.this,
                                                            "Registration successful but failed to send verification email.",
                                                            Toast.LENGTH_LONG).show();
                                                    firebaseAuth.signOut();
                                                    finish();
                                                }
                                            });
                                } else {
                                    Toast.makeText(RegisterActivity.this,
                                            "Failed to update profile: " + profileTask.getException().getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterActivity.this,
                            "Failed to create user profile: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void checkImagePermissionAndPick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_IMAGE_PERMISSION);
                return;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_IMAGE_PERMISSION);
                return;
            }
        }
        pickImage();
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_IMAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickImage();
            } else {
                Toast.makeText(this, "Permission denied to access images", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Uri copyUriToLocalFile(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            File tempFile = File.createTempFile("profile_pic", ".jpg", getCacheDir());
            OutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.close();
            inputStream.close();
            return Uri.fromFile(tempFile);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
