package com.example.anew;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if the user is logged in using Firebase Authentication
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            // User is not logged in, redirect to LoginActivity
            Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(loginIntent);
            finish();  // Prevent going back to MainActivity after login
            return;
        }

        // If logged in, show MainActivity layout
        setContentView(R.layout.activity_main);

        // Discover Button
        ImageButton discoverButton = findViewById(R.id.discoverButton);
        discoverButton.setOnClickListener(v -> {
            // Handle discover button click (Add functionality later)
        });

        // Profile Button
        ImageButton profileButton = findViewById(R.id.profileButton);
        profileButton.setOnClickListener(v -> {
            Intent profileIntent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(profileIntent);
        });
    }


}
