package net.malahovsky.appforsale;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.JsonObject;

import net.malahovsky.appforsale.lib.firebase.TokenRefreshService;
import net.malahovsky.appforsale.lib.location.LocationProvider;
import net.malahovsky.appforsale.utils.Keyboards;

import org.xwalk.core.XWalkUIClient;

import java.net.MalformedURLException;
import java.net.URL;

public class XWalkActivity extends AppCompatActivity
{
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private XWalkView xWalkView;

    private BroadcastReceiver onMessageReceived;

    public boolean orientation = false;

    private String titleText = "#ffffff";

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        if (!orientation)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
    }

    @SuppressLint("MissingPermission")
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

        setSupportActionBar((Toolbar) findViewById(R.id.toolBar));
        getSupportActionBar().setHomeButtonEnabled(true);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        final CoordinatorLayout content = (CoordinatorLayout) findViewById(R.id.content);

        toggle = new ActionBarDrawerToggle(this, drawerLayout, 0, 0) {

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset)
            {
                super.onDrawerSlide(drawerView, slideOffset);

                float slideX = drawerView.getWidth() * slideOffset;
                content.setTranslationX(slideX);
            }

        };

        toggle.setToolbarNavigationClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v)
            {
                onBackPressed();
            }

        });

        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
        xWalkView = (XWalkView) findViewById(R.id.xWalkView);
        xWalkView.setUIClient(new XWalkUIClient(xWalkView) {

            @Override
            public void onPageLoadStopped(org.xwalk.core.XWalkView view, String url, LoadStatus status)
            {
                if (status == LoadStatus.FINISHED)
                {
                    progressBar.setVisibility(View.GONE);
                    xWalkView.setVisibility(View.VISIBLE);
                }
            }

        });

        getFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {

            @Override
            public void onBackStackChanged()
            {
                Boolean back = getFragmentManager().getBackStackEntryCount() > 1;
                if (drawerLayout.getDrawerLockMode(Gravity.START) == DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(back);
                }
                else
                {
                    toggle.setDrawerIndicatorEnabled(!back);
                }
            }

        });

//        if (getSharedPreferences(getPackageName(), Context.MODE_PRIVATE)
//                .getInt("firebase_status", 0) == -1)
//        {
            startService(new Intent(getApplicationContext(), TokenRefreshService.class));
