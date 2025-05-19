package com.example.anew;

import android.app.Application;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.concurrent.TimeUnit;

public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("MyApp", "App started - checking for permissions and scheduling worker");

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        
        // Configure Firestore settings
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        firestore.setFirestoreSettings(settings);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
            scheduleNotificationWorker();
        } else {
            Log.d("MyApp", "Notification permission not granted, worker not scheduled");
        }
    }

    private void scheduleNotificationWorker() {
        Log.d("MyApp", "Scheduling periodic notification worker");

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                HiWorker.class,
                15,
                TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "HiNotificationWork",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
        );
    }
}
