package com.example.anew;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private TextView resultView;
    private TextView recommendedText;
    private GoogleMap googleMap;

    private Spinner fromWhereSpinner;
    private Spinner categorySpinner;
    private EditText searchBar;
    private Button searchButton;
    private Location userLocation;
    String apiKey = "";

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

        resultView = findViewById(R.id.text_location);
        searchBar = findViewById(R.id.searchBar);
        searchButton = findViewById(R.id.searchButton);
        fromWhereSpinner = findViewById(R.id.numberSpinner);
        categorySpinner = findViewById(R.id.categorySpinner);
        recommendedText = findViewById(R.id.recommendedText);
        LinearLayout resultsContainer = findViewById(R.id.resultsContainer);

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
            recommendedText.setText("Result");
            searchBar.setEnabled(false);
            if (userLocation != null) {
                String fromWhere = fromWhereSpinner.getSelectedItem().toString().toLowerCase();
                double userLatitude = userLocation.getLatitude();
                double userLongitude = userLocation.getLongitude();
                int radius = searchBar.getText().toString().isEmpty() ? 5 : Integer.parseInt(searchBar.getText().toString());
                String selectedCategory = categorySpinner.getSelectedItem().toString().toLowerCase();

                CordinatesFinderChurches cordinatesFinderChurches=new CordinatesFinderChurches();
                CordinatesFinderMuseums cordinatesFinderMuseums=new CordinatesFinderMuseums();
                CordinatesFinderArtGalleries cordinatesFinderArtGalleries=new CordinatesFinderArtGalleries();
                CordinatesFinderParks cordinatesFinderParks=new CordinatesFinderParks();
                if (fromWhere.equals("from my place")) {
                    switch (selectedCategory){

                        case "churches":
                            cordinatesFinderChurches.getChurchCoordinates(userLatitude, userLongitude, radius,apiKey,resultView,resultsContainer);
                            break;
                        case "museums":
                            cordinatesFinderMuseums.getMuseumCoordinates(userLatitude,userLongitude,radius,apiKey,resultView,resultsContainer);
                            break;
                        case "art galleries":
                            cordinatesFinderArtGalleries.getArtGalleryCoordinates(userLatitude,userLongitude,radius,resultView,resultsContainer);
                            break;
                        case "parks":
                            //cordinatesFinderParks.getParkCoordinatesAndDistance(userLatitude,userLongitude,radius,resultView);
                            break;

                    }

                } else {
                    double userLatitudeChoosen = 0;
                    double userLongitudeChoosen = 0;
                    cordinatesFinderChurches.getChurchCoordinates(userLatitudeChoosen, userLongitudeChoosen, radius,apiKey,resultView,resultsContainer);
                }
            } else {

                Toast.makeText(MainActivity.this, "Location not available. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });

        ImageButton discoverButton = findViewById(R.id.discoverButton);
        discoverButton.setOnClickListener(v -> {});

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
                    .addOnSuccessListener(this, location -> userLocation = location);
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
