package net.malahovsky.appforsale.lib.firebase;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MessagingService extends FirebaseMessagingService
{
    @Override
    public void onNewToken(String token)
    {
        getSharedPreferences(getPackageName(), Context.MODE_PRIVATE)
                .edit()
                .putInt("firebase_status", -1)
                .putString("firebase_token", token)
                .apply();

        startService(new Intent(getApplicationContext(), TokenRefreshService.class));
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage)
    {
        Log.e("Log", "onMessageReceived");
        if (remoteMessage.getNotification() != null)
        {

            Intent intent = new Intent("onMessageReceived");

            intent.putExtra("remoteMessage", remoteMessage);

            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

        }
    }
}