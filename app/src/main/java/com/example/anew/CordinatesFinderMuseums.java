package com.example.anew;

import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicInteger;

public class CordinatesFinderMuseums {
    private void SearchText(TextView resultView) {
        new Handler(Looper.getMainLooper()).post(() -> resultView.setText("Searching for museums"));
    }
    public void getMuseumCoordinates(double userLat, double userLng, int radius, String apiKey, TextView resultView) {
        SearchText(resultView);
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

                            for (int i = 0; i < results.length(); i++) {
                                JSONObject place = results.getJSONObject(i);
                                String placeName = place.getString("name");
                                JSONObject location = place.getJSONObject("geometry").getJSONObject("location");
                                double lat = location.getDouble("lat");
                                double lng = location.getDouble("lng");

                                // Filter by name containing "museum"
                                if (placeName.toLowerCase().contains("museum")) {
                                    // Fetch street distance for each museum
                                    getStreetDistance(userLat, userLng, lat, lng, radius, apiKey, coordinates, i + 1, resultView, pendingRequests, results.length());
                                } else {
                                    // If it doesn't match, decrement pending requests count
                                    if (pendingRequests.decrementAndGet() == 0) {
                                        updateResultView(resultView, coordinates.toString());
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
                                   StringBuilder coordinates, int index, TextView resultView, AtomicInteger pendingRequests, int totalRequests) {
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
                                    coordinates.append("Museum ").append(": ")
                                            .append(destLat).append(", ").append(destLng)
                                            .append(" (").append(distanceText).append(" via street)\n");
                                }
                            } else {
                                coordinates.append("Museum ").append(index).append(": ")
                                        .append(destLat).append(", ").append(destLng)
                                        .append(" (Distance unavailable)\n");
                            }
                        } else {
                            coordinates.append("Museum ").append(index).append(": ")
                                    .append(destLat).append(", ").append(destLng)
                                    .append(" (Distance unavailable)\n");
                        }
                    } catch (Exception e) {
                        Log.e("DistanceMatrixError", "Error parsing distance response: " + e.getMessage());
                        coordinates.append("Error fetching distance for Museum ").append(index).append("\n");
                    }

                    if (pendingRequests.decrementAndGet() == 0) {
                        updateResultView(resultView, coordinates.toString());
                    }
                },
                error -> {
                    Log.e("DistanceMatrixError", "Error fetching distance: " + error.getMessage());
                    coordinates.append("Error fetching distance for Museum ").append(index).append("\n");
                    if (pendingRequests.decrementAndGet() == 0) {
                        updateResultView(resultView, coordinates.toString());
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
}
