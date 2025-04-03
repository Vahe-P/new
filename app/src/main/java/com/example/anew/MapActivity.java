package com.example.anew;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private LatLng startLocation = new LatLng(40.1792, 44.4991);  // Example: Yerevan
    private LatLng endLocation = new LatLng(40.7899, 43.8475);    // Example: Gyumri
    private final String API_KEY = "AIzaSyDfylRP2UhEe-kcDiigAiECbCqL1HAJ3I4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map2);

        // Get intent extras
        double userLat = getIntent().getDoubleExtra("userLat", 40.1792); // Default: Yerevan
        double userLng = getIntent().getDoubleExtra("userLng", 44.4991);
        double destLat = getIntent().getDoubleExtra("destLat", 40.7899); // Default: Gyumri
        double destLng = getIntent().getDoubleExtra("destLng", 43.8475);

        // Update startLocation and endLocation
        startLocation = new LatLng(userLat, userLng);
        endLocation = new LatLng(destLat, destLng);

        // Initialize Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Add markers for start and destination
        mMap.addMarker(new MarkerOptions().position(startLocation).title("Start"));
        mMap.addMarker(new MarkerOptions().position(endLocation).title("Destination"));

        // Move camera to start location with a suitable zoom level
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startLocation, 10));

        // Fetch and draw the route
        getRoute();
    }

    private void getRoute() {
        String url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=" + startLocation.latitude + "," + startLocation.longitude +
                "&destination=" + endLocation.latitude + "," + endLocation.longitude +
                "&mode=driving" +
                "&key=" + API_KEY;

        Log.d("DebugURL", "Request URL: " + url);

        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("MapResponse", "Route response: " + response); // Debug log
                        drawRoute(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("MapError", "Failed to get route: " + error.getMessage());
                    }
                });

        queue.add(stringRequest);
    }

    private void drawRoute(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONArray routes = jsonObject.getJSONArray("routes");

            if (routes.length() > 0) {
                JSONObject route = routes.getJSONObject(0);
                JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                String encodedPolyline = overviewPolyline.getString("points");

                // Decode the polyline
                List<LatLng> polylinePoints = decodePolyline(encodedPolyline);

                // Clear previous routes & markers
                mMap.clear();

                // Add start and end markers again
                mMap.addMarker(new MarkerOptions().position(startLocation).title("Start"));
                mMap.addMarker(new MarkerOptions().position(endLocation).title("Destination"));

                // Draw the polyline on the map
                PolylineOptions polylineOptions = new PolylineOptions()
                        .addAll(polylinePoints)
                        .color(Color.RED) // Ensure RED color is visible
                        .width(12)
                        .geodesic(true); // Smooth curve

                mMap.addPolyline(polylineOptions);
                Log.d("MapDebug", "Polyline drawn with " + polylinePoints.size() + " points");

                // Move camera to fit the entire route
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(
                        new com.google.android.gms.maps.model.LatLngBounds.Builder()
                                .include(startLocation)
                                .include(endLocation)
                                .build(), 100));

            } else {
                Log.e("MapError", "No routes found in the response");
            }
        } catch (Exception e) {
            Log.e("MapError", "Failed to parse route: " + e.getMessage());
        }
    }



    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> polyline = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1F) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1F) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((lat / 1E5), (lng / 1E5));
            polyline.add(p);
        }

        return polyline;
    }
}