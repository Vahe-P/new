package com.example.anew;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private LatLng startLocation = new LatLng(40.1792, 44.4991);  // Example: Yerevan
    private LatLng endLocation = new LatLng(40.7899, 43.8475);    // Example: Gyumri
    private final String API_KEY = "AIzaSyD3aOclf9YRAKK9D0VfQPp0NLsGDCJ9xFU";
    private ProgressBar progressBar;
    private TextView routeInfoText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.your_color));
        }
        setContentView(R.layout.activity_map2);

        // Initialize views
        progressBar = findViewById(R.id.progressBar);
        routeInfoText = findViewById(R.id.routeInfoText);

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
        mMap.addMarker(new MarkerOptions()
                .position(startLocation)
                .title("Start")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

        mMap.addMarker(new MarkerOptions()
                .position(endLocation)
                .title("Destination")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        // Move camera to start location with a suitable zoom level
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startLocation, 10));

        // Show loading indicator
        progressBar.setVisibility(View.VISIBLE);
        routeInfoText.setVisibility(View.GONE);

        // Fetch and draw the route
        getRoute();
    }

    private void getRoute() {
        // Show loading indicator
        progressBar.setVisibility(View.VISIBLE);
        routeInfoText.setVisibility(View.GONE);

        // Format the URL with proper encoding
        String origin = String.format("%f,%f", startLocation.latitude, startLocation.longitude);
        String destination = String.format("%f,%f", endLocation.latitude, endLocation.longitude);
        
        String url = String.format("https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=%s" +
                "&destination=%s" +
                "&mode=driving" +
                "&alternatives=true" +
                "&key=%s",
                origin, destination, API_KEY);
        Log.d("FinalURL", url);

        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        progressBar.setVisibility(View.GONE);
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            String status = jsonResponse.getString("status");
                            
                            if (status.equals("OK")) {
                                drawRoute(response);
                            } else {
                                String errorMessage;
                                switch (status) {
                                    case "ZERO_RESULTS":
                                        errorMessage = "No route found between these locations";
                                        break;
                                    case "NOT_FOUND":
                                        errorMessage = "One or both locations could not be found";
                                        break;
                                    case "REQUEST_DENIED":
                                        errorMessage = "For now unable to find the route";
                                        break;
                                    default:
                                        errorMessage = "Could not find route: " + status;
                                }
                                Toast.makeText(MapActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                routeInfoText.setText(errorMessage);
                                routeInfoText.setVisibility(View.VISIBLE);
                            }
                        } catch (Exception e) {
                            String errorMessage = "Error processing route: " + e.getMessage();
                            Toast.makeText(MapActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                            routeInfoText.setText(errorMessage);
                            routeInfoText.setVisibility(View.VISIBLE);
                            Log.e("MapError", errorMessage);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressBar.setVisibility(View.GONE);
                        String errorMessage = "Failed to get route";
                        if (error.networkResponse != null) {
                            try {
                                String errorResponse = new String(error.networkResponse.data);
                                JSONObject jsonError = new JSONObject(errorResponse);
                                errorMessage = jsonError.optString("error_message", errorMessage);
                            } catch (Exception e) {
                                Log.e("MapError", "Error parsing error response: " + e.getMessage());
                            }
                        }
                        Toast.makeText(MapActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        routeInfoText.setText(errorMessage);
                        routeInfoText.setVisibility(View.VISIBLE);
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
                // Find the shortest route
                JSONObject shortestRoute = routes.getJSONObject(0);
                double shortestDistance = Double.MAX_VALUE;

                for (int i = 0; i < routes.length(); i++) {
                    JSONObject route = routes.getJSONObject(i);
                    JSONArray legs = route.getJSONArray("legs");
                    if (legs.length() > 0) {
                        JSONObject leg = legs.getJSONObject(0);
                        double distance = leg.getJSONObject("distance").getDouble("value");
                        if (distance < shortestDistance) {
                            shortestDistance = distance;
                            shortestRoute = route;
                        }
                    }
                }

                JSONObject overviewPolyline = shortestRoute.getJSONObject("overview_polyline");
                String encodedPolyline = overviewPolyline.getString("points");

                // Get route information
                JSONArray legs = shortestRoute.getJSONArray("legs");
                if (legs.length() > 0) {
                    JSONObject leg = legs.getJSONObject(0);
                    String distance = leg.getJSONObject("distance").getString("text");
                    String duration = leg.getJSONObject("duration").getString("text");
                    routeInfoText.setText("Distance: " + distance + "\nDuration: " + duration);
                    routeInfoText.setVisibility(View.VISIBLE);
                }

                // Decode the polyline
                List<LatLng> polylinePoints = decodePolyline(encodedPolyline);

                // Clear previous routes & markers
                mMap.clear();

                // Add start and end markers again
                mMap.addMarker(new MarkerOptions()
                        .position(startLocation)
                        .title("Start")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

                mMap.addMarker(new MarkerOptions()
                        .position(endLocation)
                        .title("Destination")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

                // Draw the polyline on the map with gradient color
                PolylineOptions polylineOptions = new PolylineOptions()
                        .addAll(polylinePoints)
                        .color(Color.parseColor("#FF4081"))  // Using a vibrant pink color
                        .width(15)  // Increased width for better visibility
                        .startCap(new RoundCap())
                        .endCap(new RoundCap())
                        .geodesic(true)
                        .pattern(Arrays.asList(new Dot(), new Gap(20)));  // Adding a dotted pattern

                Polyline polyline = mMap.addPolyline(polylineOptions);

                // Add a shadow effect by drawing a slightly thicker line underneath
                PolylineOptions shadowOptions = new PolylineOptions()
                        .addAll(polylinePoints)
                        .color(Color.parseColor("#80000000"))  // Semi-transparent black
                        .width(18)  // Slightly thicker than the main line
                        .geodesic(true);
                mMap.addPolyline(shadowOptions);

                // Move camera to fit the entire route with padding
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(
                        new com.google.android.gms.maps.model.LatLngBounds.Builder()
                                .include(startLocation)
                                .include(endLocation)
                                .build(), 150));  // Increased padding for better view

            } else {
                String errorMessage = "No route found between these locations";
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                routeInfoText.setText(errorMessage);
                routeInfoText.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            String errorMessage = "Error drawing route: " + e.getMessage();
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            routeInfoText.setText(errorMessage);
            routeInfoText.setVisibility(View.VISIBLE);
            Log.e("MapError", errorMessage);
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