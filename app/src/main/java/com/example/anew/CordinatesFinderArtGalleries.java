package com.example.anew;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class CordinatesFinderArtGalleries {
    private static final String API_KEY = "AIzaSyDfylRP2UhEe-kcDiigAiECbCqL1HAJ3I4"; 
    public void getArtGalleryCoordinates(double latitude, double longitude, int radiusInKm, TextView coordinatesView, LinearLayout resultsContainer) {
        if (coordinatesView == null || resultsContainer == null) {
            Log.e("Error", "TextView or LinearLayout is null");
            return;
        }

        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=" + latitude + "," + longitude +
                "&radius=" + (radiusInKm * 1000) +
                "&keyword=art gallery" +
                "&key=" + API_KEY;

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                coordinatesView.post(() ->
                        Toast.makeText(coordinatesView.getContext(), "Failed to fetch art places. Check your connection.", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonResponse = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(jsonResponse);
                        JSONArray results = jsonObject.optJSONArray("results");
                        if (results == null || results.length() == 0) {
                            coordinatesView.post(() ->
                                    Toast.makeText(coordinatesView.getContext(), "No results found.", Toast.LENGTH_SHORT).show()
                            );
                            return;
                        }

                        StringBuilder coordinatesBuilder = new StringBuilder();
                        coordinatesBuilder.append("Art-related places within ").append(radiusInKm).append(" km:\n");

                        for (int i = 0; i < results.length(); i++) {
                            try {
                                JSONObject place = results.getJSONObject(i);
                                JSONObject location = place.getJSONObject("geometry").getJSONObject("location");
                                String placeName = place.optString("name", "Art Place " + (i + 1));
                                double lat = location.getDouble("lat");
                                double lng = location.getDouble("lng");
                                if (placeName.toLowerCase().contains("hotel") ||
                                        placeName.toLowerCase().contains("hostel") ||
                                        placeName.toLowerCase().contains("studio")) {
                                    continue;
                                }

                                coordinatesBuilder.append(placeName).append(": ")
                                        .append(lat).append(", ").append(lng).append("\n");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        coordinatesView.post(() -> coordinatesView.setText(coordinatesBuilder.toString()));
                        createButtonsForGalleries(results, resultsContainer,coordinatesView);
                        
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    coordinatesView.post(() ->
                            Toast.makeText(coordinatesView.getContext(), "Error fetching data: " + response.message(), Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private void createButtonsForGalleries(final JSONArray results, final LinearLayout container, final TextView coordinatesView) {
        try {
            container.removeAllViews();

            if (results == null || results.length() == 0) {
                Log.e("ERROR", "No valid results available to create buttons");
                return;
            }
            for (int i = 0; i < results.length(); i++) {
                try {
                    JSONObject place = results.getJSONObject(i);
                    String name = place.getString("name");

                    Log.d("DEBUG", "Creating button for: " + name);
                    Button button = new Button(container.getContext());

                    button.setText(name);
                    button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.download, 0, 0, 0); // Add image as needed
                    button.setPadding(16, 16, 16, 16);
                    button.setTextSize(16);

                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    params.setMargins(0, 0, 0, 10); // Add margin between buttons
                    button.setLayoutParams(params);
                    
                    container.post(() -> container.addView(button));

                    Log.d("DEBUG", "Button created for: " + name);
                } catch (JSONException e) {
                    Log.e("ERROR", "Error processing result at index " + i, e);
                    continue; // Skip this result and move to the next
                }
            }

            coordinatesView.post(() -> coordinatesView.setText("Here We Go"));

            container.requestLayout();
            container.invalidate();

        } catch (Exception e) {
            Log.e("ERROR", "Error creating buttons for galleries", e);
        }
    }
}
