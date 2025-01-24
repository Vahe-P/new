package com.example.anew;

import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CordinatesFinderMuseums {
    private void setSearchText(TextView resultView) {
        new Handler(Looper.getMainLooper()).post(() -> resultView.setText("Searching for museums..."));
    }

    public void getMuseumCoordinates(double userLat, double userLng, int radius, String apiKey, TextView resultView, LinearLayout container) {
        setSearchText(resultView);
        container.removeAllViews();
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" +
                userLat + "," + userLng +
                "&radius=" + radius * 1000 + // radius in meters
                "&type=museum" +
                "&key=" + apiKey;

        RequestQueue queue = Volley.newRequestQueue(resultView.getContext());

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        if (results.length() > 0) {
                            StringBuilder coordinates = new StringBuilder();
                            AtomicInteger pendingRequests = new AtomicInteger(results.length());

                            // Call the function to create TextViews for all coordinates


                            for (int i = 0; i < results.length(); i++) {
                                JSONObject place = results.getJSONObject(i);
                                String placeName = place.getString("name");
                                JSONObject location = place.getJSONObject("geometry").getJSONObject("location");
                                double lat = location.getDouble("lat");
                                double lng = location.getDouble("lng");

                                // Filter by name containing "museum"
                                if (placeName.toLowerCase().contains("museum")) {
                                    // Fetch street distance for each museum
                                    getStreetDistance(userLat, userLng, lat, lng, radius, apiKey, coordinates, placeName, resultView, pendingRequests, results.length(),results,container);
                                } else {
                                    // If it doesn't match, decrement pending requests count
                                    if (pendingRequests.decrementAndGet() == 0) {
                                        createButtonsForMuseums(results, container);
                                        updateResultView(resultView, "Here We Go");
                                    }
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
                                   StringBuilder coordinates, String placeName, TextView resultView, AtomicInteger pendingRequests, int totalRequests,JSONArray results, LinearLayout container) {
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

                                // Only include the museum if its street distance is within the radius (in meters)
                                if (distanceInMeters <= radius * 1000) { // Radius converted to meters
                                    coordinates.append(placeName).append(": ")
                                            .append(destLat).append(", ").append(destLng)
                                            .append(" (").append(distanceText).append(" via street)\n");
                                }
                            } else {
                                coordinates.append(placeName).append(": ")
                                        .append(destLat).append(", ").append(destLng)
                                        .append(" (Distance unavailable)\n");
                            }
                        } else {
                            coordinates.append(placeName).append(": ")
                                    .append(destLat).append(", ").append(destLng)
                                    .append(" (Distance unavailable)\n");
                        }
                    } catch (Exception e) {
                        Log.e("DistanceMatrixError", "Error parsing distance response: " + e.getMessage());
                        coordinates.append("Error fetching distance for ").append(placeName).append("\n");
                    }

                    if (pendingRequests.decrementAndGet() == 0) {
                        updateResultView(resultView, "Here We Go");
                        createButtonsForMuseums(results, container);
                    }
                },
                error -> {
                    Log.e("DistanceMatrixError", "Error fetching distance: " + error.getMessage());
                    coordinates.append("Error fetching distance for ").append(placeName).append("\n");
                    if (pendingRequests.decrementAndGet() == 0) {
                        updateResultView(resultView, "Here We Go");
                        createButtonsForMuseums(results, container);
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

    private void createButtonsForMuseums(final JSONArray results, final LinearLayout container) {
        try {
            // Clear any existing views in the container only once
            container.removeAllViews();

            // Loop through the results from the API to create buttons
            for (int i = 0; i < results.length(); i++) {
                JSONObject place = results.getJSONObject(i);
                String name = place.getString("name");

                // Create a new Button for each museum
                Button button = new Button(container.getContext());

                // Set text for the button
                button.setText(name);

                // Set a default image (replace with an actual image resource if needed)
                button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.download, 0, 0, 0);

                // Apply some styling (padding, margins)
                button.setPadding(16, 16, 16, 16);
                button.setTextSize(16);

                // Set layout parameters to make sure buttons are added correctly
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 0, 0, 10);  // Add margin between buttons
                button.setLayoutParams(params);

                // Add the button to the container
                container.addView(button);

                // Log to confirm that the button is created
                Log.d("DEBUG", "Created button for museum: " + name);
            }

            // Ensure the layout is refreshed and the buttons are shown
            container.requestLayout();  // Request a layout pass
            container.invalidate();     // Force a redraw

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
