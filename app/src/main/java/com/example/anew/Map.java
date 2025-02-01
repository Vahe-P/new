package com.example.anew;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;


public class Map extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng selectedLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Set up the map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Set up the "GO" button
        Button goButton = findViewById(R.id.go_button);
        goButton.setOnClickListener(view -> {
            if (selectedLocation != null) {
                // Send the coordinates to the previous activity or handle as needed
                Intent resultIntent = new Intent();
                resultIntent.putExtra("latitude", selectedLocation.latitude);
                resultIntent.putExtra("longitude", selectedLocation.longitude);
                setResult(RESULT_OK, resultIntent);
                finish();  // Close the MapActivity and return to the previous activity
            } else {
                Toast.makeText(this, "Please select a location on the map.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Set a default location (e.g., center of the world or some default location)
        LatLng defaultLocation = new LatLng(40.7128, -74.0060);  // Example: New York City
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10));

        // Set up a map click listener to choose a place
        mMap.setOnMapClickListener(latLng -> {
            // Clear any existing markers
            mMap.clear();
            // Add a marker where the user clicked
            mMap.addMarker(new MarkerOptions().position(latLng).title("Selected Location"));
            // Save the selected location coordinates
            selectedLocation = latLng;
            if (selectedLocation != null) {
                // Send the coordinates to MainActivity via Intent
                Intent resultIntent = new Intent();
                resultIntent.putExtra("latitude", selectedLocation.latitude);
                resultIntent.putExtra("longitude", selectedLocation.longitude);
                setResult(RESULT_OK, resultIntent);
                finish();  // Close the MapActivity and return to MainActivity
            } else {
                Toast.makeText(this, "Please select a location on the map.", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
