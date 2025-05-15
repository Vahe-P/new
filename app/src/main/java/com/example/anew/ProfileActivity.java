package com.example.anew;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;

public class ProfileActivity extends AppCompatActivity {
    private TextView userEmailTextView;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private EditText searchBar;
    private MaterialButton prefer;
    private ImageButton discoverButton;
    private ImageButton profileButton;
    private ImageButton favoritesButton;
    private ImageButton logoutButton;
    private MaterialButtonToggleGroup toggleGroup;
    private MaterialButton btnFromMyPlace;
    private MaterialButton btnChooseInMap;
    private MaterialButton sourcesButton;
    private MaterialButton feedbackButton;
    private MaterialButton changePasswordButton;
    private MaterialButton deleteAccountButton;
    private LinearLayout accountManagementButtonsContainer;
    private MaterialButton helpCenterButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.your_color));
        }
        setContentView(R.layout.activity_profile);

        initializeViews();
        setupUserInterface();
        setupListeners();
    }

    private void initializeViews() {
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        userEmailTextView = findViewById(R.id.userEmailTextView);
        searchBar = findViewById(R.id.searchBar);
        prefer = findViewById(R.id.preferencesButton);
        
        // Initialize toggle group
        toggleGroup = findViewById(R.id.toggleGroup);
        btnFromMyPlace = findViewById(R.id.btnFromMyPlace);
        btnChooseInMap = findViewById(R.id.btnChooseInMap);
        
        // Initialize bottom navigation buttons
        discoverButton = findViewById(R.id.discoverButton);
        profileButton = findViewById(R.id.profileButton);
        favoritesButton = findViewById(R.id.favoritesButton);
        logoutButton = findViewById(R.id.btn_logout);

        // Set up notification button
        ImageButton notificationButton = findViewById(R.id.notificationButton);
        notificationButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, NotificationsActivity.class);
            startActivity(intent);
        });

        sourcesButton = findViewById(R.id.sourcesButton);
        feedbackButton = findViewById(R.id.feedbackButton);

        changePasswordButton = findViewById(R.id.changePasswordButton);
        deleteAccountButton = findViewById(R.id.deleteAccountButton);
        accountManagementButtonsContainer = findViewById(R.id.accountManagementButtonsContainer);
        helpCenterButton = findViewById(R.id.helpCenterButton);
    }

    private void setupUserInterface() {
        // Load previously saved data
        loadSavedData();

        if (currentUser != null) {
            userEmailTextView.setText(currentUser.getEmail());
            accountManagementButtonsContainer.setVisibility(View.VISIBLE);
        } else {
            userEmailTextView.setText("Guest Mode");
            accountManagementButtonsContainer.setVisibility(View.GONE);
        }

        // Set up toggle group
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                String selection = checkedId == R.id.btnFromMyPlace ? "From my place" : "Choose in map";
                saveData(selection, searchBar.getText().toString());
            }
        });
    }

    private void setupListeners() {
        discoverButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });

        profileButton.setOnClickListener(v ->
            Toast.makeText(ProfileActivity.this, "You are already in your profile.", Toast.LENGTH_SHORT).show()
        );

        favoritesButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, FavoritesActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });

        logoutButton.setOnClickListener(v -> showLogoutConfirmationDialog());

        prefer.setOnClickListener(v ->
            startActivity(new Intent(ProfileActivity.this, QuestionnaireActivity.class))
        );

        sourcesButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, SourcesActivity.class);
            startActivity(intent);
        });

        feedbackButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:wanderlymobile@gmail.com"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Wanderly App Feedback");
            
            try {
                startActivity(Intent.createChooser(intent, "Send Feedback"));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(ProfileActivity.this, 
                    "No email client installed", 
                    Toast.LENGTH_SHORT).show();
            }
        });

        changePasswordButton.setOnClickListener(v -> showChangePasswordDialog());
        deleteAccountButton.setOnClickListener(v -> showDeleteAccountDialog());

        helpCenterButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, HelpCenterActivity.class);
            startActivity(intent);
        });
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to log out?")
                .setCancelable(false)
                .setPositiveButton("Yes", (dialog, id) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent loginIntent = new Intent(ProfileActivity.this, LoginActivity.class);
                    loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(loginIntent);
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void saveData(String fromWhere, String inputDistance) {
        SharedPreferences sharedPreferences = getSharedPreferences("ProfilePrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("fromWhere", fromWhere);
        editor.putString("inputDistance", inputDistance);
        editor.apply();
    }

    private void loadSavedData() {
        SharedPreferences sharedPreferences = getSharedPreferences("ProfilePrefs", MODE_PRIVATE);
        String savedFromWhere = sharedPreferences.getString("fromWhere", "From my place");
        String savedInputDistance = sharedPreferences.getString("inputDistance", "");

        // Set the toggle button based on saved preference
        if (savedFromWhere.equals("From my place")) {
            toggleGroup.check(R.id.btnFromMyPlace);
        } else {
            toggleGroup.check(R.id.btnChooseInMap);
        }

        if (!TextUtils.isEmpty(savedInputDistance)) {
            searchBar.setText(savedInputDistance);
        }
    }

    private void showChangePasswordDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null);
        TextInputEditText currentPasswordInput = dialogView.findViewById(R.id.currentPasswordInput);
        TextInputEditText newPasswordInput = dialogView.findViewById(R.id.newPasswordInput);
        TextInputEditText confirmPasswordInput = dialogView.findViewById(R.id.confirmPasswordInput);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Change Password")
                .setView(dialogView)
                .setPositiveButton("Change", (dialog, which) -> {
                    String currentPassword = currentPasswordInput.getText().toString();
                    String newPassword = newPasswordInput.getText().toString();
                    String confirmPassword = confirmPasswordInput.getText().toString();

                    if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                        Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!newPassword.equals(confirmPassword)) {
                        Toast.makeText(this, "New passwords don't match", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Re-authenticate user before changing password
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null && user.getEmail() != null) {
                        user.reauthenticate(EmailAuthProvider.getCredential(user.getEmail(), currentPassword))
                                .addOnSuccessListener(aVoid -> {
                                    // Change password
                                    user.updatePassword(newPassword)
                                            .addOnSuccessListener(aVoid2 -> {
                                                Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                                                // Sign out user after password change
                                                FirebaseAuth.getInstance().signOut();
                                                Intent intent = new Intent(this, LoginActivity.class);
                                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                startActivity(intent);
                                            })
                                            .addOnFailureListener(e -> 
                                                Toast.makeText(this, "Failed to update password: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                            );
                                })
                                .addOnFailureListener(e -> 
                                    Toast.makeText(this, "Current password is incorrect", Toast.LENGTH_SHORT).show()
                                );
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteAccountDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        // First delete user data from Firestore
                        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    // Then delete the user account
                                    user.delete()
                                            .addOnSuccessListener(aVoid2 -> {
                                                Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show();
                                                Intent intent = new Intent(this, LoginActivity.class);
                                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                startActivity(intent);
                                            })
                                            .addOnFailureListener(e -> {
                                                if (e instanceof FirebaseAuthRecentLoginRequiredException) {
                                                    // Show re-authentication dialog
                                                    showReAuthenticationDialog();
                                                } else {
                                                    Toast.makeText(this, "Failed to delete account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                })
                                .addOnFailureListener(e -> 
                                    Toast.makeText(this, "Failed to delete user data: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                );
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showReAuthenticationDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_reauthenticate, null);
        TextInputEditText passwordInput = dialogView.findViewById(R.id.passwordInput);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Confirm Password")
                .setMessage("Please enter your password to continue")
                .setView(dialogView)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    String password = passwordInput.getText().toString();
                    if (password.isEmpty()) {
                        Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null && user.getEmail() != null) {
                        user.reauthenticate(EmailAuthProvider.getCredential(user.getEmail(), password))
                                .addOnSuccessListener(aVoid -> showDeleteAccountDialog())
                                .addOnFailureListener(e -> 
                                    Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show()
                                );
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}

