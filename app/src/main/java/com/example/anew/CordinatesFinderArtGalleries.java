package com.example.anew;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import android.graphics.drawable.BitmapDrawable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.res.ResourcesCompat;

import com.android.volley.toolbox.ImageRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import android.widget.ProgressBar;
import android.widget.FrameLayout;

public class CordinatesFinderArtGalleries {
    public boolean findedForArtGalleriess = false;
    private void SearchText(TextView resultView) {
        new Handler(Looper.getMainLooper()).post(() -> resultView.setText("Searching for galleries..."));
    }

    public void getArtGalleryCoordinates(double userLat, double userLng, int radius, String apiKey, TextView resultView, GridLayout resultsContainer) {
        SearchText(resultView);
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" +
                userLat + "," + userLng +
                "&radius=" + radius * 1000 + // radius in meters
                "&keyword=art_gallery art_museum" +
                "&key=" + apiKey;

        RequestQueue queue = Volley.newRequestQueue(resultView.getContext());

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        if (results.length() > 0) {
                            StringBuilder coordinates = new StringBuilder();
                            StringBuilder destinations = new StringBuilder();
                            AtomicInteger pendingRequests = new AtomicInteger(results.length());

                            for (int i = 0; i < results.length(); i++) {
                                JSONObject place = results.getJSONObject(i);
                                JSONObject location = place.getJSONObject("geometry").getJSONObject("location");
                                double lat = location.getDouble("lat");
                                double lng = location.getDouble("lng");

                                // Build destinations for batch Distance Matrix API request
                                destinations.append(lat).append(",").append(lng);
                                if (i < results.length() - 1) {
                                    destinations.append("|");
                                }
                            }

                            // Fetch street distances for all places in a single request
                            getStreetDistances(userLat, userLng, destinations.toString(), radius, apiKey, coordinates, results, resultView, pendingRequests, resultsContainer);
                        } else {
                            updateResultView(resultView, "No art galleries found within the radius.");
                        }
                    } catch (Exception e) {
                        updateResultView(resultView, "Error parsing the response.");
                    }
                },
                error -> updateResultView(resultView, "No internet connection")
        );

        queue.add(request);
    }

    private void getStreetDistances(double userLat, double userLng, String destinations, int radius, String apiKey,
                                    StringBuilder coordinates, JSONArray results, TextView resultView, AtomicInteger pendingRequests, GridLayout container) {
        String distanceUrl = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=" +
                userLat + "," + userLng +
                "&destinations=" + destinations +
                "&key=" + apiKey;

        RequestQueue queue = Volley.newRequestQueue(resultView.getContext());

        JsonObjectRequest distanceRequest = new JsonObjectRequest(Request.Method.GET, distanceUrl, null,
                response -> {
                    try {
                        JSONArray rows = response.getJSONArray("rows");
                        if (rows.length() > 0) {
                            JSONArray elements = rows.getJSONObject(0).getJSONArray("elements");

                            for (int i = 0; i < elements.length(); i++) {
                                JSONObject element = elements.getJSONObject(i);
                                JSONObject place = results.getJSONObject(i);
                                if (element.getString("status").equals("OK")) {
                                    String distanceText = element.getJSONObject("distance").getString("text"); // E.g., "2.5 km"
                                    coordinates.append(place.getString("name")).append(": ")
                                            .append(destinations.split("\\|")[i])
                                            .append(" (").append(distanceText).append(" via street)\n");

                                    addPlaceToContainer(place, container, apiKey, distanceText, radius, userLat,  userLng,  place.getJSONObject("geometry").getJSONObject("location").getDouble("lat"), place.getJSONObject("geometry").getJSONObject("location").getDouble("lng"));
                                } else {
                                    coordinates.append("Distance unavailable for: ").append(place.getString("name")).append("\n");
                                }
                            }
                        }
                    } catch (JSONException e) {
                        Log.e("DistanceMatrixError", "Error parsing distance response: " + e.getMessage());
                    }

                    if (pendingRequests.decrementAndGet() == 0) {
                        if (!findedForArtGalleriess) {
                            updateResultView(resultView, "No galleries found within the radius");
                        } else {
                            updateResultView(resultView, "Filtered Results:");
                        }
                    }
                },
                error -> Log.e("DistanceMatrixError", "Error fetching distance: " + error.getMessage())
        );

        queue.add(distanceRequest);
    }

    private void updateResultView(TextView resultView, String text) {
        new Handler(Looper.getMainLooper()).post(() -> resultView.setText(text));
    }
    private boolean nameChecker(String name){
        if(name.contains("Studio")){
            return false;
        }
        return true;
    }
    private void addPlaceToContainer(JSONObject place, GridLayout container, String apiKey, String distanceText, int radius, double userLat, double userLng, double destLat, double destLng) {

        try {
            // Array of drawable resource IDs
            int[] drawableIds = {
                    R.drawable.images,
                    R.drawable.bg,
                    R.drawable.imgg1,
                    R.drawable.imgg2,
                    R.drawable.imgg3,
                    R.drawable.imgg4,
                    R.drawable.imgg5,
                    R.drawable.imgg6,
                    R.drawable.imgg7,
                    R.drawable.imgg8,
                    R.drawable.canada,
                    R.drawable.us,
                    R.drawable.wnderimage
            };

            // Generate a random index to select an image
            Random random = new Random();
            int randomIndex = random.nextInt(drawableIds.length);
            int selectedDrawableId = drawableIds[randomIndex];

            // Create a drawable and set its alpha value for transparency
            Drawable drawable = ResourcesCompat.getDrawable(container.getResources(), selectedDrawableId, null);
            if (drawable instanceof BitmapDrawable) {
                BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
                bitmapDrawable.setAlpha(56); // Set transparency (0-255)
            }

            int totalButtons = container.getChildCount() + 1; // Increment by one as we are adding one more button
            int columns = (int) Math.ceil(totalButtons / 2.0); // Calculate the number of columns needed

            // Update column count of GridLayout dynamically
            container.setColumnCount(columns);

            if (radius >= Float.parseFloat(distanceText.substring(0, distanceText.length() - 2)) && nameChecker(place.getString("name"))) {
                String name = place.getString("name");
                String photoUrl = getPhotoUrl(place, apiKey);
                findedForArtGalleriess = true;

                // Create the button layout
                LinearLayout buttonLayout = new LinearLayout(container.getContext());
                buttonLayout.setOrientation(LinearLayout.VERTICAL);
                buttonLayout.setBackground(drawable); // Set custom background with transparency
                buttonLayout.setPadding(16, 16, 16, 16);
                buttonLayout.setClickable(true);
                buttonLayout.setFocusable(true);

                // Set the size of the button
                int buttonWidth = 400;
                int buttonHeight = 600;
                GridLayout.LayoutParams buttonParams = new GridLayout.LayoutParams();
                buttonParams.width = buttonWidth;
                buttonParams.height = buttonHeight;
                buttonParams.setMargins(16, 16, 16, 16);
                buttonLayout.setLayoutParams(buttonParams);

                // Create the ImageView with rounded corners
                ImageView imageView = new ImageView(container.getContext());
                int imageSize = 300; // Set the image size
                LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(imageSize, imageSize);
                imageParams.gravity = Gravity.CENTER_HORIZONTAL;
                imageView.setLayoutParams(imageParams);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

                // Set rounded corners
                imageView.setBackgroundResource(R.drawable.rounded_corners);
                imageView.setClipToOutline(true);

                // Create a ProgressBar for loading indication
                ProgressBar imageProgressBar = new ProgressBar(container.getContext());
                LinearLayout.LayoutParams progressBarParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                progressBarParams.gravity = Gravity.CENTER;
                imageProgressBar.setLayoutParams(progressBarParams);
                
                // Create a FrameLayout to hold ImageView and ProgressBar
                FrameLayout imageFrame = new FrameLayout(container.getContext());
                LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(imageSize, imageSize);
                frameParams.gravity = Gravity.CENTER_HORIZONTAL;
                imageFrame.setLayoutParams(frameParams);

                imageFrame.addView(imageView);
                imageFrame.addView(imageProgressBar);

                // Initially, show ProgressBar and set placeholder for ImageView
                imageProgressBar.setVisibility(View.VISIBLE);
                imageView.setImageResource(R.drawable.download); // Set placeholder

                // Load the image from the URL if available
                if (photoUrl != null) {
                    RequestQueue queue = Volley.newRequestQueue(container.getContext());
                    ImageRequest imageRequest = new ImageRequest(photoUrl,
                            response -> {
                                imageView.setImageDrawable(new BitmapDrawable(container.getResources(), response));
                                imageProgressBar.setVisibility(View.GONE); // Hide ProgressBar on success
                            },
                            0, 0, ImageView.ScaleType.CENTER_CROP, null, // Added ScaleType
                            error -> {
                                Log.e("ImageLoadError", "Error loading image: " + error.getMessage());
                                imageProgressBar.setVisibility(View.GONE); // Hide ProgressBar on error
                                // Optionally, set an error image or keep the placeholder
                                imageView.setImageResource(R.drawable.baseline_error_24); // Example error image - ensure this drawable exists
                            });
                    queue.add(imageRequest);
                } else {
                    imageProgressBar.setVisibility(View.GONE); // Hide ProgressBar if no photoUrl
                    imageView.setImageResource(R.drawable.baseline_hide_image_24); // Example 'no image' placeholder - ensure this drawable exists
                }

                // Create the TextViews for name and distance
                TextView textView = new TextView(container.getContext());
                textView.setText(name);
                textView.setTextSize(16);// Set custom font
                textView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
                textView.setPadding(0, 8, 0, 0); // Add padding to the top of the text
                textView.setMaxLines(1);
                textView.setEllipsize(TextUtils.TruncateAt.END);

                // Adjust text size based on name length
                if (name.length() > 20) {
                    textView.setTextSize(14);
                } else if (name.length() > 30) {
                    textView.setTextSize(12);
                }

                TextView distanceView = new TextView(container.getContext());
                distanceView.setText("Distance: " + distanceText);
                distanceView.setTextSize(14);
                distanceView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
                distanceView.setPadding(0, 4, 0, 16); // Add padding to the top and bottom of the text
                TextView categoryView=new TextView(container.getContext());
                categoryView.setText("Art Gallery");
                categoryView.setGravity(Gravity.LEFT | Gravity.BOTTOM);
                categoryView.setPadding(0, 8, 0, 0);
                categoryView.setTextSize(12);
                // Add TextViews to the button layout
                buttonLayout.addView(imageFrame); // Add FrameLayout instead of ImageView directly
                buttonLayout.addView(categoryView);
                buttonLayout.addView(textView);
                buttonLayout.addView(distanceView);
                // Create a horizontal layout to hold the star and share buttons
                LinearLayout actionButtonsLayout = new LinearLayout(container.getContext());
                actionButtonsLayout.setOrientation(LinearLayout.HORIZONTAL);
                actionButtonsLayout.setGravity(Gravity.CENTER_HORIZONTAL);
                LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                actionsParams.setMargins(0, 8, 0, 0);
                actionButtonsLayout.setLayoutParams(actionsParams);

// Create the star button
                ImageButton starButton = new ImageButton(container.getContext());
                LinearLayout.LayoutParams starParams = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1
                );
                starButton.setLayoutParams(starParams);
                starButton.setBackgroundColor(Color.TRANSPARENT);
                starButton.setImageResource(R.drawable.ic_star_empty);

                FirebaseFirestore db = FirebaseFirestore.getInstance();
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();


                if (currentUser == null) {
                    // Toast.makeText(container.getContext(), "You must be signed in to save favorites", Toast.LENGTH_SHORT).show();

                }else{

                    String userId = currentUser.getUid();
                    String placeId = place.getString("place_id");
// sanitize the name for ID

                    DocumentReference favoriteRef = db.collection("users")
                            .document(userId)
                            .collection("favorites")
                            .document(placeId);
                    final boolean[] isFavorite = {false};

                    favoriteRef.get().addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            starButton.setImageResource(R.drawable.ic_star_filled);
                            isFavorite[0] = true;
                        } else {
                            starButton.setImageResource(R.drawable.ic_star_empty);
                            isFavorite[0] = false;
                        }
                    });

                    starButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            isFavorite[0] = !isFavorite[0];

                            if (isFavorite[0]) {
                                Map<String, Object> favoritePlace = new HashMap<>();
                                favoritePlace.put("name", name);
                                favoritePlace.put("lat", destLat);
                                favoritePlace.put("lng", destLng);
                                favoritePlace.put("imageUrl", photoUrl);

                                favoriteRef.set(favoritePlace)
                                        .addOnSuccessListener(aVoid -> {
                                            starButton.setImageResource(R.drawable.ic_star_filled);
                                            // Toast.makeText(container.getContext(), "Added to favorites", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            isFavorite[0] = false;
                                            starButton.setImageResource(R.drawable.ic_star_empty);
                                            // Toast.makeText(container.getContext(), "Failed to add", Toast.LENGTH_SHORT).show();
                                        });
                            } else {
                                favoriteRef.delete()
                                        .addOnSuccessListener(aVoid -> {
                                            starButton.setImageResource(R.drawable.ic_star_empty);
                                            // Toast.makeText(container.getContext(), "Removed from favorites", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            isFavorite[0] = true;
                                            starButton.setImageResource(R.drawable.ic_star_filled);
                                            // Toast.makeText(container.getContext(), "Failed to remove", Toast.LENGTH_SHORT).show();
                                        });
                            }
                        }
                    });


                }


