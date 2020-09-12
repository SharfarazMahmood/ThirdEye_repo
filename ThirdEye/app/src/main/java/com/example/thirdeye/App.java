package com.example.thirdeye;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.provider.Settings;


public class App extends Application {
    public static final String TAG = "App";
    public static final String CHANNEL_1_ID = "channel1";
    public static final String CHANNEL_2_ID = "channel2";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }


    private void createNotificationChannels() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel1 = new NotificationChannel(
                    CHANNEL_1_ID, "Notifications", NotificationManager.IMPORTANCE_HIGH
            );

            channel1.setDescription("Activity Notification");
            channel1.setSound(Settings.System.DEFAULT_NOTIFICATION_URI , Notification.AUDIO_ATTRIBUTES_DEFAULT);
            channel1.setShowBadge(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel1);
        }
    }


}
