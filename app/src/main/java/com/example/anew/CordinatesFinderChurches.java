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


public class CordinatesFinderChurches {
    private void SearchText(TextView resultView) {
        new Handler(Looper.getMainLooper()).post(() -> resultView.setText("Searching for churches"));
    }

    public void getChurchCoordinates(double userLat, double userLng, int radius, String apiKey, TextView resultView) {
        SearchText(resultView);
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" +
                userLat + "," + userLng +
                "&radius=" + radius * 1000 + // radius in meters
                "&type=church" +
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
                                JSONObject location = place.getJSONObject("geometry").getJSONObject("location");
                                double lat = location.getDouble("lat");
                                double lng = location.getDouble("lng");

                                // Fetch street distance for each church
                                getStreetDistance(userLat, userLng, lat, lng, radius, apiKey, coordinates, i + 1, resultView, pendingRequests, results.length());
                            }
                        } else {
                            updateResultView(resultView, "No churches found within the radius.");
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

        // Log the radius and destination info for debugging
        Log.d("DEBUG", "Radius (in km): " + radius + " | Radius (in meters): " + (radius * 1000));
        Log.d("DEBUG", "User Location: (" + userLat + ", " + userLng + ")");
        Log.d("DEBUG", "Destination Location: (" + destLat + ", " + destLng + ")");

        RequestQueue queue = Volley.newRequestQueue(resultView.getContext());

        JsonObjectRequest distanceRequest = new JsonObjectRequest(Request.Method.GET, distanceUrl, null,
                response -> {
                    try {
                        // Log the raw API response for debugging
                        Log.d("DEBUG", "API Response: " + response.toString());

                        JSONArray rows = response.getJSONArray("rows");
                        if (rows.length() > 0) {
                            JSONObject elements = rows.getJSONObject(0).getJSONArray("elements").getJSONObject(0);
                            if (elements.getString("status").equals("OK")) {
                                String distanceText = elements.getJSONObject("distance").getString("text");
                                int distanceValue = parseDistanceToMeters(distanceText);
                                double distanceInMeters = parseDistance(distanceText); // Convert it to meters


                                // Log the parsed street distance for debugging
                                Log.d("DEBUG", "Fetched distance: " + distanceText + " | Distance in meters: " + distanceValue);

                                // Convert radius to meters if the radius is in kilometers
                                int radiusInMeters = radius * 1000; // Convert radius from kilometers to meters

                                // Log the comparison
                                Log.d("DEBUG", "Comparing distance with radius: " + distanceValue + " vs. " + radiusInMeters);

                                // Only include the church if its street distance is within the radius (in meters)
                                if (distanceInMeters <= radiusInMeters) {
                                    coordinates.append("Church ").append(": ")
                                            .append(destLat).append(", ").append(destLng)
                                            .append(" (").append(distanceText).append(" via street)\n");
                                } else {
                                    Log.d("DEBUG", "Skipping Church " + index + " due to distance: " + distanceValue + " meters (greater than radius).");
                                }
                            } else {
                                coordinates.append("Church ").append(index).append(": ")
                                        .append(destLat).append(", ").append(destLng)
                                        .append(" (Distance unavailable)\n");
                            }
                        } else {
                            coordinates.append("Church ").append(index).append(": ")
                                    .append(destLat).append(", ").append(destLng)
                                    .append(" (Distance unavailable)\n");
                        }
                    } catch (Exception e) {
                        Log.e("DistanceMatrixError", "Error parsing distance response: " + e.getMessage());
                        coordinates.append("Error fetching distance for Church ").append(index).append("\n");
                    }

                    // Check if all requests are completed
                    if (pendingRequests.decrementAndGet() == 0) {
                        updateResultView(resultView, coordinates.toString());
                    }
                },
                error -> {
                    Log.e("DistanceMatrixError", "Error fetching distance: " + error.getMessage());
                    coordinates.append("Error fetching distance for Church ").append(index).append("\n");
                    if (pendingRequests.decrementAndGet() == 0) {
                        updateResultView(resultView, coordinates.toString());
                    }
                }
        );

        queue.add(distanceRequest);
    }

    // Convert distance from text (e.g., "500 meters" or "0.5 km") to an integer value in meters
    private int parseDistanceToMeters(String distanceText) {
        int distance = 0;

        try {
            // Log the raw distance text for debugging
            Log.d("DEBUG", "Parsing distance text: " + distanceText);

            // Check if the distanceText contains a number and the unit is meters or kilometers
            if (distanceText.contains("m")) {
                distance = Integer.parseInt(distanceText.replace(" m", "").replace(",", ""));
            } else if (distanceText.contains("km")) {
                distance = (int) (Double.parseDouble(distanceText.replace(" km", "").replace(",", "")) * 1000);
            }

            // Log the final parsed distance in meters
            Log.d("DEBUG", "Parsed distance (in meters): " + distance);
        } catch (Exception e) {
            Log.e("DistanceParseError", "Error parsing distance: " + distanceText);
        }

        return distance;
    }
    private double parseDistance(String distanceText) {
        try {
            // Extract the numerical value from the distance text (e.g., "11.9 km")
            String[] parts = distanceText.split(" ");
            double distanceInKm = Double.parseDouble(parts[0]); // Get the number (e.g., 11.9)
            return distanceInKm * 1000; // Convert km to meters
        } catch (NumberFormatException e) {
            Log.e("DEBUG", "Error parsing distance: " + distanceText, e);
            return 0; // Return 0 if there's an error
        }
    }

    private void updateResultView(TextView resultView, String text) {
        new Handler(Looper.getMainLooper()).post(() -> resultView.setText(text));
    }

}
