package com.example.anew;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.view.View;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import android.widget.TextView;

public class FavoritesActivity extends AppCompatActivity {
    private RecyclerView resultsContainer;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.your_color));
        }
        setContentView(R.layout.activity_favorites);

        // Initialize views
        resultsContainer = findViewById(R.id.favoritesRecyclerView);
        resultsContainer.setLayoutManager(new GridLayoutManager(this, 2));
        emptyText = findViewById(R.id.emptyText);
        
        // Set up SwipeRefreshLayout
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        
        // Configure SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Check if RecyclerView is at the top
            if (!resultsContainer.canScrollVertically(-1)) {
                Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show();
                loadFavoritePlaces();
                new Handler(Looper.getMainLooper()).postDelayed(
                    () -> swipeRefreshLayout.setRefreshing(false),
                    1000
                );
            } else {
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        // Set up navigation buttons
        setupNavigationButtons();

        // Load favorites
        loadFavoritePlaces();
    }

    private void loadFavoritePlaces() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please sign in to view favorites", Toast.LENGTH_SHORT).show();
            emptyText.setVisibility(View.VISIBLE);
            resultsContainer.setVisibility(View.GONE);
            return;
        }

        String userId = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(userId).collection("favorites")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Place> places = new ArrayList<>();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    String placeId = document.getId();
                    String name = document.getString("name");
                    String imageUrl = document.getString("imageUrl");
                    double lat = document.getDouble("lat");
                    double lng = document.getDouble("lng");

                    places.add(new Place(placeId, name, imageUrl, lat, lng));
                }

                if (places.isEmpty()) {
                    emptyText.setVisibility(View.VISIBLE);
                    resultsContainer.setVisibility(View.GONE);
                } else {
                    emptyText.setVisibility(View.GONE);
                    resultsContainer.setVisibility(View.VISIBLE);
                    PlaceAdapter adapter = new PlaceAdapter(this, places);
                    resultsContainer.setAdapter(adapter);
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to load favorites", Toast.LENGTH_SHORT).show();
                emptyText.setVisibility(View.VISIBLE);
                resultsContainer.setVisibility(View.GONE);
            });
    }

    private void setupNavigationButtons() {
        // Set up bottom navigation buttons
        ImageButton profileButton = findViewById(R.id.profileButton);
        ImageButton discoverButton = findViewById(R.id.discoverButton);
        ImageButton favoritesButton = findViewById(R.id.favoritesButton);

        // Set up click listeners for bottom navigation
        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(FavoritesActivity.this, ProfileActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });

        discoverButton.setOnClickListener(v -> {
            Intent intent = new Intent(FavoritesActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });

        favoritesButton.setOnClickListener(v ->
            Toast.makeText(FavoritesActivity.this, "You are already in Favorites.", Toast.LENGTH_SHORT).show()
        );
    }
} 