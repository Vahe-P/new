package com.example.anew;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;


import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import android.Manifest;


public class ProfileActivity extends AppCompatActivity {
    private RecyclerView resultsContainer;

    private TextView userEmailTextView;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private Spinner fromWhereSpinner;
    private EditText searchBar;
    private Button prefer;
    private Switch darkModeSwitch;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.your_color));
        }
        setContentView(R.layout.activity_profile);


        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        resultsContainer = findViewById(R.id.resultsRecyclerView);


        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Profile");
        }


        userEmailTextView = findViewById(R.id.userEmailTextView);
        fromWhereSpinner = findViewById(R.id.fromWhere);
        searchBar = findViewById(R.id.searchBar);
        prefer=findViewById(R.id.preferencesButton);


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
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });




        ImageButton profileButton = findViewById(R.id.profileButton);
        profileButton.setOnClickListener(v ->
                Toast.makeText(ProfileActivity.this, "You are already in your profile.", Toast.LENGTH_SHORT).show()
        );


        ImageButton logoutButton = findViewById(R.id.btn_logout);
        logoutButton.setOnClickListener(v -> showLogoutConfirmationDialog());
        prefer.setOnClickListener(v ->
                startActivity(new Intent(ProfileActivity.this, QuestionnaireActivity.class))
        );

        RecyclerView resultsRecyclerView = findViewById(R.id.resultsRecyclerView);
        resultsRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        InertialScrollView scrollView = findViewById(R.id.inertialScrollView);

// Cast SwipeRefreshLayout to CustomSwipeRefreshLayout
        CustomSwipeRefreshLayout customSwipeRefreshLayout = (CustomSwipeRefreshLayout) swipeRefreshLayout;

// Associate the ScrollView with the CustomSwipeRefreshLayout
        customSwipeRefreshLayout.setAssociatedScrollView(scrollView);

// Set up refresh listener for SwipeRefreshLayout
        customSwipeRefreshLayout.setOnRefreshListener(() -> {
            if (!scrollView.canScrollVertically(-1)) {
                // ScrollView is at the top
                Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show();

                resultsContainer.removeAllViews(); // Clear previous places
                loadFavoritePlaces(resultsRecyclerView); // Reload new data
                customSwipeRefreshLayout.setRefreshing(false); // Hide loading spinner

            }
        });



        // Associate the ScrollView with the SwipeRefreshLayout


        // Set up refresh listener for SwipeRefreshLayout




        loadFavoritePlaces(resultsRecyclerView);

    }


    // Handle permission result

    private void loadFavoritePlaces(RecyclerView recyclerView) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String userId = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(userId).collection("favorites")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Place> places = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String placeId = document.getId(); // Get document ID
                        String name = document.getString("name");
                        String imageUrl = document.getString("imageUrl");
                        double lat = document.getDouble("lat");
                        double lng = document.getDouble("lng");

                        // Add place to the list
                        places.add(new Place(placeId, name, imageUrl, lat, lng));
                    }

                    // Set up the adapter
                    PlaceAdapter adapter = new PlaceAdapter(this, places);
                    recyclerView.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileActivity.this, "Failed to load favorites", Toast.LENGTH_SHORT).show();
                });
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

