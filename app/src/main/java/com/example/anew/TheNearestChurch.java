package com.example.anew;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

import android.graphics.drawable.BitmapDrawable;
import android.widget.Toast;

import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.toolbox.ImageRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class TheNearestChurch {
    public boolean findedForChurches = false;

    private void SearchText(TextView resultView) {
        new Handler(Looper.getMainLooper()).post(() -> resultView.setText("Searching for churches..."));

    }
    private List<Place_2> allResults = new ArrayList<>();


    public void getChurchCoordinates(String category, double userLat, double userLng,  String apiKey, TextView resultView, RecyclerView resultsContainer) {
        SearchText(resultView);
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" +
                userLat + "," + userLng +
                "&rankby=distance" + // radius in meters
                "&keyword=" + category +
                "&key=" + apiKey;

        RequestQueue queue = Volley.newRequestQueue(resultView.getContext());

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        if (results.length() > 0) {
                            StringBuilder coordinates = new StringBuilder();
                            StringBuilder destinations = new StringBuilder();

                            for (int i = 0; i < results.length(); i++) {
                                JSONObject place = results.getJSONObject(i);
                                JSONObject location = place.getJSONObject("geometry").getJSONObject("location");
                                double lat = location.getDouble("lat");
                                double lng = location.getDouble("lng");

                                if (i > 0) {
                                    destinations.append("|");
                                }
                                destinations.append(lat).append(",").append(lng);

                                Log.e("AAJN", "Processing " + category + ": " + place.getString("name"));
                            }

                            getStreetDistances(resultView.getContext(), userLat, userLng, destinations.toString(), apiKey, coordinates, results, resultView, resultsContainer, category);
                        } else {
                            updateResultView(resultView, "No results found for " + category + " within the radius.");
                        }
                    } catch (Exception e) {
                        updateResultView(resultView, "Error parsing the response.");
                    }
                },
                error -> updateResultView(resultView, "No internet connection")
        );

        queue.add(request);
    }

    private void getStreetDistances(Context context, double userLat, double userLng, String destinations, String apiKey,
                                    StringBuilder coordinates, JSONArray results, TextView resultView, RecyclerView container, String category) {
        String distanceUrl = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=" +
                userLat + "," + userLng +
                "&destinations=" + destinations +
                "&key=" + apiKey;

        RequestQueue queue = Volley.newRequestQueue(context);

        JsonObjectRequest distanceRequest = new JsonObjectRequest(Request.Method.GET, distanceUrl, null,
                response -> {
                    try {
                        JSONArray rows = response.getJSONArray("rows");
                        if (rows.length() > 0) {
                            List<Place_2> places = new ArrayList<>();
                            JSONArray elements = rows.getJSONObject(0).getJSONArray("elements");

                            for (int i = 0; i < elements.length(); i++) {
                                JSONObject element = elements.getJSONObject(i);
                                JSONObject place = results.getJSONObject(i);

                                if (element.getString("status").equals("OK")) {
                                    String distanceText = element.getJSONObject("distance").getString("text");
                                    coordinates.append(place.getString("name")).append(": ")
                                            .append(destinations.split("\\|")[i])
                                            .append(" (").append(distanceText).append(" via street)\n");

                                    JSONObject location = place.getJSONObject("geometry").getJSONObject("location");
                                    double lat = location.getDouble("lat");
                                    double lng = location.getDouble("lng");

                                    String name = place.getString("name");
                                    String photoUrl = getPhotoUrl(place, apiKey);

                                    String numericPart = distanceText.split(" ")[0];
                                    float distanceInKm = Float.parseFloat(numericPart);

                                    places.add(new Place_2(name, photoUrl, lat, lng, distanceText, category));
                                    break;

                                } else {
                                    coordinates.append("Distance unavailable for: ").append(place.getString("name")).append("\n");
                                }
                            }

                            // Add the results to the combined list and refresh the adapter
                            allResults.addAll(places);
                            //PlaceAdapter_2 adapter = new PlaceAdapter_2(context, allResults);
                            //container.setAdapter(adapter);
                        }
                    } catch (JSONException e) {
                        Log.e("DistanceMatrixError", "Error parsing distance response: " + e.getMessage());
                    }

                    updateResultView(resultView, findedForChurches ? "Filtered Results:" : "No results found within the radius");
                },
                error -> Log.e("DistanceMatrixError", "Error fetching distance: " + error.getMessage())
        );

        queue.add(distanceRequest);
    }

    private void updateResultView(TextView resultView, String text) {
        new Handler(Looper.getMainLooper()).post(() -> resultView.setText(text));
    }

    private boolean nameChecker(String name) {
        return !name.contains("Studio") && !name.contains("Ando") && !name.contains("Dili") && !name.contains("Parcheggio") && !name.contains("Anglicana")&& !name.contains("Nativity");
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