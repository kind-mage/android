package net.malahovsky.appforsale;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.webkit.ValueCallback;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.JsonObject;
import com.google.zxing.integration.android.IntentIntegrator;

import net.malahovsky.appforsale.lib.firebase.TokenRefreshService;
import net.malahovsky.appforsale.lib.location.LocationService;

import org.xwalk.core.XWalkActivityDelegate;
import org.xwalk.core.XWalkJavascriptResult;
import org.xwalk.core.XWalkNavigationHistory;
import org.xwalk.core.XWalkResourceClient;
import org.xwalk.core.XWalkUIClient;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class XWalkActivity extends AppCompatActivity
{

    private JsonObject mCallback;

    private static final int PERMISSION_REQUEST_CAMERA = 1001;
    private static final int PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 1002;
    private static final int PERMISSION_REQUEST_QR_CODE = 1003;
    private static final int PERMISSION_REQUEST_READ_CONTACTS = 1004;

    private static final int FILE_REQUEST_CODE = 1001;
    private static final int LOCATION_SETTINGS_REQUEST_CODE = 1002;

    private ValueCallback<Uri> mUploadFile;
    private File mImageFile;
    private File mVideoFile;




    private BroadcastReceiver onMessageReceived;


    private Toolbar toolBar;
    private DrawerLayout drawer;

    private XWalkView xWalkMain;
    private FrameLayout layer;
    private ProgressBar progressBar;

    private Timer timer;

    private String titleText = "#ffffff";

    private XWalkActivityDelegate mActivityDelegate;

    private XWalkView xWalkMenu;

    private ActionBarDrawerToggle toggle;



    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xwalk);

        new Handler().postDelayed(new Runnable() {

            public void run()
            {
                ImageView splash = (ImageView) findViewById(R.id.splash);
                if (splash != null)
                {
                    splash.setVisibility(View.INVISIBLE);
                }
            }

        }, 3000);

        toolBar = (Toolbar) findViewById(R.id.toolBar);
        setSupportActionBar(toolBar);



        drawer = (DrawerLayout) findViewById(R.id.drawerLayout);
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        xWalkMain = (XWalkView) findViewById(R.id.xWalkMain);
        xWalkMain.getSettings().setSaveFormData(false);



        xWalkMenu = (XWalkView) findViewById(R.id.xWalkMenu);

        layer = (FrameLayout) findViewById(R.id.layer);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        xWalkMain.setResourceClient(new XWalkResourceClient(xWalkMain) {

            @Override
            public void onDocumentLoadedInFrame(org.xwalk.core.XWalkView view, long frameId) {
                if (timer != null) {
                    timer.cancel();
                }

                if (view.getTag() != null && view.getTag().equals("clear")) {
                    view.getNavigationHistory().clear();
                    view.setTag(null);
                }

                if (view.getNavigationHistory().canGoBack()) {
                    if (toggle != null) {
                        toggle.setDrawerIndicatorEnabled(false);
                    } else {
                        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    }
                } else {
                    if (toggle != null) {
                        toggle.setDrawerIndicatorEnabled(true);
                    } else {
                        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                    }
                }

                if (!view.getTitle().startsWith(Uri.parse(getString(R.string.app_url)).getHost()))
                {
                    setTitle(view.getTitle());
                }

                if (layer != null && layer.getVisibility() == View.VISIBLE)
                {
                    fadeOut(layer);
                }

                if (progressBar != null)
                {
                    progressBar.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onReceivedSslError(org.xwalk.core.XWalkView view, final ValueCallback<Boolean> callback, SslError error)
            {
                new AlertDialog.Builder(XWalkActivity.this)
                        .setIcon(getApplicationInfo().icon)
                        .setTitle(getApplicationInfo().labelRes)
                        .setMessage(R.string.notification_error_ssl_cert_invalid)
                        .setPositiveButton(R.string.ssl_continue, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialogInterface, int i)
                            {
                                callback.onReceiveValue(true);
                            }

                        })
                        .setNegativeButton(R.string.ssl_cancel, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialogInterface, int i)
                            {
                                callback.onReceiveValue(false);
                            }

                        })
                        .setCancelable(false)
                        .create()
                        .show();
            }

            @Override
            public boolean shouldOverrideUrlLoading(org.xwalk.core.XWalkView view, String url)
            {
                Uri uri = Uri.parse(url);

                if (uri.getScheme().equals("http") || uri.getScheme().equals("https"))
                {
                    loadPageBlank(uri.toString(), "");
                    return true;
                }
                else
                {
                    return super.shouldOverrideUrlLoading(view, url);
                }
            }

            @Override
            public void onProgressChanged(org.xwalk.core.XWalkView view, int progressInPercent)
            {
                progressBar.setProgress(progressInPercent);
            }

        });

        xWalkMain.setUIClient(new XWalkUIClient(xWalkMain) {

            @Override
            public boolean onCreateWindowRequested(final org.xwalk.core.XWalkView view, InitiateBy initiator, ValueCallback<org.xwalk.core.XWalkView> callback)
            {
                View _page = LayoutInflater.from(XWalkActivity.this).inflate(R.layout.content_xwalk, null);

                final ProgressBar _progressBar = _page.findViewById(R.id.progressBar);
                final FrameLayout _layer = _page.findViewById(R.id.layer);
                final org.xwalk.core.XWalkView _xWalkView = _page.findViewById(R.id.xWalkView);

                final Timer _timer = new Timer();
                _timer.schedule(new TimerTask() {

                    @Override
                    public void run()
                    {

                            runOnUiThread(new Runnable() {

                                @Override
                                public void run()
                                {
                                    _progressBar.setVisibility(View.VISIBLE);
                                }

                            });

                    }

                }, 2000);

                final Dialog dialog = new Dialog(XWalkActivity.this);
                dialog.setContentView(_page);
                dialog.getWindow().setDimAmount(0);
                dialog.show();

                _xWalkView.setUIClient(new XWalkUIClient(_xWalkView) {

                    @Override
                    public void onPageLoadStopped(org.xwalk.core.XWalkView view, String url, LoadStatus status)
                    {
                        if (status == LoadStatus.FINISHED)
                        {
                            _timer.cancel();

                            _progressBar.setVisibility(View.INVISIBLE);
                            _progressBar.setVisibility(View.VISIBLE);
                            fadeOut(_layer);
                        }
                    }

                    @Override
                    public void onJavascriptCloseWindow(org.xwalk.core.XWalkView view)
                    {
                        _timer.cancel();
                        dialog.dismiss();
                    }

                });

                callback.onReceiveValue(_xWalkView);

                return true;
            }

            @Override
            public boolean onJsAlert(org.xwalk.core.XWalkView view, String url, String message, final XWalkJavascriptResult result)
            {
                new AlertDialog.Builder(XWalkActivity.this)
                        .setIcon(getApplicationInfo().icon)
                        .setTitle(getApplicationInfo().labelRes)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialogInterface, int i)
                            {
                                result.confirm();
                            }

                        })
                        .setCancelable(false)
                        .create()
                        .show();

                return true;
            }

            @Override
            public void openFileChooser(org.xwalk.core.XWalkView view, ValueCallback<Uri> uploadFile, String acceptType, String capture)
            {
                mUploadFile = uploadFile;

                String[] PERMISSIONS = { Manifest.permission.CAMERA };
                if (!hasPermissions(XWalkActivity.this, PERMISSIONS))
                {
                    ActivityCompat.requestPermissions(XWalkActivity.this, PERMISSIONS, PERMISSION_REQUEST_CAMERA);
                }
                else
                {
                    openFile();
                }
            }

            @Override
            public void onPageLoadStopped(org.xwalk.core.XWalkView view, String url, LoadStatus status)
            {
                if (url != null && !url.equals(""))
                {
                    switch (status)
                    {
                        case FAILED:
                            new AlertDialog.Builder(XWalkActivity.this)
                                    .setIcon(getApplicationInfo().icon)
                                    .setTitle(getApplicationInfo().labelRes)
                                    .setMessage("Вы не подключены к Интернету")
                                    .setPositiveButton("Повторить", new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i)
                                        {
                                            xWalkMain.reload(org.xwalk.core.XWalkView.RELOAD_NORMAL);
                                        }

                                    })
                                    .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i)
                                        {
                                            onBackPressed();
                                        }

                                    })
                                    .setCancelable(false)
                                    .create()
                                    .show();

                            break;
                    }
                }
            }
        });

        mActivityDelegate = new XWalkActivityDelegate(this,

                new Runnable() { public void run() { finish(); }},

                new Runnable() { public void run() {

                    checkIntent();


                }}
        );

        //        if (getSharedPreferences(getPackageName(), Context.MODE_PRIVATE)
