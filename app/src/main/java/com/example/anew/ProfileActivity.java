package com.example.anew;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileActivity extends AppCompatActivity {

    private TextView userNameTextView, userEmailTextView;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        // Enable back navigation in the ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Profile");
        }

        // Initialize UI elements
        userNameTextView = findViewById(R.id.userNameTextView);
        userEmailTextView = findViewById(R.id.userEmailTextView);

        // Set user information (e.g., name and email)
        if (currentUser != null) {
            userNameTextView.setText(currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "User Name");
            userEmailTextView.setText(currentUser.getEmail());
        } else {
            userNameTextView.setText("User Name");
            userEmailTextView.setText("No email available");
        }

        // Discover Button
        ImageButton discoverButton = findViewById(R.id.discoverButton);
        discoverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent discoverIntent = new Intent(ProfileActivity.this, MainActivity.class);
                startActivity(discoverIntent);
                finish(); // Close the ProfileActivity
            }
        });

        // Profile Button (Already in ProfileActivity, so does nothing)
        ImageButton profileButton = findViewById(R.id.profileButton);
        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ProfileActivity.this, "You are already in your profile.", Toast.LENGTH_SHORT).show();
            }
        });

        // Logout Button with confirmation dialog
        Button logoutButton = findViewById(R.id.btn_logout);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogoutConfirmationDialog();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish(); // Close the current activity to navigate back
        return true;
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to log out?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // Sign out from Firebase
                        FirebaseAuth.getInstance().signOut();

                        // Navigate to LoginActivity
                        Intent loginIntent = new Intent(ProfileActivity.this, LoginActivity.class);
                        loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear activity stack
                        startActivity(loginIntent);
                        finish(); // Close ProfileActivity
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }
}
