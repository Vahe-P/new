package com.example.anew;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int MAP_REQUEST_CODE = 100;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Button searchButton;
    private Location userLocation;
    private RecyclerView resultsContainer;
    private ImageView errorImageView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView recommendedText;

    private CordinatesFinderChurches cordinatesFinderChurches = new CordinatesFinderChurches();
    private String apiKey = "AIzaSyD3aOclf9YRAKK9D0VfQPp0NLsGDCJ9xFU";
    private Button btn;
    private int totalCategories = 0;
    private int foundResults = 0;
    private static final int REQUEST_CODE_NOTIFICATIONS = 101;
    private static final String CHANNEL_ID = "hi_channel";
    private Handler handler = new Handler();
    private Runnable notifyRunnable;
    private GestureDetector gestureDetector;
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.your_color));
        }

        boolean isGuest = getIntent().getBooleanExtra("isGuest", false);
        boolean fromProfile = getIntent().getBooleanExtra("fromProfile", false);
        Log.d("DEBUG", "fromProfile: " + fromProfile);

        if (!isGuest && FirebaseAuth.getInstance().getCurrentUser() == null && !fromProfile) {
            Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(loginIntent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        searchButton = findViewById(R.id.searchButton);
        recommendedText = findViewById(R.id.recommendedText);
        resultsContainer = findViewById(R.id.resultsRecyclerView);
        resultsContainer.setLayoutManager(new GridLayoutManager(this, 2));
        errorImageView = findViewById(R.id.errorImageView);
        btn = findViewById(R.id.btn);

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (!resultsContainer.canScrollVertically(-1)) {
                List<String> selectedCategories = getSelectedCategories();
                if (resultsContainer.getAdapter() != null) {
                    ((PlaceAdapter_2) resultsContainer.getAdapter()).clearData();
                }
                cordinatesFinderChurches.clearResults();

                if (selectedCategories.isEmpty()) {
                    loadRecommendedPlaces();
                } else {
                    searchButton.performClick();
                }
                new Handler(Looper.getMainLooper()).postDelayed(
                    () -> swipeRefreshLayout.setRefreshing(false),
                    1000
                );
            } else {
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        getLocation();
        ImageView compassImage = findViewById(R.id.compassImage);
        if (compassImage == null) {
            Log.e("DEBUgggG", "compassImage is null. Check if the ID in XML matches the ID in the code.");
        } else {
            Log.d("DEBUgggG", "compassImage is initialized correctly.");
        }

        getLocation();
        compassImage.setOnClickListener(v -> {
            if(isNetworkAvailable(this) || userLocation != null){
                List<String> selectedCategories = getSelectedCategories();

                if (selectedCategories.isEmpty()) {
                    loadRecommendedPlaces();
                } else {
                    searchButton.performClick();
                }
                getLocation();
            }
        });
        if (!isNetworkAvailable(this) || userLocation == null) {
            showRotatingCompass(compassImage);

            if (!isNetworkAvailable(this)) {
                Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            }

            if (userLocation == null) {
                Toast.makeText(this, "Location is not available", Toast.LENGTH_SHORT).show();
            }
        }

        if (!isNetworkAvailable(this) || userLocation == null) {
            RotateAnimation rotate = new RotateAnimation(
                    0f, 360f,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f
            );
            rotate.setDuration(2000);
            rotate.setRepeatCount(Animation.INFINITE);
            compassImage.startAnimation(rotate);
            compassImage.setVisibility(View.VISIBLE);
            if(!isNetworkAvailable(this) ){
                Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            }
            if (userLocation == null){
                Toast.makeText(this, "Location is not available", Toast.LENGTH_SHORT).show();
            }
        } if(isNetworkAvailable(this) && userLocation != null) {
            compassImage.clearAnimation();
            compassImage.setVisibility(View.GONE);
        }
        if(userLocation==null){
            Toast.makeText(this, "Location isjkdsfvndjnkfvj not available", Toast.LENGTH_SHORT).show();
            showRotatingCompass(compassImage);
        }

        searchButton.setOnClickListener(v -> {
            resultsContainer.removeAllViews();
            SharedPreferences sharedPreferences = getSharedPreferences("ProfilePrefs", MODE_PRIVATE);
            String fromWhere = sharedPreferences.getString("fromWhere", "From my place");
            int radius = sharedPreferences.getString("inputDistance", "").isEmpty()
                    ? 5
                    : Integer.parseInt(sharedPreferences.getString("inputDistance", ""));

            btn.setVisibility(View.GONE);
            if (resultsContainer.getAdapter() != null) {
                ((PlaceAdapter_2) resultsContainer.getAdapter()).clearData();
            }
            cordinatesFinderChurches.clearResults();

            if(userLocation==null){
                showRotatingCompass(compassImage);
            }else{
                hideRotatingCompass(compassImage);
            }
            if (!isNetworkAvailable(this) || userLocation==null) {
                RotateAnimation rotate = new RotateAnimation(
                        0f, 360f,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f
                );
                rotate.setDuration(2000);
                rotate.setRepeatCount(Animation.INFINITE);
                compassImage.startAnimation(rotate);
                compassImage.setVisibility(View.VISIBLE);
                Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            } else {
                compassImage.clearAnimation();
                compassImage.setVisibility(View.GONE);
            }
            recommendedText.setText("Result");
            errorImageView.setVisibility(View.GONE);

            Log.d("SEARCH", "fromWhere value: " + fromWhere);

            if (userLocation != null) {
                List<String> selectedCategories = getSelectedCategories();
                totalCategories = selectedCategories.size();
                foundResults = 0;

                if (!selectedCategories.isEmpty()) {
                    double userLatitude = userLocation.getLatitude();
                    double userLongitude = userLocation.getLongitude();
                    
                    if (fromWhere.equals("From my place")) {
                        for (String category : selectedCategories) {
                            callSearchFunction(category, userLatitude, userLongitude, radius);
                        }
                    } else {
                        Intent mapIntent = new Intent(MainActivity.this, KartaActivity.class);
                        startActivityForResult(mapIntent, MAP_REQUEST_CODE);
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Please select at least one category.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MainActivity.this, "Location not available. Please try again.", Toast.LENGTH_SHORT).show();
            }
            if(fromWhere.equals("From my place")){
                startCountdownAndCheckGrid();
                new Handler(Looper.getMainLooper()).postDelayed(this::checkGridLayoutEmpty, 5000);
            }
        });

        ImageButton profileButton = findViewById(R.id.profileButton);
        profileButton.setOnClickListener(v -> {
            SharedPreferences sharedPreferences = getSharedPreferences("ProfilePrefs", MODE_PRIVATE);

            String fromWhere = sharedPreferences.getString("fromWhere", "from my place");
            int radius = sharedPreferences.getString("inputDistance", "").isEmpty()
                    ? 5
                    : Integer.parseInt(sharedPreferences.getString("inputDistance", ""));
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            intent.putExtra("selectedFromWhere", fromWhere);
            intent.putExtra("inputDistance", String.valueOf(radius));
            startActivity(intent);
        });

        ImageButton discoverButton = findViewById(R.id.discoverButton);
        discoverButton.setOnClickListener(v ->
                Toast.makeText(MainActivity.this, "You are already in Main.", Toast.LENGTH_SHORT).show()
        );

        ImageButton favoritesButton = findViewById(R.id.favoritesButton);
        favoritesButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FavoritesActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });

        btn.setOnClickListener(v ->{
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        loadRecommendedPlaces();
        changeCheckboxColor();
        setupCheckboxListeners();
        updateSearchButtonState();
        requestNotificationPermission();

        gestureDetector = new GestureDetector(this, new SwipeGestureListener());

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnTouchListener((v, event) -> {
                gestureDetector.onTouchEvent(event);
                return false;
            });
        }
    }
    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
    private void showRotatingCompass(ImageView compassImage) {
        Toast.makeText(this, "Location is notpppppppppp available", Toast.LENGTH_SHORT).show();

        RotateAnimation rotate = new RotateAnimation(
                0f, 360f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        rotate.setDuration(2000);
        rotate.setRepeatCount(Animation.INFINITE);
        compassImage.setVisibility(View.VISIBLE);
        compassImage.startAnimation(rotate);
    }

    private void hideRotatingCompass(ImageView compassImage) {
        compassImage.clearAnimation();
        compassImage.setVisibility(View.GONE);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {

                Log.d("MainActivity", "Requesting POST_NOTIFICATIONS permission");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_CODE_NOTIFICATIONS);
            } else {
                Log.d("MainActivity", "Permission already granted");
                scheduleNotificationWorker();
            }
        } else {
            Log.d("MainActivity", "Permission not required (API < 33)");
            scheduleNotificationWorker();
        }
    }

    private void scheduleNotificationWorker() {
        Log.d("MainActivity", "Scheduling worker");

        PeriodicWorkRequest workRequest =
                new PeriodicWorkRequest.Builder(HiWorker.class, 15, TimeUnit.MINUTES)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "HiNotificationWork",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Permission granted by user");
                scheduleNotificationWorker();
            } else {
                Log.w("MainActivity", "Permission denied by user");
            }
        }
    }

    private List<String> getSelectedCategories() {
        List<String> selectedCategories = new ArrayList<>();

        Map<Integer, String> checkBoxCategoryMap = new HashMap<>();
        checkBoxCategoryMap.put(R.id.checkChurches, "churches");
        checkBoxCategoryMap.put(R.id.checkMuseums, "museums");
        checkBoxCategoryMap.put(R.id.checkArtGalleries, "art galleries");
        checkBoxCategoryMap.put(R.id.checkParks, "parks");
        checkBoxCategoryMap.put(R.id.checkLibraries, "libraries");
        checkBoxCategoryMap.put(R.id.checkHotels, "hotels");
        checkBoxCategoryMap.put(R.id.checkCinemas, "hospital");
        checkBoxCategoryMap.put(R.id.checkShoppingMalls, "shopping malls");
        checkBoxCategoryMap.put(R.id.checkTheaters, "theaters");
        checkBoxCategoryMap.put(R.id.checkRestaurants, "restaurants");
        checkBoxCategoryMap.put(R.id.checkGas, "gas");
        checkBoxCategoryMap.put(R.id.checkFortress, "fortress");

        for (Map.Entry<Integer, String> entry : checkBoxCategoryMap.entrySet()) {
            CheckBox checkBox = findViewById(entry.getKey());
            if (checkBox != null && checkBox.isChecked()) {
                selectedCategories.add(entry.getValue());
            }
        }

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
                            loadRecommendedPlaces();
                        } else {
                            Log.e("LOCATION", "Failed to get location.");
                            Toast.makeText(this, "Location is not available. Try again later.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("LOCATION", "Error retrieving location: " + e.getMessage());
                    });
        }
    }
    private void startCountdownAndCheckGrid() {
        if (isNetworkAvailable(this)) {
            final ProgressBar loadingSpinner = findViewById(R.id.loadingSpinner);
            loadingSpinner.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MAP_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                double userLatitude = data.getDoubleExtra("latitude", 0.0);
                double userLongitude = data.getDoubleExtra("longitude", 0.0);
                
                SharedPreferences sharedPreferences = getSharedPreferences("ProfilePrefs", MODE_PRIVATE);
                String fromWhere = sharedPreferences.getString("fromWhere", "From my place");
                int radius = sharedPreferences.getString("inputDistance", "").isEmpty()
                        ? 5
                        : Integer.parseInt(sharedPreferences.getString("inputDistance", ""));
                List<String> selectedCategories = getSelectedCategories();
                totalCategories = selectedCategories.size();
                foundResults = 0;

                if (resultsContainer.getAdapter() != null) {
                    ((PlaceAdapter_2) resultsContainer.getAdapter()).clearData();
                }
                cordinatesFinderChurches.clearResults();

                if (!selectedCategories.isEmpty()) {
                    for (String category : selectedCategories) {
                        callSearchFunction(category, userLatitude, userLongitude, radius);
                    }
                } else {
                    Toast.makeText(this, "Please select at least one category.", Toast.LENGTH_SHORT).show();
                }
            }

            startCountdownAndCheckGrid();
        }
    }

    private void callSearchFunction(String category, double latitude, double longitude, int radius) {
        switch (category) {
            case "churches":
                cordinatesFinderChurches.getChurchCoordinates("church", latitude, longitude, radius, apiKey, resultsContainer);
                break;
            case "museums":
                cordinatesFinderChurches.getChurchCoordinates("museum", latitude, longitude, radius, apiKey, resultsContainer);
                break;
            case "art galleries":
                cordinatesFinderChurches.getChurchCoordinates("art_gallery art_museum", latitude, longitude, radius, apiKey, resultsContainer);
                break;
            case "parks":
                cordinatesFinderChurches.getChurchCoordinates("park", latitude, longitude, radius, apiKey, resultsContainer);
                break;
            case "libraries":
                cordinatesFinderChurches.getChurchCoordinates("library", latitude, longitude, radius, apiKey, resultsContainer);
                break;
            case "fastfood":
                cordinatesFinderChurches.getChurchCoordinates("fast+food", latitude, longitude, radius, apiKey, resultsContainer);
                break;
            case "hotels":
                cordinatesFinderChurches.getChurchCoordinates("hotels", latitude, longitude, radius, apiKey, resultsContainer);
                break;
            case "gas":
                cordinatesFinderChurches.getChurchCoordinates("gas", latitude, longitude, radius, apiKey, resultsContainer);
                break;
            case "fortress":
                cordinatesFinderChurches.getChurchCoordinates("fortress", latitude, longitude, radius, apiKey, resultsContainer);
                break;
            case "restaurants":
                cordinatesFinderChurches.getChurchCoordinates("restaurants", latitude, longitude, radius, apiKey, resultsContainer);
                break;
            case "theaters":
                cordinatesFinderChurches.getChurchCoordinates("theaters", latitude, longitude, radius, apiKey, resultsContainer);
                break;
            case "shopping malls":
                cordinatesFinderChurches.getChurchCoordinates("shopping malls", latitude, longitude, radius, apiKey, resultsContainer);
                break;
            case "hospital":
                cordinatesFinderChurches.getChurchCoordinates("hospital", latitude, longitude, radius, apiKey, resultsContainer);
                break;
        }
    }

    private void changeCheckboxColor() {
        CheckBox checkChurches = findViewById(R.id.checkChurches);
        CheckBox checkMuseums = findViewById(R.id.checkMuseums);
        CheckBox checkArtGalleries = findViewById(R.id.checkArtGalleries);
        CheckBox checkParks = findViewById(R.id.checkParks);
        CheckBox checkLibraries = findViewById(R.id.checkLibraries);
        CheckBox checkHotels = findViewById(R.id.checkHotels);
        CheckBox checkCinemas = findViewById(R.id.checkCinemas);
        CheckBox checkMalls = findViewById(R.id.checkShoppingMalls);
        CheckBox checkTheaters = findViewById(R.id.checkTheaters);
        CheckBox checkResturants = findViewById(R.id.checkRestaurants);
        CheckBox checkGas = findViewById(R.id.checkGas);
        CheckBox checkFortress = findViewById(R.id.checkFortress);

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
        if (checkFortress != null) {
            checkFortress.setButtonTintList(ContextCompat.getColorStateList(this, R.color.my_checkbox_color));
        }
        if (checkGas != null) {
            checkGas.setButtonTintList(ContextCompat.getColorStateList(this, R.color.my_checkbox_color));
        }
        if (checkTheaters != null) {
            checkTheaters.setButtonTintList(ContextCompat.getColorStateList(this, R.color.my_checkbox_color));
        }
        if (checkResturants != null) {
            checkResturants.setButtonTintList(ContextCompat.getColorStateList(this, R.color.my_checkbox_color));
        }
        if (checkMalls != null) {
            checkMalls.setButtonTintList(ContextCompat.getColorStateList(this, R.color.my_checkbox_color));
        }
        if (checkCinemas!= null) {
            checkCinemas.setButtonTintList(ContextCompat.getColorStateList(this, R.color.my_checkbox_color));
        }
    }
    private void updateSearchButtonState() {
        CheckBox[] checkBoxes = {
                findViewById(R.id.checkChurches),
                findViewById(R.id.checkMuseums),
                findViewById(R.id.checkArtGalleries),
                findViewById(R.id.checkParks),
                findViewById(R.id.checkLibraries),
                findViewById(R.id.checkHotels),
                findViewById(R.id.checkCinemas),
                findViewById(R.id.checkShoppingMalls),
                findViewById(R.id.checkTheaters),
                findViewById(R.id.checkRestaurants),
                findViewById(R.id.checkGas),
                findViewById(R.id.checkFortress)
        };

        boolean anyChecked = false;
        for (CheckBox checkBox : checkBoxes) {
            if (checkBox.isChecked()) {
                anyChecked = true;
                break;
            }
        }

        if (anyChecked) {
            searchButton.setEnabled(true);
            searchButton.setBackgroundColor(ContextCompat.getColor(this, R.color.your_color));
        } else {
            searchButton.setEnabled(false);
            searchButton.setBackgroundColor(ContextCompat.getColor(this, R.color.unabled_button_color));
        }
    }
    private void setupCheckboxListeners() {
        CheckBox[] checkBoxes = {
                findViewById(R.id.checkChurches),
                findViewById(R.id.checkMuseums),
                findViewById(R.id.checkArtGalleries),
                findViewById(R.id.checkParks),
                findViewById(R.id.checkLibraries),
                findViewById(R.id.checkHotels),
                findViewById(R.id.checkCinemas),
                findViewById(R.id.checkShoppingMalls),
                findViewById(R.id.checkTheaters),
                findViewById(R.id.checkRestaurants),
                findViewById(R.id.checkGas),
                findViewById(R.id.checkFortress)
        };

        View.OnClickListener listener = v -> updateSearchButtonState();

        for (CheckBox checkBox : checkBoxes) {
            checkBox.setOnClickListener(listener);
        }
    }

    private class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            boolean result = false;
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();

                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            onSwipeRight();
                        } else {
                            onSwipeLeft();
                        }
                        result = true;
                    }
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return result;
        }
    }

    private void onSwipeRight() {
        Log.d("SwipeGesture", "Right swipe detected, navigating to FavoritesActivity");
        Intent intent = new Intent(MainActivity.this, FavoritesActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    private void onSwipeLeft() {
        Log.d("SwipeGesture", "Left swipe detected, navigating to ProfileActivity");
        Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void checkGridLayoutEmpty() {
        if (totalCategories == 0 && foundResults == 0) {
            errorImageView.setVisibility(View.VISIBLE);
            btn.setVisibility(View.VISIBLE);
        }
    }
}