//                .getInt("firebase_status", 0) == -1)
//        {
        startService(new Intent(getApplicationContext(), TokenRefreshService.class));
//        }


        if (hasPermissions(this, new String[] { Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION }))
        {
            ActivityCompat.startForegroundService(this, new Intent(this, LocationService.class));
        }

        onMessageReceived = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent)
            {
                if (intent != null && intent.hasExtra("remoteMessage"))
                {
                    onMessageReceived((RemoteMessage) intent.getParcelableExtra("remoteMessage"));
                }
            }

        };

    }

    private String getPath(String sUrl)
    {
        try
        {
            URL url = new URL(sUrl);
            return url.getHost() + url.getPath();
        }
        catch (MalformedURLException e)
        {
            return "";
        }
    }

    private boolean hasAction(RemoteMessage remoteMessage)
    {
        if (remoteMessage.getData().size() > 0 && remoteMessage.getData().containsKey("url"))
        {
            String currentUrl = getPath(xWalkMain.getUrl());
            String url = getPath(remoteMessage.getData().get("url"));

            if (currentUrl.equals(url))
                return false;
        }

        return true;
    }

    public void onMessageReceived(final RemoteMessage remoteMessage)
    {
        if (hasAction(remoteMessage))
        {
            RemoteMessage.Notification notification = remoteMessage.getNotification();
            if (notification != null && notification.getBody() != null)
            {
                Snackbar snackbar = Snackbar.make(xWalkMain,
                        (notification.getTitle() != null
                                ? notification.getTitle() + "\n" : "") + notification.getBody(),
                        5000);

                if (remoteMessage.getData().size() > 0 && remoteMessage.getData().containsKey("url"))
                {
                    snackbar.setAction("Открыть", new View.OnClickListener() {

                        @Override
                        public void onClick(View v)
                        {
                            loadPageBlank(remoteMessage.getData().get("url"),
                                    remoteMessage.getData().containsKey("title")
                                            ? remoteMessage.getData().get("title") : "");
                        }

                    });
                }
                snackbar.show();
            }
        }
    }

    public void enableMenu()
    {
        if (drawer.getDrawerLockMode(Gravity.START) == DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);

            final CoordinatorLayout content = (CoordinatorLayout) findViewById(R.id.content);

            toggle = new ActionBarDrawerToggle(
                    this, drawer, toolBar, 0, 0) {

                @Override
                public void onDrawerSlide(View view, float offset)
                {
                    content.setTranslationX(view.getWidth() * offset);
                }

            };
            drawer.addDrawerListener(toggle);
            toggle.syncState();

            toggle.setToolbarNavigationClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view)
                {

                    if (xWalkMain.getNavigationHistory().canGoBack())
                    {
                        xWalkMain.getNavigationHistory().navigate(XWalkNavigationHistory.Direction.BACKWARD, 1);
                    }
                }

            });

            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }
    }

    private void checkIntent()
    {
        if (getIntent().hasExtra("url"))
        {
            String url = getIntent().getStringExtra("url");
            loadPageStart(url, "");

            Uri uri = Uri.parse(url);
            xWalkMenu.loadUrl(uri.getScheme() + "://" + uri.getHost() + "/youdo/left.php");
        }
        else
        {
            String url = getString(R.string.app_url);
            loadPageStart(url + "/", "");
            xWalkMenu.loadUrl(url + "/left.php");
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        this.mActivityDelegate.onResume();

        if (xWalkMain != null)
        {
            xWalkMain.resumeTimers();
            xWalkMain.onShow();
        }

        if (xWalkMenu != null)
        {
            xWalkMenu.resumeTimers();
            xWalkMenu.onShow();
        }

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(onMessageReceived, new IntentFilter("onMessageReceived"));
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        if (xWalkMain != null)
        {
            xWalkMain.pauseTimers();
            xWalkMain.onHide();
        }

        if (xWalkMenu != null)
        {
            xWalkMenu.pauseTimers();
            xWalkMenu.onHide();
        }

        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(onMessageReceived);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if (xWalkMain != null)
        {
            xWalkMain.onDestroy();
        }

        if (xWalkMenu != null)
        {
            xWalkMenu.onDestroy();
        }

        if (timer != null)
        {
            timer.cancel();
        }
    }

    private void showProgress()
    {
        if (layer != null)
        {
            layer.setVisibility(View.VISIBLE);
        }

        timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run()
            {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run()
                    {
                        if (progressBar != null && layer != null && layer.getVisibility() == View.VISIBLE)
                        {
                            progressBar.setVisibility(View.VISIBLE);
                        }
                    }

                });
            }

        }, 2000);
    }

    public void loadPageStart(String url, String title)
    {
        if (title != null)
        {
            setTitle(title);
        }

        showProgress();

        xWalkMain.setTag("clear");
        xWalkMain.loadUrl(url);
    }

    public void loadPageBlank(String url, String title)
    {
        for (Integer i = xWalkMain.getNavigationHistory().getCurrentIndex(); i >= 0; i--)
        {
            if (xWalkMain.getNavigationHistory().getItemAt(i).getUrl().equals(url))
            {
                Integer step = xWalkMain.getNavigationHistory().getCurrentIndex() - i;
                xWalkMain.getNavigationHistory().navigate(XWalkNavigationHistory.Direction.BACKWARD, step);

                return;
            }
        }

        if (title != null)
        {
            setTitle(title);
        }

        showProgress();

        xWalkMain.loadUrl(url);
    }

    private void fadeOut(final View view)
    {
        Animation animation = new AlphaAnimation(1, 0);
        animation.setDuration(500);

        animation.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation)
            {
                view.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation)
            {
                view.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation)
            {

            }

        });

        view.startAnimation(animation);
    }

    public void setColors(String background, String titleText)
    {
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(background)));
        toggle.getDrawerArrowDrawable().setColor(Color.parseColor(titleText));
        this.titleText = titleText;
        setTitle(getTitle());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            getWindow().setNavigationBarColor(Color.parseColor(background));
            getWindow().setStatusBarColor(Color.parseColor(background));
        }
    }

    @Override
    public void setTitle(CharSequence title)
    {
        if (title != null && !title.equals(""))
        {
            super.setTitle(Html.fromHtml("<font color=\"" + titleText + "\">" + title + "</font>"));
        }
        else
        {
            super.setTitle("");
        }
    }

    @Override
    public void onBackPressed()
    {
        if (!closeMenu())
        {
            if (xWalkMain.getNavigationHistory().canGoBack())
            {
                super.onBackPressed();
            }
            else
            {
                new AlertDialog.Builder(this)
                        .setMessage("Выйти из приложения?")
                        .setPositiveButton("Да", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialogInterface, int i)
                            {
                                finish();
                            }

                        })
                        .setNegativeButton("Нет", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialogInterface, int i)
                            {

                            }

                        })
                        .setCancelable(false)
                        .create()
                        .show();
            }
        }
    }

    public boolean closeMenu()
    {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawerLayout);
        if (drawer.isDrawerOpen(GravityCompat.START))
        {
            drawer.closeDrawer(GravityCompat.START);
            return true;
        }

        return false;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            if (xWalkMain.getNavigationHistory().canGoBack())
            {
                xWalkMain.getNavigationHistory().navigate(XWalkNavigationHistory.Direction.BACKWARD, 1);
            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void setServer(String url)
    {
        loadPageStart(url + "/", "");
        xWalkMenu.loadUrl(url + "/left.php");
    }

    private void openFile()
    {
        Intent target = new Intent(Intent.ACTION_GET_CONTENT)
                .setType("*/*");

        Intent chooser = Intent.createChooser(target, "Выберите действие");

        if (hasPermissions(XWalkActivity.this, new String[] { Manifest.permission.CAMERA }))
        {
            List<Intent> extra = new ArrayList<Intent>();

            try
            {
                String prefix = new SimpleDateFormat("yyyyMMdd_HHmmss")
                        .format(new Date());

                mImageFile = File.createTempFile("IMG_" + prefix, ".jpg",
                        getExternalFilesDir(Environment.DIRECTORY_PICTURES));

                mVideoFile = File.createTempFile("VID_" + prefix, ".mp4",
                        getExternalFilesDir(Environment.DIRECTORY_MOVIES));
            }
            catch (IOException e)
            {

            }

            if (mImageFile != null)
            {
                extra.add(
                        new Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                                .putExtra(MediaStore.EXTRA_OUTPUT, getUriForFile(mImageFile))
                );
            }

            if (mVideoFile != null)
            {
                extra.add(
                        new Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                                .putExtra(MediaStore.EXTRA_OUTPUT, getUriForFile(mVideoFile))
                );
            }

            if (!extra.isEmpty())
            {
                chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, extra.toArray(new Parcelable[extra.size()]));
            }
        }

        startActivityForResult(chooser, FILE_REQUEST_CODE);
    }

    private Uri getUriForFile(File file)
    {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M)
        {
            return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        }
        else
        {
            return Uri.fromFile(file);
        }
    }

    @SuppressLint("MissingPermission")
    public void getCurrentPosition(final JsonObject params)
    {
        mCallback = params;

        String[] PERMISSIONS = { Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION };
        if (!hasPermissions(XWalkActivity.this, PERMISSIONS))
        {
            ActivityCompat.requestPermissions(XWalkActivity.this, PERMISSIONS, PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
        }
        else
        {
            if (params.has("success"))
            {
                LocationRequest request = LocationRequest.create()
                        .setNumUpdates(1);

                if (params.has("options"))
                {
                    JsonObject options = params.getAsJsonObject("options");

                    if (options.has("enableHighAccuracy") && options.get("enableHighAccuracy").getAsBoolean())
                    {
                        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    }

                    if (options.has("timeout"))
                    {
                        request.setMaxWaitTime(options.get("timeout").getAsLong());
                    }

                    if (options.has("maximumAge"))
                    {
                        request.setExpirationDuration(options.get("maximumAge").getAsLong());
                    }
                }

                LocationServices.getFusedLocationProviderClient(this)
                        .requestLocationUpdates(request, new LocationCallback() {

                            @Override
                            public void onLocationResult(LocationResult locationResult)
                            {
                                Location location = locationResult.getLastLocation();
                                if (location != null)
                                {
                                    callBackExecute(params.get("success").getAsInt(), "{ coords: { latitude: " + location.getLatitude() + ", longitude: " + location.getLongitude() + ", accuracy: " + location.getAccuracy() + " }, timestamp: " + location.getTime() + " }");
                                }
                            }

                        }, Looper.myLooper());
                }
            }


    }

    public void onCustomEvent(String eventName, JsonObject params)
    {
        String js = "javascript:BX.onCustomEvent('" + eventName + "', [JSON.parse('" + params + "')])";
        xWalkMain.loadUrl(js);
        xWalkMenu.loadUrl(js);
    }

    public void closeController()
    {
        if (xWalkMain.getNavigationHistory().canGoBack())
        {
            xWalkMain.getNavigationHistory().navigate(XWalkNavigationHistory.Direction.BACKWARD, 1);
        }
    }

    public void reload()
    {
        if (xWalkMain != null)
        {
            xWalkMain.reload(org.xwalk.core.XWalkView.RELOAD_NORMAL);
        }
    }

    public void getQRCode(int callback)
    {
        mCallback = new JsonObject();
        mCallback.addProperty("callback", callback);

        String[] PERMISSIONS = { Manifest.permission.CAMERA };
        if (!hasPermissions(XWalkActivity.this, PERMISSIONS))
        {
            ActivityCompat.requestPermissions(XWalkActivity.this, PERMISSIONS, PERMISSION_REQUEST_QR_CODE);
        }
        else
        {
            new IntentIntegrator(this)
                    .initiateScan();
        }
    }

    public void getContacts(int callback)
    {
        mCallback = new JsonObject();
        mCallback.addProperty("callback", callback);

        String[] PERMISSIONS = { Manifest.permission.READ_CONTACTS };
        if (!hasPermissions(XWalkActivity.this, PERMISSIONS))
        {
            ActivityCompat.requestPermissions(XWalkActivity.this, PERMISSIONS, PERMISSION_REQUEST_READ_CONTACTS);
        }
        else
        {
            StringBuilder result = new StringBuilder();
            result.append("{");

            ContentResolver content = getContentResolver();
            Cursor contacts = content.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
            if (contacts.getCount() > 0)
            {
                int i = 0;
                while (contacts.moveToNext())
                {
                    String _ID = contacts.getString(contacts.getColumnIndex(ContactsContract.Data._ID));
                    String DISPLAY_NAME = contacts.getString(contacts.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));

                    if (Integer.parseInt(contacts.getString(contacts.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0)
                    {
                        Cursor phones = getContentResolver().query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + _ID,
                                null,
                                null
                        );

                        int c = 0;
                        while (phones.moveToNext())
                        {
                            if (i > 0)
                            {
                                result.append(",");
                            }

                            String PHONE = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA));
                            result.append("\""+DISPLAY_NAME + (c > 0 ? " (" + (c + 1) + ")" : "") + "\":\"" + PHONE + "\"");

                            i++;
                            c++;
                        }
                        phones.close();
                    }
                }
            }

            contacts.close();

            result.append("}");

            callBackExecute(callback, "{CODE:'" + result.toString() + "'}");
        }
    }

    public void locationSettings(int callback)
    {
        mCallback = new JsonObject();
        mCallback.addProperty("callback", callback);

        startActivityForResult(
                new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                LOCATION_SETTINGS_REQUEST_CODE
        );
    }

    private void callBackExecute(int index, String result)
    {
        if (xWalkMain != null)
        {
            xWalkMain.loadUrl("javascript:app.callBackExecute(" + index + ", " + result + ")");
        }
    }

    public static boolean hasPermissions(Context context, String... permissions)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null)
        {
            for (String permission : permissions)
            {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED)
                {
                    return false;
                }
            }
        }

        return true;
    }

    @SuppressLint("NewApi")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        switch (requestCode)
        {
            case PERMISSION_REQUEST_CAMERA:
                openFile();
                break;

            case PERMISSION_REQUEST_ACCESS_FINE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    getCurrentPosition(mCallback);
                    ActivityCompat.startForegroundService(this, new Intent(XWalkActivity.this, LocationService.class));
                }
                else
                {
                    if (mCallback != null && mCallback.has("error"))
                    {
                        callBackExecute(mCallback.get("error").getAsInt(), "{code: 1, message: \"PERMISSION_DENIED\"}");
                    }
                }
                break;

            case PERMISSION_REQUEST_QR_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    getQRCode(mCallback.get("callback").getAsInt());
                }
                break;

            case PERMISSION_REQUEST_READ_CONTACTS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    getContacts(mCallback.get("callback").getAsInt());
                }
                break;
        }
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        if (intent.hasExtra("url"))
        {
            setIntent(intent);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            case FILE_REQUEST_CODE:

                if (resultCode == RESULT_CANCELED)
                {
                    mUploadFile.onReceiveValue(null);
                }
                else
                {
                    if (data != null && data.getData() != null)
                    {
                        mUploadFile.onReceiveValue(data.getData());
                    }
                    else if (mImageFile != null && mImageFile.length() > 0)
                    {
                        mVideoFile.delete();
                        mUploadFile.onReceiveValue(Uri.fromFile(mImageFile));
                    }
                    else if (mVideoFile != null && mVideoFile.length() > 0)
                    {
                        mImageFile.delete();
                        mUploadFile.onReceiveValue(Uri.fromFile(mVideoFile));
                    }
                    else
                    {
                        mImageFile.delete();
                        mVideoFile.delete();
                        mUploadFile.onReceiveValue(null);
                    }

                    mUploadFile = null;
                    mImageFile = null;
                    mVideoFile = null;
                }
                break;

            case IntentIntegrator.REQUEST_CODE:
                if (resultCode == RESULT_OK)
                {
                    callBackExecute(mCallback.get("callback").getAsInt(), "{CODE:'" + data.getStringExtra("SCAN_RESULT") + "'}");
                }
                break;

            case LOCATION_SETTINGS_REQUEST_CODE:
                callBackExecute(mCallback.get("callback").getAsInt(), "{}");
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }



}