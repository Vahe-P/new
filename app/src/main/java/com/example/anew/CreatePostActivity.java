package com.example.anew;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.cloudinary.json.JSONException;
import org.cloudinary.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CreatePostActivity extends AppCompatActivity {
    private ImageButton btnBack;
    private Button btnPost;
    private ImageView postImagePreview;
    private Button btnSelectImage;
    private TextInputEditText postContent;
    private MaterialButtonToggleGroup locationToggleGroup;
    private Button btnCurrentLocation;
    private Button btnChooseLocation;
    private TextView selectedLocation;
    private Uri selectedImageUri;
    private GeoPoint selectedLocationPoint;
    private String selectedLocationName;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize views
        btnBack = findViewById(R.id.btnBack);
        btnPost = findViewById(R.id.btnPost);
        postImagePreview = findViewById(R.id.postImagePreview);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        postContent = findViewById(R.id.postContent);
        locationToggleGroup = findViewById(R.id.locationToggleGroup);
        btnCurrentLocation = findViewById(R.id.btnCurrentLocation);
        btnChooseLocation = findViewById(R.id.btnChooseLocation);
        selectedLocation = findViewById(R.id.selectedLocation);

        // Set up image picker
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    Glide.with(this)
                        .load(selectedImageUri)
                        .into(postImagePreview);
                }
            }
        );

        // Set up click listeners
        btnBack.setOnClickListener(v -> finish());

        btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        btnPost.setOnClickListener(v -> createPost());

        btnCurrentLocation.setOnClickListener(v -> getCurrentLocation());

        btnChooseLocation.setOnClickListener(v -> {
            Intent intent = new Intent(this, MapActivity.class);
            intent.putExtra("selectLocation", true);
            startActivityForResult(intent, 1);
        });
    }

    private void getCurrentLocation() {
        if (checkLocationPermission()) {
            fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        selectedLocationPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                        selectedLocationName = "Current Location";
                        selectedLocation.setText(selectedLocationName);
                    } else {
                        Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                    }
                });
        }
    }

    private boolean checkLocationPermission() {
        // TODO: Implement location permission check
        return true;
    }

    private void createPost() {
        String content = postContent.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Please write something", Toast.LENGTH_SHORT).show();
            return;
        }

        btnPost.setEnabled(false);
        String userId = auth.getCurrentUser().getUid();

        // First upload image if selected
        if (selectedImageUri != null) {
            uploadImageAndCreatePost(userId, content);
        } else {
            createPostInFirestore(userId, content, null);
        }
    }
    private static final String CLOUDINARY_UPLOAD_URL = "https://api.cloudinary.com/v1_1/dulcec599/image/upload";
    private static final String CLOUDINARY_UPLOAD_PRESET = "unsigned_android_upload";

    private void uploadImageAndCreatePost(String userId, String content) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
            byte[] imageBytes = getBytes(inputStream);

            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();

            okhttp3.MultipartBody.Builder builder = new okhttp3.MultipartBody.Builder().setType(okhttp3.MultipartBody.FORM)
                    .addFormDataPart("file", "image.jpg", okhttp3.RequestBody.create(imageBytes, okhttp3.MediaType.parse("image/*")))
                    .addFormDataPart("upload_preset", CLOUDINARY_UPLOAD_PRESET);

            okhttp3.RequestBody requestBody = builder.build();

            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(CLOUDINARY_UPLOAD_URL)
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(CreatePostActivity.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                        btnPost.setEnabled(true);
                    });
                }

                @Override
                public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                    String responseBody = response.body().string();
                    if (!response.isSuccessful()) {
                        runOnUiThread(() -> {
                            Log.e("CloudinaryError", "Code: " + response.code() + ", Body: " + responseBody);
                            Toast.makeText(CreatePostActivity.this, "Image upload failed", Toast.LENGTH_SHORT).show();
                            btnPost.setEnabled(true);
                        });
                        return;
                    }

                    try {
                        JSONObject json = new JSONObject(responseBody);
                        String imageUrl = json.getString("secure_url");

                        runOnUiThread(() -> createPostInFirestore(userId, content, imageUrl));
                    } catch (JSONException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            Toast.makeText(CreatePostActivity.this, "Error parsing Cloudinary response", Toast.LENGTH_SHORT).show();
                            btnPost.setEnabled(true);
                        });
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error reading image", Toast.LENGTH_SHORT).show();
            btnPost.setEnabled(true);
        }
    }
    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }

        return byteBuffer.toByteArray();
    }


    private void createPostInFirestore(String userId, String content, String imageUrl) {
        Map<String, Object> post = new HashMap<>();
        post.put("userId", userId);
        post.put("content", content);
        post.put("imageUrl", imageUrl);
        post.put("timestamp", System.currentTimeMillis());
        post.put("likes", new HashMap<String, Boolean>());
        post.put("comments", new HashMap<String, Object>());

        if (selectedLocationPoint != null) {
            post.put("location", selectedLocationPoint);
            post.put("locationName", selectedLocationName);
        }

        db.collection("posts")
            .add(post)
            .addOnSuccessListener(documentReference -> {
                Toast.makeText(this, "Post created successfully", Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> {

                Log.e("FirestoreError", "Error adding post: ", e);
                Toast.makeText(this, "Failed to create post", Toast.LENGTH_SHORT).show();
                btnPost.setEnabled(true);
            });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            double lat = data.getDoubleExtra("lat", 0);
            double lng = data.getDoubleExtra("lng", 0);
            String locationName = data.getStringExtra("locationName");
            
            selectedLocationPoint = new GeoPoint(lat, lng);
            selectedLocationName = locationName;
            selectedLocation.setText(locationName);
        }
    }
} 