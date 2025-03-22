package com.example.anew;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;


public class MainActivity extends AppCompatActivity {


    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int MAP_REQUEST_CODE = 100; // Request code for the map activity
    private FusedLocationProviderClient fusedLocationProviderClient;
    private TextView resultView;
    private TextView recommendedText;
    private Spinner fromWhereSpinner;
    private Spinner categorySpinner;
    private EditText searchBar;
    private Button searchButton;
    private Location userLocation;


    private LinearLayout resultsContainer;


    private CordinatesFinderChurches cordinatesFinderChurches = new CordinatesFinderChurches();
    private CordinatesFinderMuseums cordinatesFinderMuseums = new CordinatesFinderMuseums();
    private CordinatesFinderArtGalleries cordinatesFinderArtGalleries = new CordinatesFinderArtGalleries();
    private CordinatesFinderParks cordinatesFinderParks = new CordinatesFinderParks();
    private CoordinatesFinderLibraries coordinatesFinderLibraries = new CoordinatesFinderLibraries();
    private CordinatesFinderFood cordinatesFinderFood = new CordinatesFinderFood();
    private String apiKey = "AIzaSyDfylRP2UhEe-kcDiigAiECbCqL1HAJ3I4";


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
        resultsContainer = findViewById(R.id.resultsContainer);


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
            searchBar.setEnabled(true);
            resultsContainer.removeAllViews();
            if (userLocation != null) {
                String fromWhere = fromWhereSpinner.getSelectedItem().toString().toLowerCase();
                int radius = searchBar.getText().toString().isEmpty() ? 5 : Integer.parseInt(searchBar.getText().toString());
                String selectedCategory = categorySpinner.getSelectedItem().toString().toLowerCase();


                if (fromWhere.equals("from my place")) {
                    double userLatitude = userLocation.getLatitude();
                    double userLongitude = userLocation.getLongitude();
                    callSearchFunction(selectedCategory, userLatitude, userLongitude, radius);
                } else {
                    // Open Map Activity to choose a location
                    Intent mapIntent = new Intent(MainActivity.this, Map.class);
                    startActivityForResult(mapIntent, MAP_REQUEST_CODE);
                }
            } else {
                Toast.makeText(MainActivity.this, "Location not available. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
        ImageButton profileButton = findViewById(R.id.profileButton);
        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });
        ImageButton discoverButton = findViewById(R.id.discoverButton);
        discoverButton.setOnClickListener(v ->
                Toast.makeText(MainActivity.this, "You are already in your profile.", Toast.LENGTH_SHORT).show()
        );


        getLocation();

        loadRecommendedPlaces();
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
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == MAP_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                double userLatitude = data.getDoubleExtra("latitude", 0.0);
                double userLongitude = data.getDoubleExtra("longitude", 0.0);
                int radius = searchBar.getText().toString().isEmpty() ? 5 : Integer.parseInt(searchBar.getText().toString());
                String selectedCategory = categorySpinner.getSelectedItem().toString().toLowerCase();


                callSearchFunction(selectedCategory, userLatitude, userLongitude, radius);
            }
        }
    }


    private void callSearchFunction(String category, double latitude, double longitude, int radius) {
        switch (category) {
            case "churches":
                cordinatesFinderChurches.getChurchCoordinates(latitude, longitude, radius, apiKey, resultView, resultsContainer);
                break;
            case "museums":
                cordinatesFinderMuseums.getMuseumCoordinates(latitude, longitude, radius, apiKey, resultView, resultsContainer);
                break;
            case "art galleries":
                cordinatesFinderArtGalleries.getArtGalleryCoordinates(latitude, longitude, radius,apiKey, resultView, resultsContainer);
                break;
            case "parks":
                cordinatesFinderParks.getParkCoordinates(latitude, longitude, radius,apiKey, resultView, resultsContainer);
                break;


            case "library":
                coordinatesFinderLibraries.getLibraryCoordinates(latitude, longitude, radius,apiKey, resultView, resultsContainer);
                break;
        }
    }
    private void loadRecommendedPlaces() {
        resultsContainer.removeAllViews(); // Clear previous recommendations


        String[] recommendedPlaces = {"Best Museums", "Famous Parks", "Top Libraries","Churches","FastFood"};


        for (String place : recommendedPlaces) {
            Button placeButton = new Button(this);
            placeButton.setText(place);
            placeButton.setOnClickListener(v -> {
                recommendedText.setText("Result");
                searchBar.setEnabled(false);
                searchBar.setEnabled(true);
                resultsContainer.removeAllViews();
                performRecommendedSearch(place);
            });


            resultsContainer.addView(placeButton);
        }


    }


    // 🔍 Perform search when user clicks on a recommended button
    private void performRecommendedSearch(String placeType) {
        if (userLocation != null) {
            double userLatitude = userLocation.getLatitude();
            double userLongitude = userLocation.getLongitude();
            int radius = 10; // Default radius for recommendations


            switch (placeType) {
                case "Best Museums":
                    cordinatesFinderMuseums.getMuseumCoordinates(userLatitude, userLongitude, radius, apiKey, resultView, resultsContainer);
                    break;
                case "Famous Parks":
                    cordinatesFinderParks.getParkCoordinates(userLatitude, userLongitude, radius, apiKey, resultView, resultsContainer);
                    break;
                case "Top Libraries":
                    coordinatesFinderLibraries.getLibraryCoordinates(userLatitude, userLongitude, radius, apiKey, resultView, resultsContainer);
                    break;
                case "Churches":
                    cordinatesFinderChurches.getChurchCoordinates(userLatitude, userLongitude, radius, apiKey, resultView, resultsContainer);
                    break;
                case "FastFood":
                    cordinatesFinderFood.getFoodCoordinates(userLatitude, userLongitude, radius, apiKey, resultView, resultsContainer);
                    break;
            }
        } else {
            Toast.makeText(this, "Location not available. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }
}
