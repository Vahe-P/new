package com.example.anew;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.PlaceViewHolder> {

    private Context context;
    private List<Place> places;
    private FusedLocationProviderClient fusedLocationProviderClient; // For user location
    private Location userLocation; // Store user's current location

    public PlaceAdapter(Context context, List<Place> places) {
        this.context = context;
        this.places = places;
        this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);

        // Fetch the user's location
        getUserLocation();
    }

    private void getUserLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Request location permission
            ActivityCompat.requestPermissions((android.app.Activity) context,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
        } else {
            fusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            userLocation = location;
                            Log.d("LOCATION", "User location retrieved: Lat = " + location.getLatitude() + ", Lng = " + location.getLongitude());
                        } else {
                            Log.e("LOCATION", "Failed to get location.");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("LOCATION", "Error retrieving location: " + e.getMessage());
                    });
        }
    }

    @NonNull
    @Override
    public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.activity_place_adapter, parent, false);
        return new PlaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceViewHolder holder, int position) {
        Place place = places.get(position);

        // Set random background transparency
        holder.containerLayout.setBackgroundColor(Color.TRANSPARENT);

        // Load Image with Volley
        if (place.getImageUrl() != null) {
            RequestQueue queue = Volley.newRequestQueue(context);
            ImageRequest imageRequest = new ImageRequest(place.getImageUrl(),
                    response -> holder.placeImage.setImageDrawable(new BitmapDrawable(context.getResources(), response)),
                    0, 0, null, null,
                    error -> Log.e("ImageLoadError", "Error loading image: " + error.getMessage()));
            queue.add(imageRequest);
        } else {
            holder.placeImage.setImageResource(R.drawable.download);
        }

        // Set place name
        holder.placeName.setText(place.getName());
        if (place.getName().length() > 20) {
            holder.placeName.setTextSize(14);
        } else if (place.getName().length() > 30) {
            holder.placeName.setTextSize(12);
        }

        // Handle star button for favorites
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DocumentReference favoriteRef = db.collection("users").document(userId).collection("favorites").document(place.getId());
            final boolean[] isFavorite = {false};

            favoriteRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    holder.starButton.setImageResource(R.drawable.ic_star_filled);
                    isFavorite[0] = true;
                } else {
                    holder.starButton.setImageResource(R.drawable.ic_star_empty);
                    isFavorite[0] = false;
                }
            });

            holder.starButton.setOnClickListener(v -> {
                isFavorite[0] = !isFavorite[0];
                if (isFavorite[0]) {
                    Map<String, Object> favoritePlace = new HashMap<>();
                    favoritePlace.put("name", place.getName());
                    favoritePlace.put("lat", place.getLat());
                    favoritePlace.put("lng", place.getLng());
                    favoritePlace.put("imageUrl", place.getImageUrl());

                    favoriteRef.set(favoritePlace)
                            .addOnSuccessListener(aVoid -> {
                                holder.starButton.setImageResource(R.drawable.ic_star_filled);
                                Toast.makeText(context, "Added to favorites", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                isFavorite[0] = false;
                                holder.starButton.setImageResource(R.drawable.ic_star_empty);
                                Toast.makeText(context, "Failed to add", Toast.LENGTH_SHORT).show();
                            });
                } else {
                    favoriteRef.delete()
                            .addOnSuccessListener(aVoid -> {
                                holder.starButton.setImageResource(R.drawable.ic_star_empty);
                                Toast.makeText(context, "Removed from favorites", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                isFavorite[0] = true;
                                holder.starButton.setImageResource(R.drawable.ic_star_filled);
                                Toast.makeText(context, "Failed to remove", Toast.LENGTH_SHORT).show();
                            });
                }
            });
        }

        // Handle share button
        holder.shareButton.setOnClickListener(v -> {
            String googleMapsUrl = "https://www.google.com/maps/search/?api=1&query=" + place.getLat() + "," + place.getLng();
            String shareText = "Check out this place: " + place.getName() + "\n" + googleMapsUrl;

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            context.startActivity(Intent.createChooser(shareIntent, "Share via"));
        });
        holder.commentsButton.setOnClickListener(v -> {
            if (currentUser == null) {
                Toast.makeText(context, "Please sign in to view comments", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create bottom sheet dialog
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
            View bottomSheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_comments, null);
            bottomSheetDialog.setContentView(bottomSheetView);

            // Initialize views
            RecyclerView commentsRecyclerView = bottomSheetView.findViewById(R.id.commentsRecyclerView);
            EditText commentInput = bottomSheetView.findViewById(R.id.commentInput);
            ImageButton postButton = bottomSheetView.findViewById(R.id.postButton);
            ImageButton closeButton = bottomSheetView.findViewById(R.id.closeButton);

            // Setup RecyclerView
            commentsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
            List<DocumentSnapshot> commentsList = new ArrayList<>();
            CommentAdapter commentAdapter = new CommentAdapter(context, commentsList, place.getId());
            commentsRecyclerView.setAdapter(commentAdapter);

            // Load comments from Firestore
            CollectionReference commentsRef = db.collection("places")
                    .document(place.getId())
                    .collection("comments");

            commentsRef.orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(50)
                    .addSnapshotListener((snapshots, error) -> {
                        if (error != null) return;
                        commentsList.clear();
                        if (snapshots != null) {
                            commentsList.addAll(snapshots.getDocuments());
                        }
                        commentAdapter.notifyDataSetChanged();
                    });

            // Handle post button click
            postButton.setOnClickListener(v1 -> {
                String commentText = commentInput.getText().toString().trim();
                if (!commentText.isEmpty()) {
                    // Fetch user info for name and profile picture
                    String userId = currentUser.getUid();
                    Log.d("CommentDebug", "Fetching user info for userId: " + userId);
                    Log.d("CommentDebug", "Current user email: " + currentUser.getEmail());
                    Log.d("CommentDebug", "Current user is email verified: " + currentUser.isEmailVerified());

                    db.collection("users").document(userId).get()
                            .addOnSuccessListener(userDoc -> {
                                if (userDoc.exists()) {
                                    Log.d("CommentDebug", "User document exists");
                                    String firstName = userDoc.getString("firstName");
                                    String lastName = userDoc.getString("lastName");
                                    String profilePictureUrl = userDoc.getString("profilePictureUrl");

                                    Log.d("CommentDebug", "User data - firstName: " + firstName +
                                            ", lastName: " + lastName +
                                            ", profilePictureUrl: " + profilePictureUrl);

                                    if (firstName == null) {
                                        Log.e("CommentDebug", "User profile is incomplete - firstName is null");
                                        Toast.makeText(context, "User profile is incomplete. Please update your profile.", Toast.LENGTH_LONG).show();
                                        return;
                                    }

                                    String fullName = firstName + (lastName != null && !lastName.isEmpty() ? " " + lastName : "");

                                    Map<String, Object> comment = new HashMap<>();
                                    comment.put("userId", userId);
                                    comment.put("userName", fullName);
                                    comment.put("profilePictureUrl", profilePictureUrl);
                                    comment.put("text", commentText);
                                    comment.put("timestamp", System.currentTimeMillis());
                                    comment.put("likes", new HashMap<String, Boolean>());

                                    Log.d("CommentDebug", "Attempting to add comment to place: " + place.getId());
                                    commentsRef.add(comment)
                                            .addOnSuccessListener(documentReference -> {
                                                Log.d("CommentDebug", "Comment added successfully with ID: " + documentReference.getId());
                                                commentInput.setText("");
                                                Toast.makeText(context, "Comment posted", Toast.LENGTH_SHORT).show();
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e("CommentDebug", "Failed to post comment", e);
                                                Toast.makeText(context, "Failed to post comment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });
                                } else {
                                    Log.e("CommentDebug", "User document does not exist for userId: " + userId);
                                    Toast.makeText(context, "User profile not found. Please update your profile.", Toast.LENGTH_LONG).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e("CommentDebug", "Failed to get user info", e);
                                Toast.makeText(context, "Failed to get user info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                } else {
                    Toast.makeText(context, "Please enter a comment", Toast.LENGTH_SHORT).show();
                }
            });

            // Handle close button click
            closeButton.setOnClickListener(v1 -> bottomSheetDialog.dismiss());

            // Show the bottom sheet
            bottomSheetDialog.show();
        });


        // Handle click on the container
        holder.containerLayout.setOnClickListener(v -> {
            if (userLocation != null) {
                Intent intent = new Intent(context, MapActivity.class);
                intent.putExtra("userLat", userLocation.getLatitude()); // User's current latitude
                intent.putExtra("userLng", userLocation.getLongitude()); // User's current longitude
                intent.putExtra("destLat", place.getLat()); // Destination latitude
                intent.putExtra("destLng", place.getLng()); // Destination longitude
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "User location is not available.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return places.size();
    }

    public static class PlaceViewHolder extends RecyclerView.ViewHolder {

        LinearLayout containerLayout;
        ImageView placeImage;
        TextView placeName;
        ImageButton starButton, shareButton,commentsButton;

        public PlaceViewHolder(@NonNull View itemView) {
            super(itemView);
            containerLayout = itemView.findViewById(R.id.containerLayout);
            placeImage = itemView.findViewById(R.id.placeImage);
            placeName = itemView.findViewById(R.id.placeName);
            starButton = itemView.findViewById(R.id.starButton);
            shareButton = itemView.findViewById(R.id.shareButton);
            commentsButton = itemView.findViewById(R.id.commentsButton);
        }
    }
}