package com.example.anew;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
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

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int MAP_REQUEST_CODE = 100; // Request code for the map activity
    private FusedLocationProviderClient fusedLocationProviderClient;
    private TextView resultView;
    private TextView recommendedText;
    private Button searchButton;
    private Location userLocation;

    private LinearLayout resultsContainer;
    private LinearLayout recomContainer;

    private CordinatesFinderChurches cordinatesFinderChurches = new CordinatesFinderChurches();
    private CordinatesFinderMuseums cordinatesFinderMuseums = new CordinatesFinderMuseums();
    private CordinatesFinderArtGalleries cordinatesFinderArtGalleries = new CordinatesFinderArtGalleries();
    private CordinatesFinderParks cordinatesFinderParks = new CordinatesFinderParks();
    private CoordinatesFinderLibraries coordinatesFinderLibraries = new CoordinatesFinderLibraries();
    private CordinatesFinderFood cordinatesFinderFood = new CordinatesFinderFood();
    private CordinatesFinderHotels cordinatesHotels = new CordinatesFinderHotels();
    private String apiKey = "AIzaSyDfylRP2UhEe-kcDiigAiECbCqL1HAJ3I4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.your_color));
        }


        // Check if the user is a guest or logged in
        boolean isGuest = getIntent().getBooleanExtra("isGuest", false);
        boolean fromProfile = getIntent().getBooleanExtra("fromProfile", false);
        Log.d("DEBUG", "fromProfile: " + fromProfile);

        // Redirect to LoginActivity if the user is not logged in and not a guest
        if (!isGuest && FirebaseAuth.getInstance().getCurrentUser() == null && !fromProfile) {
            Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(loginIntent);
            finish();
            return;
        }

        // Set the layout for the activity
        setContentView(R.layout.activity_main);

        // Initialize views
        resultView = findViewById(R.id.text_location);
        searchButton = findViewById(R.id.searchButton);
        recommendedText = findViewById(R.id.recommendedText);
        resultsContainer = findViewById(R.id.resultsContainer);
        recomContainer = findViewById(R.id.recomContainer);

        // Initialize FusedLocationProviderClient
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Retrieve data passed from ProfileActivity
        final String fromWhere = getIntent().getStringExtra("selectedFromWhere") != null
                ? getIntent().getStringExtra("selectedFromWhere")
                : "from my place";

        final int radius = getIntent().getStringExtra("inputDistance") != null &&
                !getIntent().getStringExtra("inputDistance").isEmpty()
                ? Integer.parseInt(getIntent().getStringExtra("inputDistance"))
                : 5;

        Log.d("DEBUG", "Received fromProfile: " + fromProfile + ", fromWhere: " + fromWhere + ", radius: " + radius);

        // Set up the search button click listener
        // Modify search button click listener to check selected checkboxes
        searchButton.setOnClickListener(v -> {
            recommendedText.setText("Result");
            resultsContainer.removeAllViews();
            recomContainer.removeAllViews();

            Log.d("SEARCH", "fromWhere value: " + fromWhere);

            if (userLocation != null) {
                List<String> selectedCategories = getSelectedCategories();

                if (!selectedCategories.isEmpty()) {
                    double userLatitude = userLocation.getLatitude();
                    double userLongitude = userLocation.getLongitude();

                    if (fromWhere.equals("From My Place")) {
                        // Call the search function with the user's location
                        for (String category : selectedCategories) {
                            callSearchFunction(category, userLatitude, userLongitude, radius);
                        }
                    } else {
                        // Open Map Activity to choose a location
                        Intent mapIntent = new Intent(MainActivity.this, Map.class);
                        startActivityForResult(mapIntent, MAP_REQUEST_CODE);
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Please select at least one category.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MainActivity.this, "Location not available. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });

        // Set up the profile button click listener
        ImageButton profileButton = findViewById(R.id.profileButton);
        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            // Pass current values for "fromWhere" and "radius"
            intent.putExtra("selectedFromWhere", fromWhere);
            intent.putExtra("inputDistance", String.valueOf(radius));
            startActivity(intent);
        });

        // Set up the discover button click listener
        ImageButton discoverButton = findViewById(R.id.discoverButton);
        discoverButton.setOnClickListener(v ->
                Toast.makeText(MainActivity.this, "You are already in Main.", Toast.LENGTH_SHORT).show()
        );

        getLocation();

        loadRecommendedPlaces();

        // Change the color of checkboxes
        changeCheckboxColor();
    }

    private List<String> getSelectedCategories() {
        List<String> selectedCategories = new ArrayList<>();

        CheckBox checkChurches = findViewById(R.id.checkChurches);
        CheckBox checkMuseums = findViewById(R.id.checkMuseums);
        CheckBox checkArtGalleries = findViewById(R.id.checkArtGalleries);
        CheckBox checkParks = findViewById(R.id.checkParks);
        CheckBox checkLibraries = findViewById(R.id.checkLibraries);
        CheckBox checkHotels = findViewById(R.id.checkHotels);

        if (checkChurches != null && checkChurches.isChecked()) selectedCategories.add("churches");
        if (checkMuseums != null && checkMuseums.isChecked()) selectedCategories.add("museums");
        if (checkArtGalleries != null && checkArtGalleries.isChecked()) selectedCategories.add("art galleries");
        if (checkParks != null && checkParks.isChecked()) selectedCategories.add("parks");
        if (checkLibraries != null && checkLibraries.isChecked()) selectedCategories.add("libraries");
        if (checkHotels != null && checkHotels.isChecked()) selectedCategories.add("hotels");

        return selectedCategories;
    }

    private void getLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            userLocation = location;
                            // Load recommended places after location is retrieved
                            loadRecommendedPlaces();
                        } else {
                            Log.e("LOCATION", "Failed to get location.");
                            Toast.makeText(this, "Location is not available. Try again later.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("LOCATION", "Error retrieving location: " + e.getMessage());
                        Toast.makeText(this, "Error retrieving location.", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MAP_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                double userLatitude = data.getDoubleExtra("latitude", 0.0);
                double userLongitude = data.getDoubleExtra("longitude", 0.0);

                // Get radius from intent (just like in onCreate)
                String radiusInput = getIntent().getStringExtra("inputDistance");
                int radius = (radiusInput == null || radiusInput.isEmpty()) ? 5 : Integer.parseInt(radiusInput);

                // Get selected categories from checkboxes
                List<String> selectedCategories = getSelectedCategories();

                if (!selectedCategories.isEmpty()) {
                    for (String category : selectedCategories) {
                        callSearchFunction(category, userLatitude, userLongitude, radius);
                    }
                } else {
                    Toast.makeText(this, "Please select at least one category.", Toast.LENGTH_SHORT).show();
                }
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
                cordinatesFinderArtGalleries.getArtGalleryCoordinates(latitude, longitude, radius, apiKey, resultView, resultsContainer);
                break;
            case "parks":
                cordinatesFinderParks.getParkCoordinates(latitude, longitude, radius, apiKey, resultView, resultsContainer);
                break;
            case "libraries":
                coordinatesFinderLibraries.getLibraryCoordinates(latitude, longitude, radius, apiKey, resultView, resultsContainer);
                break;
            case "fastfood":
                cordinatesFinderFood.getFoodCoordinates(latitude, longitude, radius, apiKey, resultView, resultsContainer);
                break;
            case "hotels":
                cordinatesHotels.getHotelCoordinates(latitude, longitude, radius, apiKey, resultView, resultsContainer);
                break;
        }
    }

    private void loadRecommendedPlaces() {
        resultsContainer.removeAllViews();
        recomContainer.removeAllViews();// Clear previous recommendations
        if (userLocation != null) {
            double userLatitude = userLocation.getLatitude();
            double userLongitude = userLocation.getLongitude();
            int radius = 10;
            Log.e("Fast", "HAsav ");

            cordinatesFinderFood.getFoodCoordinates(userLatitude, userLongitude, radius, apiKey, resultView, resultsContainer);

        } else {
            Log.e("Fast", "CHAsav ");
        }
        String[] recommendedPlaces = {"Best Museums", "Famous Parks", "Top Libraries", "Churches"};

        for (String place : recommendedPlaces) {
            Button placeButton = new Button(this);
            placeButton.setText(place);
            placeButton.setOnClickListener(v -> {
                recommendedText.setText("Result");
                resultsContainer.removeAllViews();
                recomContainer.removeAllViews();
                performRecommendedSearch(place);
            });
            recomContainer.addView(placeButton);
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
            }
        } else {
            Toast.makeText(this, "Location not available. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void changeCheckboxColor() {
        CheckBox checkChurches = findViewById(R.id.checkChurches);
        CheckBox checkMuseums = findViewById(R.id.checkMuseums);
        CheckBox checkArtGalleries = findViewById(R.id.checkArtGalleries);
        CheckBox checkParks = findViewById(R.id.checkParks);
        CheckBox checkLibraries = findViewById(R.id.checkLibraries);
        CheckBox checkHotels = findViewById(R.id.checkHotels);

        if (checkChurches != null) {
            checkChurches.setButtonTintList(ContextCompat.getColorStateList(this, R.color.my_checkbox_color));
        }
        if (checkMuseums != null) {
            checkMuseums.setButtonTintList(ContextCompat.getColorStateList(this, R.color.my_checkbox_color));
        }
        if (checkArtGalleries != null) {
            checkArtGalleries.setButtonTintList(ContextCompat.getColorStateList(this, R.color.my_checkbox_color));
        }
        if (checkParks != null) {
            checkParks.setButtonTintList(ContextCompat.getColorStateList(this, R.color.my_checkbox_color));
        }
        if (checkLibraries != null) {
            checkLibraries.setButtonTintList(ContextCompat.getColorStateList(this, R.color.my_checkbox_color));
        }
        if (checkHotels != null) {
            checkHotels.setButtonTintList(ContextCompat.getColorStateList(this, R.color.my_checkbox_color));
        }
    }
}