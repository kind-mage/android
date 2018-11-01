package net.malahovsky.appforsale.lib.location;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

public class LocationProvider
{
    private static LocationProvider instance;

    private LocationRequest locationRequest;
    private PendingIntent pendingIntent;
    private FusedLocationProviderClient locationProviderClient;

    public static LocationProvider getInstance(Context context)
    {
        if (instance == null)
        {
            instance = new LocationProvider(context);
        }

        return instance;
    }

    public LocationProvider(Context context)
    {
        locationRequest = LocationRequest.create()
                .setInterval(15*60*1000)
                .setFastestInterval(15*60*1000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        pendingIntent = PendingIntent.getService(context, 0, new Intent(context, LocationService.class), PendingIntent.FLAG_CANCEL_CURRENT);

        locationProviderClient = LocationServices.getFusedLocationProviderClient(context);
    }

    public void requestLocationUpdates()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            ActivityCompat.checkSelfPermission(locationProviderClient.getApplicationContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        locationProviderClient.requestLocationUpdates(locationRequest, pendingIntent);
    }
}