package net.malahovsky.appforsale.lib.firebase;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;

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

public class TokenRefreshService extends Service
{
    @Override
    public int onStartCommand(Intent intent, int flags, final int startId)
    {
        final SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);

        if (
               // sharedPreferences.getInt("firebase_status", 0) == -1 &&
            sharedPreferences.getString("firebase_token", null) != null)
        {
            sharedPreferences.edit()
                    .putInt("firebase_status", 0)
                    .apply();

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

            RequestBody body = new FormBody.Builder()
                    .add("app_id", getApplicationContext().getPackageName())
                    .add("device_id", Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID))
                    .add("device_model", Build.BRAND + " " + Build.MODEL)
                    .add("system_version", Build.VERSION.RELEASE)
                    .add("token", sharedPreferences.getString("firebase_token", ""))
                    .build();

            Request request = new Request.Builder()
                    .url(uri.getScheme() + "://" + uri.getHost() + "/bitrix/tools/mlab_appforsale/register_device.php")
                    .headers(headers)
                    .post(body)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(Call call, IOException e)
                {
//                    sharedPreferences.edit()
//                            .putInt("firebase_status", -1)
//                            .apply();

                    stopSelf(startId);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException
                {

                    if (response.body().string().equals("{\"response\":1}"))
                    {
//                        sharedPreferences.edit()
//                                .putInt("firebase_status", 1)
//                                .remove("firebase_token")
//                                .apply();


                        stopSelf(startId);
                    }
                    else
                    {
                        onFailure(call, new IOException());
                    }

                }
            });

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