// Create the share button
                ImageButton shareButton = new ImageButton(container.getContext());
                LinearLayout.LayoutParams shareParams = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1
                );
                shareButton.setLayoutParams(shareParams);
                shareButton.setBackgroundColor(Color.TRANSPARENT);
                shareButton.setImageResource(R.drawable.baseline_share_24); // Make sure you have this drawable

// Handle share intent
                shareButton.setOnClickListener(v -> {
                    String googleMapsUrl = "https://www.google.com/maps/search/?api=1&query=" + destLat + "," + destLng;
                    String shareText = "Check out this place: " + name + "\n" + googleMapsUrl;

                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                    container.getContext().startActivity(Intent.createChooser(shareIntent, "Share via"));
                });

// Add star and share buttons to the layout
                actionButtonsLayout.addView(starButton);
                actionButtonsLayout.addView(shareButton);

// Add the horizontal layout to the button layout
                buttonLayout.addView(actionButtonsLayout);



                // Set the onClickListener for the button layout
                buttonLayout.setOnClickListener(v -> {
                    Intent intent = new Intent(container.getContext(), MapActivity.class);
                    intent.putExtra("userLat", userLat);
                    intent.putExtra("userLng", userLng);
                    intent.putExtra("destLat", destLat);
                    intent.putExtra("destLng", destLng);
                    container.getContext().startActivity(intent);
                });

                // Add the button layout to the container
                container.addView(buttonLayout);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String getPhotoUrl(JSONObject place, String apiKey) {
        try {
            JSONArray photos = place.optJSONArray("photos");
            if (photos != null && photos.length() > 0) {
                String photoReference = photos.getJSONObject(0).getString("photo_reference");
                return "https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photo_reference=" + photoReference + "&key=" + apiKey;
            }
        } catch (JSONException e) {
            Log.e("DEBUG", "Error extracting photo reference: " + e.getMessage());
        }
        return null;
    }
}