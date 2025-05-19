package com.example.anew;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageButton;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import android.view.Window;

public class SourcesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.your_color));
        }
        setContentView(R.layout.activity_sources);

        // Set up back button
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // Set up Google Maps card
        CardView googleMapsCard = findViewById(R.id.googleMapsCard);
        googleMapsCard.setOnClickListener(v -> {
            // Open Google Maps app or website
            Uri gmmIntentUri = Uri.parse("geo:0,0?z=10");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            
            // If Google Maps app is not installed, open in browser
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, 
                    Uri.parse("https://www.google.com/maps"));
                startActivity(browserIntent);
            }
        });

        // Set up OpenRouteService card
        CardView orsCard = findViewById(R.id.orsCard);
        orsCard.setOnClickListener(v -> {
            // Open OpenRouteService website
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, 
                Uri.parse("https://openrouteservice.org"));
            startActivity(browserIntent);
        });
    }
} 