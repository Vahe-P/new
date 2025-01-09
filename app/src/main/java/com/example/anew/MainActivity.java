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

        boolean isGuest = getIntent().getBooleanExtra("isGuest", false);
        boolean fromProfile = getIntent().getBooleanExtra("fromProfile", false);
        if (!isGuest && FirebaseAuth.getInstance().getCurrentUser() == null && !fromProfile) {
            Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(loginIntent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        textLocation = findViewById(R.id.text_location);
        searchBar = findViewById(R.id.searchBar);
        searchButton = findViewById(R.id.searchButton);
        fromWhereSpinner = findViewById(R.id.numberSpinner);
        categorySpinner = findViewById(R.id.categorySpinner);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        ArrayAdapter<CharSequence> numberAdapter = ArrayAdapter.createFromResource(this,
                R.array.fromWhereArr, android.R.layout.simple_spinner_item);
        numberAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fromWhereSpinner.setAdapter(numberAdapter);

        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(this,
                R.array.category_array, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        searchButton.setOnClickListener(v -> {
            String searchValue = searchBar.getText().toString().trim();
            int radius = searchValue.isEmpty() ? 5 : Integer.parseInt(searchValue); // Default to 5 if empty
            String fromWhereChoice = fromWhereSpinner.getSelectedItem().toString();
            double userLongitude = userLocation != null ? userLocation.getLongitude() : 0;
            double userLatitude = userLocation != null ? userLocation.getLatitude() : 0;
        });

        ImageButton discoverButton = findViewById(R.id.discoverButton);
        discoverButton.setOnClickListener(v -> {

        });

        ImageButton profileButton = findViewById(R.id.profileButton);
        profileButton.setOnClickListener(v -> {
            Intent profileIntent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(profileIntent);
        });

        getLocation();
    }

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
                            userLocation = location;
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
            }
        }
    }
}
