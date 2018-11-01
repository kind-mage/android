package net.malahovsky.appforsale.lib.location;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.IBinder;
import android.provider.Settings;

import com.google.android.gms.location.LocationResult;

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
    @Override
    public int onStartCommand(Intent intent, int flags, final int startId)
    {
        if (LocationResult.hasResult(intent))
        {
            LocationResult locationResult = LocationResult.extractResult(intent);
            Location location = locationResult.getLastLocation();
            if (location != null)
            {
                OkHttpClient httpClient = new OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .cookieJar(new CookieJarHandler())
                        .build();

                Uri uri = Uri.parse(getString(R.string.app_url));

                Headers headers = new Headers.Builder()
                        .add("bm-device", "android")
                        .add("bm-device-id", Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID))
                        .add("bm-api-version", "2")
                        .build();

                final RequestBody body = new FormBody.Builder()
                        .add("longitude", Double.toString(location.getLongitude()))
                        .add("latitude", Double.toString(location.getLatitude()))
                        .add("accuracy", Double.toString(location.getAccuracy()))
                        .add("timestamp", Long.toString(location.getTime()))
                        .add("app_id", getApplicationContext().getPackageName())
                        .add("device_id", Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID))
                        .build();

                final Request request = new Request.Builder()
                        .url(uri.getScheme() + "://" + uri.getHost() + "/bitrix/tools/mlab_appforsale/set_current_position.php")
                        .headers(headers)
                        .post(body)
                        .build();

                httpClient.newCall(request).enqueue(new Callback() {

                    @Override
                    public void onFailure(Call call, IOException e)
                    {
                        stopSelf(startId);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException
                    {
                        stopSelf(startId);
                    }

                });
            }
            else
            {
                stopSelf(startId);
            }
        }
        else
        {
            stopSelf(startId);
        }

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }
}