//        }

        LocationProvider.getInstance(this).requestLocationUpdates();

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

        checkIntent();
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        setIntent(intent);
        checkIntent();
    }

    private void checkIntent()
    {
        if (getIntent().hasExtra("url"))
        {
            String url = getIntent().getStringExtra("url");
            loadPageStart(url, "");

            Uri uri = Uri.parse(url);
            xWalkView.loadUrl(uri.getScheme() + "://" + uri.getHost() + "/youdo/left.php");
        }
        else
        {
            String url = getString(R.string.app_url);
            loadPageStart(url + "/", "");
            xWalkView.loadUrl(url + "/left.php");
        }
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
            if (getCurrentFragment() instanceof XWalkFragment)
            {
                XWalkFragment pageFragment = (XWalkFragment) getCurrentFragment();
                String currentUrl = getPath(pageFragment.getArguments().getString("url"));
                String url = getPath(remoteMessage.getData().get("url"));

                if (currentUrl.equals(url))
                    return false;
            }
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
                Snackbar snackbar = Snackbar.make(xWalkView,
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

    @Override
    protected void onResume()
    {
        super.onResume();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(onMessageReceived, new IntentFilter("onMessageReceived"));
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(onMessageReceived);
    }

//    private void loadDefaultServer()
//    {
//        xWalkView.loadUrl(getString(R.string.app_url) + "/left.php");
//        // loadPageStart(getString(R.string.app_url) + "/", "Главная");
//    }

    @Override
    public void onBackPressed()
    {
        if (drawerLayout.isDrawerOpen(GravityCompat.START))
        {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        else
        {
            if (getFragmentManager().getBackStackEntryCount() <= 1)
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
            else
            {
                super.onBackPressed();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if ((drawerLayout.getDrawerLockMode(Gravity.START) != DrawerLayout.LOCK_MODE_LOCKED_CLOSED
                && toggle.onOptionsItemSelected(item)))
        {
            return true;
        }

        switch (item.getItemId())
        {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void replaceFragment(final Fragment fragment, final boolean clearBackStack)
    {
        if (fragment == null)
        {
            return;
        }

        Fragment currentFragment = getCurrentFragment();
        FragmentManager manager = getFragmentManager();

        manager.popBackStack((clearBackStack ? null : fragment.getArguments().getString("url")), FragmentManager.POP_BACK_STACK_INCLUSIVE);

        @SuppressLint("CommitTransaction")
        FragmentTransaction transaction = manager.beginTransaction()
                .replace(R.id.container, fragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(fragment.getArguments().getString("url"));

        transaction.commit();

        hideKeyboard();
    }

    public void getContacts(int callback)
    {
        if (getCurrentFragment() instanceof XWalkFragment)
        {
            ((XWalkFragment) getCurrentFragment()).getContacts(callback);
        }
    }

    public Fragment getCurrentFragment()
    {
        return getFragmentManager().findFragmentById(R.id.container);
    }

    public void hideKeyboard()
    {
        Keyboards.hideKeyboard(this);
    }

    public void enableMenu()
    {
        if (drawerLayout.getDrawerLockMode(Gravity.START) == DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    public void closeMenu()
    {
        drawerLayout.closeDrawer(GravityCompat.START);
    }

    public void loadPageStart(String url, String title)
    {
        replaceFragment(XWalkFragment.newInstance(url, title), true);
    }

    public void loadPageBlank(String url, String title)
    {
        replaceFragment(XWalkFragment.newInstance(url, title), false);
    }

    public void onCustomEvent(String eventName, JsonObject params)
    {
        String js = "javascript:BX.onCustomEvent('" + eventName + "', [JSON.parse('" + params + "')])";
        xWalkView.loadUrl(js);
        LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(new Intent("onCustomEvent").putExtra("js", js));
    }

    public void getQRCode(int callback)
    {
        if (getCurrentFragment() instanceof XWalkFragment)
        {
            ((XWalkFragment) getCurrentFragment()).getQRCode(callback);
        }
    }

    public void reload()
    {
        if (getCurrentFragment() instanceof XWalkFragment)
        {
            ((XWalkFragment) getCurrentFragment()).reload();
        }
    }

    public void setColors(String background, String titleText)
    {
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(background)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            getWindow().setNavigationBarColor(Color.parseColor(background));
            getWindow().setStatusBarColor(Color.parseColor(background));
        }
        this.titleText = titleText;
        setTitle(getTitle());
    }

    public void setServer(String url)
    {
        loadPageStart(url + "/", "");
        xWalkView.loadUrl(url + "/left.php");
    }

    @Override
    public void setTitle(CharSequence title)
    {
        super.setTitle(Html.fromHtml("<font color=\"" + titleText + "\">" + title + "</font>"));
    }

    public void locationSettings(int callback)
    {
        if (getCurrentFragment() instanceof XWalkFragment)
        {
            ((XWalkFragment) getCurrentFragment()).locationSettings(callback);
        }
    }

    public void getCurrentPosition(JsonObject params)
    {
        Fragment fragment = getCurrentFragment();
        if (fragment instanceof XWalkFragment)
        {
            ((XWalkFragment) fragment).getCurrentPosition(params);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        if (getCurrentFragment() instanceof XWalkFragment)
        {
            ((XWalkFragment) getCurrentFragment()).onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        if (getCurrentFragment() instanceof XWalkFragment)
        {
            ((XWalkFragment) getCurrentFragment()).onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}