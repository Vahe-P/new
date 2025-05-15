package com.example.anew;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CordinatesFinderChurches {
    private List<Place_2> allResults = new ArrayList<>();
    private Set<String> addedPlaceKeys = new HashSet<>();

    public void getChurchCoordinates(String category, double userLat, double userLng, int radius, String apiKey, RecyclerView resultsContainer) {
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=" + userLat + "," + userLng +
                "&radius=" + (radius * 1000) +
                "&keyword=" + category +
                "&key=" + apiKey;

        RequestQueue queue = Volley.newRequestQueue(resultsContainer.getContext());
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        if (results.length() > 0) {
                            List<Place_2> places = new ArrayList<>();
                            for (int i = 0; i < results.length(); i++) {
                                JSONObject place = results.getJSONObject(i);
                                String name = place.getString("name");
                                JSONObject location = place.getJSONObject("geometry").getJSONObject("location");
                                double lat = location.getDouble("lat");
                                double lng = location.getDouble("lng");
                                
                                // Create a unique key for each place based on name and location
                                String placeKey = name + "_" + lat + "_" + lng;
                                
                                // Skip if this place is already added
                                if (addedPlaceKeys.contains(placeKey)) {
                                    continue;
                                }
                                
                                String photoReference = "";
                                if (place.has("photos")) {
                                    JSONArray photos = place.getJSONArray("photos");
                                    if (photos.length() > 0) {
                                        photoReference = photos.getJSONObject(0).getString("photo_reference");
                                    }
                                }
                                String imageUrl = "https://maps.googleapis.com/maps/api/place/photo" +
                                        "?maxwidth=400" +
                                        "&photo_reference=" + photoReference +
                                        "&key=" + apiKey;

                                // Calculate distance text
                                float[] results_2 = new float[1];
                                android.location.Location.distanceBetween(userLat, userLng, lat, lng, results_2);
                                String distanceText = String.format("%.1f km", results_2[0] / 1000);
                                
                                places.add(new Place_2(name, imageUrl, lat, lng, distanceText, category));
                                addedPlaceKeys.add(placeKey);
                            }
                            
                            // Add the results to the combined list and refresh the adapter
                            allResults.addAll(places);
                            new Handler(Looper.getMainLooper()).post(() -> {
                                PlaceAdapter_2 adapter = new PlaceAdapter_2(resultsContainer.getContext(), allResults, userLat, userLng);
                                resultsContainer.setAdapter(adapter);
                            });
                        } else {
                            showToast(resultsContainer, "No results found for " + category);
                        }
                    } catch (Exception e) {
                        showToast(resultsContainer, "Error parsing the response");
                    }
                },
                error -> showToast(resultsContainer, "No internet connection"));

        queue.add(request);
    }

    private void showToast(RecyclerView resultsContainer, String message) {
        new Handler(Looper.getMainLooper()).post(() -> 
            Toast.makeText(resultsContainer.getContext(), message, Toast.LENGTH_SHORT).show()
        );
    }

    public void clearResults() {
        allResults.clear();
        addedPlaceKeys.clear();
    }
}