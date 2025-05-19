package com.example.anew;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.List;

public class PostsActivity extends AppCompatActivity {
    private RecyclerView postsRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton fabCreatePost;
    private ImageButton btnRefresh;
    private PostsAdapter postsAdapter;
    private List<Post> posts;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.your_color));
        }
        setContentView(R.layout.activity_posts);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();

        // Initialize views
        postsRecyclerView = findViewById(R.id.postsRecyclerView);
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        fabCreatePost = findViewById(R.id.fabCreatePost);
        btnRefresh = findViewById(R.id.btn_refresh);

        // Set up RecyclerView
        posts = new ArrayList<>();
        postsAdapter = new PostsAdapter(this, posts);
        postsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        postsRecyclerView.setAdapter(postsAdapter);

        // Set up SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this::loadPosts);

        // Set up FAB
        fabCreatePost.setOnClickListener(v -> {
            if (auth.getCurrentUser() != null) {
                startActivity(new Intent(PostsActivity.this, CreatePostActivity.class));
            } else {
                Toast.makeText(this, "Please sign in to create posts", Toast.LENGTH_SHORT).show();
            }
        });

        // Set up refresh button
        btnRefresh.setOnClickListener(v -> loadPosts());

        // Set up bottom navigation
        setupBottomNavigation();

        // Load posts
        loadPosts();
    }

    private void loadPosts() {
        swipeRefreshLayout.setRefreshing(true);
        db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                posts.clear();
                queryDocumentSnapshots.forEach(doc -> {
                    Post post = doc.toObject(Post.class);
                    post.setId(doc.getId());
                    posts.add(post);
                });
                postsAdapter.notifyDataSetChanged();
                swipeRefreshLayout.setRefreshing(false);
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Error loading posts", Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
            });
    }

    private void setupBottomNavigation() {
        ImageButton profileButton = findViewById(R.id.profileButton);
        ImageButton discoverButton = findViewById(R.id.discoverButton);
        ImageButton postsButton = findViewById(R.id.postsButton);
        ImageButton favoritesButton = findViewById(R.id.favoritesButton);

        profileButton.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
        });

        discoverButton.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        postsButton.setOnClickListener(v -> {
            // Already in PostsActivity
            Toast.makeText(this, "Already in PostsActivity", Toast.LENGTH_SHORT).show();
        });

        favoritesButton.setOnClickListener(v -> {
            startActivity(new Intent(this, FavoritesActivity.class));
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPosts();
    }
} 