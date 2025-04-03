package com.example.anew;


import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class ProfileActivity extends AppCompatActivity {


    private TextView userEmailTextView;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private Spinner fromWhereSpinner;
    private EditText searchBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);


        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();


        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Profile");
        }


        userEmailTextView = findViewById(R.id.userEmailTextView);
        fromWhereSpinner = findViewById(R.id.fromWhere);
        searchBar = findViewById(R.id.searchBar);


        ArrayAdapter<CharSequence> numberAdapter = ArrayAdapter.createFromResource(this,
                R.array.fromWhereArr, android.R.layout.simple_spinner_item);
        numberAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fromWhereSpinner.setAdapter(numberAdapter);
        // Load previously saved data (if any)
        loadSavedData();


        if (currentUser != null) {
            userEmailTextView.setText(currentUser.getEmail());
        } else {
            userEmailTextView.setText("No email available");
        }


        ImageButton discoverButton = findViewById(R.id.discoverButton);
        discoverButton.setOnClickListener(v -> {
            // Get the values from the profile and send to MainActivity
            String selectedFromWhere = fromWhereSpinner.getSelectedItem() != null ?
                    fromWhereSpinner.getSelectedItem().toString() : "from my place";


            String inputDistance = searchBar.getText().toString();


            saveData(selectedFromWhere, inputDistance);
            Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
            intent.putExtra("fromProfile", true);
            intent.putExtra("selectedFromWhere", selectedFromWhere);
            intent.putExtra("inputDistance", inputDistance);
            startActivity(intent);
        });




        ImageButton profileButton = findViewById(R.id.profileButton);
        profileButton.setOnClickListener(v ->
                Toast.makeText(ProfileActivity.this, "You are already in your profile.", Toast.LENGTH_SHORT).show()
        );


        Button logoutButton = findViewById(R.id.btn_logout);
        logoutButton.setOnClickListener(v -> showLogoutConfirmationDialog());
    }


    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
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
                    finish(); // Close ProfileActivity
                })
                .setNegativeButton("No", null)
                .show();
    }


    // Save the selected values in SharedPreferences
    private void saveData(String fromWhere, String inputDistance) {
        SharedPreferences sharedPreferences = getSharedPreferences("ProfilePrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("fromWhere", fromWhere);
        editor.putString("inputDistance", inputDistance);
        editor.apply();
    }


    // Load saved data from SharedPreferences
    private void loadSavedData() {
        SharedPreferences sharedPreferences = getSharedPreferences("ProfilePrefs", MODE_PRIVATE);
        String savedFromWhere = sharedPreferences.getString("fromWhere", "");
        String savedInputDistance = sharedPreferences.getString("inputDistance", "");


        // Set the spinner value
        if (!savedFromWhere.isEmpty()) {
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) fromWhereSpinner.getAdapter();
            int spinnerPosition = adapter.getPosition(savedFromWhere);
            fromWhereSpinner.setSelection(spinnerPosition);
        }


        // Set the EditText value
        searchBar.setText(savedInputDistance);
    }
}

