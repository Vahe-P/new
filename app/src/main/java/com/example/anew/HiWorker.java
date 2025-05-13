package com.example.anew;

import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.List;
import java.util.Random;

public class HiWorker extends Worker {

    // Array of slogans
    private static final String[] SLOGANS = {
            "Wander More, Worry Less.",
            "Adventure Awaits – We'll Notify You!",
            "Discover Hidden Gems, One Ping at a Time.",
            "Let Curiosity Guide You – We’ll Handle the Rest.",
            "Unlock the World Around You.",
            "Your Pocket Guide to the Globe.",
            "Explore More with Every Notification.",
            "Where Next? We’ve Got Ideas.",
            "Wanderlust on Demand.",
            "A World of Wonders – Just a Tap Away."
    };

    public HiWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("HiWorker", "doWork() called");
        if (isAppInForeground(getApplicationContext())) {
            Log.d("HiWorker", "App is in foreground. Skipping notification.");
            return Result.success();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.w("HiWorker", "Notification permission not granted");
                return Result.success();
            }
        }

        // Create notification channel (keep existing code)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "hi_channel",
                    "Hi Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getApplicationContext().getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                Log.d("HiWorker", "Notification channel created");
            }
        }

        // Create intent to open MainActivity when notification is clicked
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Select random slogan
        Random random = new Random();
        String randomSlogan = SLOGANS[random.nextInt(SLOGANS.length)];

        // Build notification with click action
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "hi_channel")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Explore Now")
                .setContentText(randomSlogan)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)  // This makes the notification clickable
                .setAutoCancel(true);  // Notification disappears when clicked

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());

        Log.d("HiWorker", "Notification sent with slogan: " + randomSlogan);
        return Result.success();
    }
    private boolean isAppInForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) return false;

        final String packageName = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                    appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

}