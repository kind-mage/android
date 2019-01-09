package net.malahovsky.appforsale.lib.location;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.ActivityCompat;

import net.malahovsky.appforsale.XWalkActivity;

public class LocationReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED))
        {
            if (XWalkActivity.hasPermissions(context, new String[] { Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION }))
            {
                ActivityCompat.startForegroundService(context, new Intent(context, LocationService.class));
            }
        }
    }
}