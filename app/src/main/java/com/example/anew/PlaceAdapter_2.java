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
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.graphics.Typeface;
import androidx.core.content.res.ResourcesCompat;

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
                isFavorite[0] = !isFavorite[0];
                if (isFavorite[0]) {
                    Map<String, Object> favoritePlace = new HashMap<>();
                    favoritePlace.put("name", church.getName());
                    favoritePlace.put("lat", church.getLat());
                    favoritePlace.put("lng", church.getLng());
                    favoritePlace.put("imageUrl", church.getImageUrl());

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

        holder.shareButton.setOnClickListener(v -> {
            String googleMapsUrl = "https://www.google.com/maps/search/?api=1&query=" + church.getLat() + "," + church.getLng();
            String shareText = "Check out this place: " + church.getName() + "\n" + googleMapsUrl;

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            context.startActivity(Intent.createChooser(shareIntent, "Share via"));
        });

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
        ImageButton starButton, shareButton;

        public ChurchViewHolder(@NonNull View itemView) {
            super(itemView);
            containerLayout = itemView.findViewById(R.id.containerLayout);
            category=itemView.findViewById(R.id.category);
            churchImage = itemView.findViewById(R.id.placeImage);
            churchName = itemView.findViewById(R.id.placeName);
            churchDistance = itemView.findViewById(R.id.placeDistance);
            starButton = itemView.findViewById(R.id.starButton);
            shareButton = itemView.findViewById(R.id.shareButton);
        }
    }
}