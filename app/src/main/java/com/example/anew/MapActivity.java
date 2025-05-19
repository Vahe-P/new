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
import com.google.android.gms.maps.model.LatLngBounds;
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
    private LatLng startLocation;
    private LatLng endLocation;
    private ProgressBar progressBar;
    private TextView routeInfoText;
    private RequestQueue requestQueue;
    private static final String ORS_API_KEY = "5b3ce3597851110001cf624857e40ce940834ca4ac28232cdf50be2d";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.your_color));
        }
        setContentView(R.layout.activity_map2);

        progressBar = findViewById(R.id.progressBar);
        routeInfoText = findViewById(R.id.routeInfoText);
        requestQueue = Volley.newRequestQueue(this);

        // Get coordinates from Intent
        double userLat = getIntent().getDoubleExtra("userLat", 40.1792);
        double userLng = getIntent().getDoubleExtra("userLng", 44.4991);
        double destLat = getIntent().getDoubleExtra("destLat", 40.7899);
        double destLng = getIntent().getDoubleExtra("destLng", 43.8475);

        startLocation = new LatLng(userLat, userLng);
        endLocation = new LatLng(destLat, destLng);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Add markers for start and end points
        mMap.addMarker(new MarkerOptions()
                .position(startLocation)
                .title("Start")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

        mMap.addMarker(new MarkerOptions()
                .position(endLocation)
                .title("Destination")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        // Move camera to show both points
        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(startLocation)
                .include(endLocation)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));

        progressBar.setVisibility(View.VISIBLE);
        routeInfoText.setVisibility(View.GONE);

        // Get the route
        getRoute();
    }

    private void getRoute() {
        String url = String.format(
            "https://api.openrouteservice.org/v2/directions/driving-car?api_key=%s&start=%f,%f&end=%f,%f",
            ORS_API_KEY,
            startLocation.longitude, startLocation.latitude,
            endLocation.longitude, endLocation.latitude
        );

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
            response -> {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    JSONArray features = jsonResponse.getJSONArray("features");
                    
                    if (features.length() > 0) {
                        JSONObject feature = features.getJSONObject(0);
                        JSONObject properties = feature.getJSONObject("properties");
                        JSONObject summary = properties.getJSONObject("summary");
                        
                        // Get distance and duration
                        double distance = summary.getDouble("distance") / 1000.0; // Convert to km
                        double duration = summary.getDouble("duration") / 60.0; // Convert to minutes
                        
                        // Update route info text
                        String routeInfo = String.format("Distance: %.1f km\nDuration: %.0f min", distance, duration);
                        routeInfoText.setText(routeInfo);
                        routeInfoText.setVisibility(View.VISIBLE);
                        
                        // Get and draw the route
                        JSONObject geometry = feature.getJSONObject("geometry");
                        JSONArray coordinates = geometry.getJSONArray("coordinates");
                        List<LatLng> points = new ArrayList<>();
                        
                        for (int i = 0; i < coordinates.length(); i++) {
                            JSONArray coord = coordinates.getJSONArray(i);
                            double lng = coord.getDouble(0);
                            double lat = coord.getDouble(1);
                            points.add(new LatLng(lat, lng));
                        }
                        
                        // Draw the route
                        drawRouteOnMap(points);
                    }
                } catch (Exception e) {
                    Log.e("RouteError", "Error parsing route: " + e.getMessage());
                    Toast.makeText(this, "Error getting route", Toast.LENGTH_SHORT).show();
                } finally {
                    progressBar.setVisibility(View.GONE);
                }
            },
            error -> {
                Log.e("RouteError", "Error getting route: " + error.getMessage());
                Toast.makeText(this, "Error getting route", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        );

        requestQueue.add(stringRequest);
    }

    private void drawRouteOnMap(List<LatLng> points) {
            // Draw shadow line
            mMap.addPolyline(new PolylineOptions()
                    .addAll(points)
                    .color(Color.parseColor("#80000000"))  // semi-transparent black
                    .width(18)
                    .geodesic(true));

            // Draw main line
            mMap.addPolyline(new PolylineOptions()
                    .addAll(points)
                    .color(Color.parseColor("#FF4081"))  // vibrant pink
                    .width(15)
                    .startCap(new RoundCap())
                    .endCap(new RoundCap())
                    .pattern(Arrays.asList(new Dot(), new Gap(20)))
                    .geodesic(true));
    }
}
