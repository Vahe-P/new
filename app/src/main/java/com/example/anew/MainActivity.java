package com.example.anew;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private TextView textLocation;

    private Spinner fromWhereSpinner;
    private Spinner categorySpinner;
    private EditText searchBar;
    private Button searchButton;
    private Location userLocation;

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

        // Initialize UI elements for location and spinners
        textLocation = findViewById(R.id.text_location);
        searchBar = findViewById(R.id.searchBar);
        searchButton = findViewById(R.id.searchButton);
        fromWhereSpinner = findViewById(R.id.numberSpinner);
        categorySpinner = findViewById(R.id.categorySpinner);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Set up the spinners
        ArrayAdapter<CharSequence> numberAdapter = ArrayAdapter.createFromResource(this,
                R.array.fromWhereArr, android.R.layout.simple_spinner_item);
        numberAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fromWhereSpinner.setAdapter(numberAdapter);

        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(this,
                R.array.category_array, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        // Search Button click listener
        searchButton.setOnClickListener(v -> {
            String searchValue = searchBar.getText().toString().trim();
            int radius = searchValue.isEmpty() ? 5 : Integer.parseInt(searchValue); // Default to 5 if empty
            String fromWhereChoice = fromWhereSpinner.getSelectedItem().toString();

            if ("From My Place".equals(fromWhereChoice)) {
                // Get current location if "From My Place" is chosen
                if (userLocation != null) {
                    String result = "Category: " + categorySpinner.getSelectedItem()+"\nRadius: " + radius + " km\nLocation: Latitude: " + userLocation.getLatitude()
                            + "\nLongitude: " + userLocation.getLongitude();
                    textLocation.setText(result);
                } else {
                    textLocation.setText("Location not available");
                }
            } else {
                // Handle other cases when "From My Place" is not selected
                textLocation.setText("Radius: " + radius + " km\n Category: " + categorySpinner.getSelectedItem());
            }
        });

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

        // Get the user's current location if needed
        getLocation();
    }

    // Method to get the user's current location
    private void getLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                userLocation = location;
                                textLocation.setText("Location: Latitude: " + location.getLatitude()
                                        + ", Longitude: " + location.getLongitude());
                            } else {
                                textLocation.setText("Location not available");
                            }
                        }
                    });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            } else {
                textLocation.setText("Permission denied");
            }
        }
    }
}
