package com.example.anew;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import android.graphics.Typeface;
import androidx.core.content.res.ResourcesCompat;

import android.widget.EditText;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class PlaceAdapter_2 extends RecyclerView.Adapter<PlaceAdapter_2.ChurchViewHolder> {

    private Context context;
    private List<Place_2> churches;
    private Location userLocation;
    private FusedLocationProviderClient fusedLocationProviderClient;
    public double userLng;
    public double userLat;

    public PlaceAdapter_2(Context context, List<Place_2> churches,double userLat,double userLng) {
        this.context = context;
        this.churches = churches;
        this.userLat=userLat;
        this.userLng=userLng;

        this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
        getUserLocation();
    }

    @NonNull
    @Override
    public ChurchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.activity_place_adapter2, parent, false);
        return new ChurchViewHolder(view);
    }

    private void getUserLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
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

    @Override
    public void onBindViewHolder(@NonNull ChurchViewHolder holder, int position) {
        Place_2 church = churches.get(position);

        if (church.getImageUrl() != null && !church.getImageUrl().isEmpty()) {
            RequestQueue queue = Volley.newRequestQueue(context);
            ImageRequest imageRequest = new ImageRequest(church.getImageUrl(),
                    response -> holder.churchImage.setImageDrawable(new BitmapDrawable(context.getResources(), response)),
                    0, 0, ImageView.ScaleType.CENTER_CROP, null,
                    error -> {
                        Log.e("ImageLoadError", "Error loading image: " + error.getMessage());
                        holder.churchImage.setImageResource(R.drawable.download);
                    });
            queue.add(imageRequest);
        } else {
            holder.churchImage.setImageResource(R.drawable.download);
        }
        Typeface customFont = ResourcesCompat.getFont(context, R.font.roboto_condensed_black);
        holder.churchName.setTypeface(customFont);
        holder.churchDistance.setTypeface(customFont);
        holder.category.setTypeface(customFont);

        holder.category.setText(church.getCategory());
        holder.churchName.setText(church.getName());
        holder.churchDistance.setText(church.getDistance());
        if (church.getName().length() > 20) {
            holder.churchName.setTextSize(14);
        } else if (church.getName().length() > 30) {
            holder.churchName.setTextSize(12);
        }
        Log.d("btn", church.getName());

        String placeId = church.getId();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DocumentReference favoriteRef = db.collection("users")
                    .document(userId)
                    .collection("favorites")
                    .document(placeId);
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
                final boolean newFavoriteState = !isFavorite[0];
                if (newFavoriteState) {
                    Map<String, Object> favoritePlace = new HashMap<>();
                    favoritePlace.put("name", church.getName());
                    favoritePlace.put("lat", church.getLat());
                    favoritePlace.put("lng", church.getLng());
                    favoritePlace.put("imageUrl", church.getImageUrl());

                    favoriteRef.set(favoritePlace)
                            .addOnSuccessListener(aVoid -> {
                                isFavorite[0] = true;
                                holder.starButton.setImageResource(R.drawable.ic_star_filled);
                                Toast.makeText(context, "Added to favorites", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                holder.starButton.setImageResource(R.drawable.ic_star_empty);
                                Toast.makeText(context, "Failed to add", Toast.LENGTH_SHORT).show();
                            });
                } else {
                    favoriteRef.delete()
                            .addOnSuccessListener(aVoid -> {
                                isFavorite[0] = false;
                                holder.starButton.setImageResource(R.drawable.ic_star_empty);
                                Toast.makeText(context, "Removed from favorites", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                holder.starButton.setImageResource(R.drawable.ic_star_filled);
                                Toast.makeText(context, "Failed to remove", Toast.LENGTH_SHORT).show();
                            });
                }
            });
        }

        holder.shareButton.setOnClickListener(v -> {
            String googleMapsUrl = "https://www.google.com/maps/search/?api=1&query=" + church.getLat() + "," + church.getLng();
            String shareText = "Check out this place: " + church.getName() + "\n" + googleMapsUrl;

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            context.startActivity(Intent.createChooser(shareIntent, "Share via"));
        });

<<<<<<< HEAD
=======
        // Handle comments button
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
            CommentAdapter commentAdapter = new CommentAdapter(context, commentsList, church.getId());
            commentsRecyclerView.setAdapter(commentAdapter);

            // Load comments from Firestore
            CollectionReference commentsRef = db.collection("places")
                    .document(church.getId())
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
                                    
                                    Log.d("CommentDebug", "Attempting to add comment to place: " + church.getId());
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

        // OnClickListener to open MapActivity
>>>>>>> 2ef41b3152620b48a7166eb50f19d0cef7c9a2f9
        holder.containerLayout.setOnClickListener(v -> {
            Intent intent = new Intent(context, MapActivity.class);
            intent.putExtra("userLat", userLat);
            intent.putExtra("userLng", userLng);
            intent.putExtra("destLat", church.getLat());
            intent.putExtra("destLng", church.getLng());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return churches.size();
    }
    public void clearData() {
        if (churches != null) {
            churches.clear();
            notifyDataSetChanged();
        }
    }

    public static class ChurchViewHolder extends RecyclerView.ViewHolder {
        LinearLayout containerLayout;
        ImageView churchImage;
        TextView churchName, churchDistance,category;
        ImageButton starButton, shareButton, commentsButton;

        public ChurchViewHolder(@NonNull View itemView) {
            super(itemView);
            containerLayout = itemView.findViewById(R.id.containerLayout);
            category=itemView.findViewById(R.id.category);
            churchImage = itemView.findViewById(R.id.placeImage);
            churchName = itemView.findViewById(R.id.placeName);
            churchDistance = itemView.findViewById(R.id.placeDistance);
            starButton = itemView.findViewById(R.id.starButton);
            shareButton = itemView.findViewById(R.id.shareButton);
<<<<<<< HEAD
=======
            commentsButton = itemView.findViewById(R.id.commentsButton);
>>>>>>> 2ef41b3152620b48a7166eb50f19d0cef7c9a2f9
        }
    }
}