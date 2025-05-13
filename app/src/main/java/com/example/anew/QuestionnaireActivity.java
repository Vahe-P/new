package com.example.anew;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Map;

public class QuestionnaireActivity extends AppCompatActivity {

    private CheckBox church, hotel, library, park,museum,mountain;
    private Button submitButton;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questionnaire);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.your_color));
        }

        // Initialize Firebase Auth and Realtime Database
        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();


        // Find UI elements
        church = findViewById(R.id.church);
        hotel = findViewById(R.id.hotels);
        park = findViewById(R.id.parks);
        museum = findViewById(R.id.museums);
        mountain = findViewById(R.id.mountains);
        library=findViewById(R.id.lib);
        submitButton = findViewById(R.id.submitButton);

        // Button listeners
        submitButton.setOnClickListener(v -> savePreferences());
        // Write a message to the database

    }

    private void savePreferences() {
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            Log.e("QuestionnaireActivity", "User is null");
            return;
        }

        if (!user.isEmailVerified()) {
            Toast.makeText(this, "Please verify your email first.", Toast.LENGTH_LONG).show();
            return;
        }

        List<String> selectedOptions = new ArrayList<>();
        if (church.isChecked()) selectedOptions.add("Churches");
        if (hotel.isChecked()) selectedOptions.add("Hotels");
        if (library.isChecked()) selectedOptions.add("Libraries");
        if (park.isChecked()) selectedOptions.add("Parks");
        if (museum.isChecked()) selectedOptions.add("Museums");
        if (mountain.isChecked()) selectedOptions.add("Mountains");

        if (selectedOptions.isEmpty()) {
            selectedOptions.add("skip");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("selectedPlaces", selectedOptions);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("UserPreferences")
                .document(user.getUid())
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Preferences saved!", Toast.LENGTH_SHORT).show();
                    navigateToMainActivity();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save preferences: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("FirestoreError", "Exception: ", e);
                });
    }


    private void navigateToMainActivity() {
        Intent intent = new Intent(QuestionnaireActivity.this, MainActivity.class);
        startActivity(intent);
        finish(); // Optional: closes the current activity
    }
}
