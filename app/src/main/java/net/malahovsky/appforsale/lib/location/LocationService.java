package net.malahovsky.appforsale.lib.location;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import net.malahovsky.appforsale.R;
import net.malahovsky.appforsale.lib.cookie.CookieJarHandler;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LocationService extends Service
{
    private FusedLocationProviderClient locationProviderClient;
    private LocationCallback locationCallback;

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate()
    {
        super.onCreate();

        String channel_id = "";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            channel_id = "location";
            NotificationChannel channel = new NotificationChannel(channel_id, "Location channel", NotificationManager.IMPORTANCE_DEFAULT);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:" + getPackageName())), PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this, channel_id)
                .setContentTitle("Приложение \"" + getString(R.string.app_name) + "\" выполняется")
                .setContentText("Нажмите, чтобы получить дополнительные данные...")
                .setSmallIcon(R.mipmap.ic_stat_name)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        locationProviderClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());

        locationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(LocationResult locationResult)
            {
                Location location = locationResult.getLastLocation();
                if (location != null)
                {
                    OkHttpClient httpClient = new OkHttpClient.Builder()
                            .connectTimeout(30, TimeUnit.SECONDS)
                            .cookieJar(new CookieJarHandler(getApplicationContext()))
                            .build();

                    Uri uri = Uri.parse(getString(R.string.app_url));

                    Headers headers = new Headers.Builder()
                            .add("bm-device", "android")
                            .add("bm-device-id", Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID))
                            .add("bm-api-version", "2")
                            .build();

                    RequestBody body = new FormBody.Builder()
                            .add("longitude", Double.toString(location.getLongitude()))
                            .add("latitude", Double.toString(location.getLatitude()))
                            .add("accuracy", Double.toString(location.getAccuracy()))
                            .add("timestamp", Long.toString(location.getTime()))
                            .add("app_id", getApplicationContext().getPackageName())
                            .add("device_id", Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID))
                            .build();

                    Request request = new Request.Builder()
                            .url(uri.getScheme() + "://" + uri.getHost() + "/bitrix/tools/mlab_appforsale/set_current_position.php")
                            .headers(headers)
                            .post(body)
                            .build();

                    httpClient.newCall(request).enqueue(new Callback() {

                        @Override
                        public void onFailure(Call call, IOException e)
                        {

                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException
                        {

                        }

                    });

                }

            }

        };

        LocationRequest locationRequest = new LocationRequest()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(15 * 60 * 1000)
                .setFastestInterval(15 * 60 * 1000);

        locationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if (locationProviderClient != null && locationCallback != null)
        {
            locationProviderClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }
}