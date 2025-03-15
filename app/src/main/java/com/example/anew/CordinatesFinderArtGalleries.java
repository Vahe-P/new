package com.example.anew;

import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
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

import java.util.concurrent.atomic.AtomicInteger;

import android.graphics.drawable.BitmapDrawable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.ImageRequest;

public class CordinatesFinderArtGalleries {
    public boolean findedForArtGalleriess=false;
    private void SearchText(TextView resultView) {
        new Handler(Looper.getMainLooper()).post(() -> resultView.setText("Searching for galleries..."));
    }

    public void getArtGalleryCoordinates(double userLat, double userLng, int radius, String apiKey, TextView resultView, LinearLayout resultsContainer) {
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
                            AtomicInteger pendingRequests = new AtomicInteger(results.length());

                            for (int i = 0; i < results.length(); i++) {
                                JSONObject place = results.getJSONObject(i);
                                JSONObject location = place.getJSONObject("geometry").getJSONObject("location");
                                double lat = location.getDouble("lat");
                                double lng = location.getDouble("lng");

                                // Fetch street distance for each church
                                getStreetDistance(userLat, userLng, lat, lng, radius, apiKey, coordinates, place, resultView, pendingRequests, results, resultsContainer);
                            }
                        } else {
                            updateResultView(resultView, "No art galleries found within the radius.");
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
                                   StringBuilder coordinates, JSONObject place, TextView resultView, AtomicInteger pendingRequests, JSONArray results, LinearLayout container) {
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
                                String distanceText = elements.getJSONObject("distance").getString("text"); // E.g., "2.5 km"
                                coordinates.append(place.getString("name")).append(": ")
                                        .append(destLat).append(", ").append(destLng)
                                        .append(" (").append(distanceText).append(" via street)\n");

                                addPlaceToContainer(place, container, apiKey, distanceText, radius);
                            } else {
                                coordinates.append("Distance unavailable for: ").append(place.getString("name")).append("\n");
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
    private void addPlaceToContainer(JSONObject place, LinearLayout container, String apiKey,String distanceText,int radius) {
        try {
            if(radius>=Float.parseFloat(distanceText.substring(0, distanceText.length() - 2)) && nameChecker(place.getString("name"))){
                String name = place.getString("name");
                String photoUrl = getPhotoUrl(place, apiKey);
                findedForArtGalleriess =true;
                LinearLayout buttonLayout = new LinearLayout(container.getContext());
                buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
                buttonLayout.setBackgroundResource(android.R.drawable.btn_default);
                buttonLayout.setPadding(16, 16, 16, 16);
                buttonLayout.setClickable(true);
                buttonLayout.setFocusable(true);

                ImageView imageView = new ImageView(container.getContext());
                int imageSize = 150;
                LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(imageSize, imageSize);
                imageView.setLayoutParams(imageParams);
                imageView.setImageResource(R.drawable.download);

                if (photoUrl != null) {
                    RequestQueue queue = Volley.newRequestQueue(container.getContext());
                    ImageRequest imageRequest = new ImageRequest(photoUrl,
                            response -> imageView.setImageDrawable(new BitmapDrawable(container.getResources(), response)),
                            0, 0, null, null,
                            error -> Log.e("ImageLoadError", "Error loading image: " + error.getMessage()));
                    queue.add(imageRequest);
                }
                LinearLayout textContainer = new LinearLayout(container.getContext());
                textContainer.setOrientation(LinearLayout.VERTICAL);

                TextView textView = new TextView(container.getContext());
                textView.setText(name);
                textView.setTextSize(16);

                TextView distanceView = new TextView(container.getContext());
                distanceView.setText("Distance: " + distanceText);
                distanceView.setTextSize(14);

                textContainer.addView(textView);
                textContainer.addView(distanceView);

                buttonLayout.addView(imageView);
                buttonLayout.addView(textContainer);

                buttonLayout.setOnClickListener(v -> Toast.makeText(container.getContext(), "Clicked: " + name, Toast.LENGTH_SHORT).show());

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
