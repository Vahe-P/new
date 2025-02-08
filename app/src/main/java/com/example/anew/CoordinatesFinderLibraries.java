package com.example.anew;

import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicInteger;



public class CoordinatesFinderLibraries {
    private void setSearchText(TextView resultView) {
        new Handler(Looper.getMainLooper()).post(() -> resultView.setText("Searching for libraries..."));
    }

    public void getLibraryCoordinates(double userLat, double userLng, int radius, String apiKey, TextView resultView, LinearLayout container) {
        setSearchText(resultView);
        container.removeAllViews();
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" +
                userLat + "," + userLng +
                "&radius=" + radius * 1000 + // radius in meters
                "&type=library" +
                "&key=" + apiKey;

        RequestQueue queue = Volley.newRequestQueue(resultView.getContext());

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        if (results.length() > 0) {
                            StringBuilder coordinates = new StringBuilder();
                            AtomicInteger pendingRequests = new AtomicInteger(results.length());

                            for (int i = 0; i < results.length(); i++) {
                                JSONObject place = results.getJSONObject(i);
                                String placeName = place.getString("name").toLowerCase(); // Case-insensitive matching
                                JSONObject location = place.getJSONObject("geometry").getJSONObject("location");
                                double lat = location.getDouble("lat");
                                double lng = location.getDouble("lng");

                                // Filter by name containing "museum" and exclude "studio"

                                if (pendingRequests.decrementAndGet() == 0) {
                                    updateResultView(resultView, "Filtered results ready4."+String.valueOf(results.length()));
                                }

                            }
                        } else {
                            updateResultView(resultView, "No museums found within the radius.");
                        }
                    } catch (Exception e) {
                        updateResultView(resultView, "Error parsing the response.");
                    }
                },
                error -> updateResultView(resultView, "Error fetching data: " + error.getMessage())
        );

        queue.add(request);
    }

    private void getStreetDistance(double userLat, double userLng, double destLat, double destLng, int radius, String apiKey,
                                   StringBuilder coordinates, JSONObject place, TextView resultView, AtomicInteger pendingRequests, LinearLayout container) {
        String distanceUrl = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=" +
                userLat + "," + userLng +
                "&destinations=" + destLat + "," + destLng +
                "&key=" + apiKey;

        RequestQueue queue = Volley.newRequestQueue(resultView.getContext());

        JsonObjectRequest distanceRequest = new JsonObjectRequest(Request.Method.GET, distanceUrl, null,
                response -> {
                    try {
                        JSONArray rows = response.getJSONArray("rows");
                        if (rows.length() > 0) {
                            JSONObject elements = rows.getJSONObject(0).getJSONArray("elements").getJSONObject(0);
                            if (elements.getString("status").equals("OK")) {
                                String distanceText = elements.getJSONObject("distance").getString("text");
                                double distanceInMeters = parseDistance(distanceText);

                                // Include only if within the radius
                                if (distanceInMeters <= radius * 1000 ) {
                                    coordinates.append(place.getString("name")).append(": ")
                                            .append(destLat).append(", ").append(destLng)
                                            .append(" (").append(distanceText).append(" via street)\n");

                                    // Add the place to the UI, including the distance
                                    addPlaceToContainer(place, distanceText, container, apiKey);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e("DistanceMatrixError", "Error parsing distance response: " + e.getMessage());
                    }

                    if (pendingRequests.decrementAndGet() == 0) {
                        updateResultView(resultView, "Filtered results ready3.");
                    }
                },
                error -> {
                    Log.e("DistanceMatrixError", "Error fetching distance: " + error.getMessage());
                    if (pendingRequests.decrementAndGet() == 0) {
                        updateResultView(resultView, "Filtered results ready2.");
                    }
                }
        );

        queue.add(distanceRequest);
    }

    private double parseDistance(String distanceText) {
        try {
            String[] parts = distanceText.split(" ");
            double distanceInKm = Double.parseDouble(parts[0]);
            return distanceInKm * 1000;
        } catch (NumberFormatException e) {
            Log.e("DEBUG", "Error parsing distance: " + distanceText, e);
            return 0;
        }
    }

    private void updateResultView(TextView resultView, String text) {
        new Handler(Looper.getMainLooper()).post(() -> resultView.setText(text));
    }

    private void addPlaceToContainer(JSONObject place, String distanceText, LinearLayout container, String apiKey) {
        try {
            String name = place.getString("name");
            String photoUrl = getPhotoUrl(place, apiKey);

            // Create a horizontal LinearLayout to act as a button
            LinearLayout buttonLayout = new LinearLayout(container.getContext());
            buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
            buttonLayout.setBackgroundResource(android.R.drawable.btn_default); // Make it look like a button
            buttonLayout.setPadding(16, 16, 16, 16);
            buttonLayout.setClickable(true);
            buttonLayout.setFocusable(true);

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            layoutParams.setMargins(0, 0, 0, 10);
            buttonLayout.setLayoutParams(layoutParams);

            // Create ImageView for the museum image
            ImageView imageView = new ImageView(container.getContext());
            int imageSize = 150; // Set image size
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(imageSize, imageSize);
            imageParams.setMargins(0, 0, 16, 0);
            imageView.setLayoutParams(imageParams);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setImageResource(R.drawable.download); // Default image

            // Load the actual image dynamically
            if (photoUrl != null) {
                RequestQueue queue = Volley.newRequestQueue(container.getContext());
                ImageRequest imageRequest = new ImageRequest(photoUrl,
                        response -> imageView.setImageDrawable(new BitmapDrawable(container.getResources(), response)),
                        0, 0, null, null,
                        error -> Log.e("ImageLoadError", "Error loading image: " + error.getMessage()));
                queue.add(imageRequest);
            }

            // Create TextView for the museum name and distance
            TextView textView = new TextView(container.getContext());
            textView.setText(name + " - Distance: " + distanceText);
            textView.setTextSize(16);
            textView.setTextColor(container.getContext().getResources().getColor(android.R.color.black));

            // Add ImageView and TextView to the button layout
            buttonLayout.addView(imageView);
            buttonLayout.addView(textView);

            // Set click event (optional)
            buttonLayout.setOnClickListener(v -> Toast.makeText(container.getContext(), "Clicked: " + name, Toast.LENGTH_SHORT).show());

            // Add the layout to the container
            container.addView(buttonLayout);

        } catch (JSONException e) {
            Log.e("DEBUG", "Error adding place to container: " + e.getMessage());
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
        return null; // No photo available
    }
    private static final String[] excludedWords = {
            "Hotel"
    };


    private boolean shouldExclude(String placeName) {
        // Check if the place name contains any of the excluded words
        for (String word : excludedWords) {
            if (placeName.toLowerCase().contains(word.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

}
