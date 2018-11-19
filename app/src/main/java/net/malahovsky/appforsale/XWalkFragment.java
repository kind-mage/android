package net.malahovsky.appforsale;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.widget.ProgressBar;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.gson.JsonObject;
import com.google.zxing.integration.android.IntentIntegrator;

import net.malahovsky.appforsale.lib.location.LocationProvider;

import org.xwalk.core.XWalkJavascriptResult;
import org.xwalk.core.XWalkResourceClient;
import org.xwalk.core.XWalkUIClient;
import org.xwalk.core.XWalkView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class XWalkFragment extends Fragment
{
    private ProgressBar progressBar;
    private XWalkView xWalkView;

    private static final int PERMISSION_REQUEST_CAMERA = 1001;
    private static final int PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 1002;
    private static final int PERMISSION_REQUEST_QR_CODE = 1003;
    private static final int PERMISSION_REQUEST_READ_CONTACTS = 1004;

    private static final int FILE_REQUEST_CODE = 1001;
    private static final int LOCATION_SETTINGS_REQUEST_CODE = 1002;

    private ValueCallback<Uri> mUploadFile;
    private File mImageFile;
    private File mVideoFile;

    private BroadcastReceiver mMessageReceiver;

    private JsonObject mCallback;

    private View page;

    private Boolean bFinished = false;

    private Timer timer;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mMessageReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent)
            {
                xWalkView.loadUrl(intent.getStringExtra("js"));
            }

        };
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("onCustomEvent"));
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
    }

    public static XWalkFragment newInstance(String url, String title)
    {
        Bundle args = new Bundle();
        args.putString("url", url);
        args.putString("title", title);

        XWalkFragment fragment = new XWalkFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        getActivity().setTitle(getArguments().getString("title", ""));

        if (page == null)
        {
            page = inflater.inflate(R.layout.content_xwalk, container, false);

            progressBar = (ProgressBar) page.findViewById(R.id.progressBar);

            timer = new Timer();
            timer.schedule(new TimerTask() {

                @Override
                public void run()
                {
                    if (getActivity() != null)
                    {
                        getActivity().runOnUiThread(new Runnable() {

                            @Override
                            public void run()
                            {
                                progressBar.setVisibility(View.VISIBLE);
                            }

                        });
                    }
                }

            }, 2000);

            xWalkView = (XWalkView) page.findViewById(R.id.xWalkView);

            xWalkView.setResourceClient(new XWalkResourceClient(xWalkView) {

                @Override
                public void onReceivedSslError(XWalkView view, final ValueCallback<Boolean> callback, SslError error)
                {
                    new AlertDialog.Builder(getActivity())
                            .setIcon(getActivity().getApplicationInfo().icon)
                            .setTitle(getActivity().getApplicationInfo().labelRes)
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
                public boolean shouldOverrideUrlLoading(XWalkView view, String url)
                {
                    Uri uri = Uri.parse(url);

                    if (bFinished && (uri.getScheme().equals("http") || uri.getScheme().equals("https")))
                    {
                        if (getActivity() != null)
                        {
                            ((XWalkActivity) getActivity()).loadPageBlank(uri.toString(), "");
                        }

                        return true;
                    }
                    else
                    {
                        return super.shouldOverrideUrlLoading(view, url);
                    }
                }

                @Override
                public void onProgressChanged(XWalkView view, int progressInPercent)
                {
                    progressBar.setProgress(progressInPercent);
                }

            });

            xWalkView.setUIClient(new XWalkUIClient(xWalkView) {

                @Override
                public boolean onCreateWindowRequested(final XWalkView view, InitiateBy initiator, ValueCallback<XWalkView> callback)
                {
                    View _page = LayoutInflater.from(getActivity()).inflate(R.layout.content_xwalk, null);

                    final ProgressBar _progressBar = _page.findViewById(R.id.progressBar);
                    final XWalkView _xWalkView = _page.findViewById(R.id.xWalkView);

                    final Timer _timer = new Timer();
                    _timer.schedule(new TimerTask() {

                        @Override
                        public void run()
                        {
                            if (getActivity() != null)
                            {
                                getActivity().runOnUiThread(new Runnable() {

                                    @Override
                                    public void run()
                                    {
                                        _progressBar.setVisibility(View.VISIBLE);
                                    }

                                });
                            }
                        }

                    }, 2000);

                    final Dialog dialog = new Dialog(getActivity());
                        dialog.setContentView(_page);
                        dialog.getWindow().setDimAmount(0);
                        dialog.show();

                    _xWalkView.setUIClient(new XWalkUIClient(_xWalkView) {

                        @Override
                        public void onPageLoadStopped(XWalkView view, String url, LoadStatus status)
                        {
                            if (status == LoadStatus.FINISHED)
                            {
                                _timer.cancel();

                                _progressBar.setVisibility(View.INVISIBLE);
                                _xWalkView.setVisibility(View.VISIBLE);
                            }
                        }

                        @Override
                        public void onJavascriptCloseWindow(XWalkView view)
                        {
                            _timer.cancel();
                            dialog.dismiss();
                        }

                    });

                    callback.onReceiveValue(_xWalkView);

                    return true;
                }

                @Override
                public boolean onJsAlert(XWalkView view, String url, String message, final XWalkJavascriptResult result)
                {
                    new AlertDialog.Builder(getActivity())
                            .setIcon(getActivity().getApplicationInfo().icon)
                            .setTitle(getActivity().getApplicationInfo().labelRes)
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
                public void openFileChooser(XWalkView view, ValueCallback<Uri> uploadFile, String acceptType, String capture)
                {
                    mUploadFile = uploadFile;

                    String[] PERMISSIONS = { Manifest.permission.CAMERA };
                    if (!hasPermissions(getActivity(), PERMISSIONS))
                    {
                        ActivityCompat.requestPermissions(getActivity(), PERMISSIONS, PERMISSION_REQUEST_CAMERA);
                    }
                    else
                    {
                        openFile();
                    }
                }

                @Override
                public void onPageLoadStopped(XWalkView view, String url, LoadStatus status)
                {
                    if (url != null && !url.equals(""))
                    {
                        switch (status)
                        {
                            case FINISHED:

                                if (getActivity() != null)
                                {
                                    if ((getActivity().getTitle() == null || getActivity().getTitle().equals(""))
                                            && view.getTitle() != null && !view.getTitle().equals(""))
                                    {
                                        getActivity().setTitle(view.getTitle());
                                    }
                                }

                                timer.cancel();

                                progressBar.setVisibility(View.GONE);
                                xWalkView.setVisibility(View.VISIBLE);

                                break;

                            case FAILED:

                                new AlertDialog.Builder(getActivity())
                                        .setIcon(getActivity().getApplicationInfo().icon)
                                        .setTitle(getActivity().getApplicationInfo().labelRes)
                                        .setMessage("Вы не подключены к Интернету")
                                        .setPositiveButton("Повторить", new DialogInterface.OnClickListener() {

                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i)
                                            {
                                                xWalkView.reload(XWalkView.RELOAD_NORMAL);
                                            }

                                        })
                                        .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i)
                                            {
                                                getActivity().onBackPressed();
                                            }

                                        })
                                        .setCancelable(false)
                                        .create()
                                        .show();

                                break;
                        }

                        bFinished = true;
                    }
                }

            });

            xWalkView.loadUrl(getArguments().getString("url"));

        }

        return page;
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        timer.cancel();
    }

    private void openFile()
    {
        Intent target = new Intent(Intent.ACTION_GET_CONTENT)
                .setType("*/*");

        Intent chooser = Intent.createChooser(target, "Выберите действие");

        if (hasPermissions(getActivity(), new String[] { Manifest.permission.CAMERA }))
        {
            List<Intent> extra = new ArrayList<Intent>();

            try
            {
                String prefix = new SimpleDateFormat("yyyyMMdd_HHmmss")
                        .format(new Date());

                mImageFile = File.createTempFile("IMG_" + prefix, ".jpg",
                        getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES));

                mVideoFile = File.createTempFile("VID_" + prefix, ".mp4",
                        getActivity().getExternalFilesDir(Environment.DIRECTORY_MOVIES));
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
            return FileProvider.getUriForFile(getActivity(), getActivity().getPackageName() + ".fileprovider", file);
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

        String[] PERMISSIONS = { Manifest.permission.ACCESS_FINE_LOCATION };
        if (!hasPermissions(getActivity(), PERMISSIONS))
        {
            ActivityCompat.requestPermissions(getActivity(), PERMISSIONS, PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
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

                LocationServices.getFusedLocationProviderClient(getActivity())
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

    public void getQRCode(int callback)
    {
        mCallback = new JsonObject();
        mCallback.addProperty("callback", callback);

        String[] PERMISSIONS = { Manifest.permission.CAMERA };
        if (!hasPermissions(getActivity(), PERMISSIONS))
        {
            ActivityCompat.requestPermissions(getActivity(), PERMISSIONS, PERMISSION_REQUEST_QR_CODE);
        }
        else
        {
            new IntentIntegrator(getActivity())
                    .initiateScan();
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

    public void getContacts(Integer callback)
    {
        mCallback = new JsonObject();
        mCallback.addProperty("callback", callback);

        String[] PERMISSIONS = { Manifest.permission.READ_CONTACTS };
        if (!hasPermissions(getActivity(), PERMISSIONS))
        {
            ActivityCompat.requestPermissions(getActivity(), PERMISSIONS, PERMISSION_REQUEST_READ_CONTACTS);
        }
        else
        {
            StringBuilder result = new StringBuilder();
            result.append("{");

            ContentResolver content = getActivity().getContentResolver();
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
                        Cursor phones = getActivity().getContentResolver().query(
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

    private static boolean hasPermissions(Context context, String... permissions)
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
                    LocationProvider.getInstance(getActivity()).requestLocationUpdates();
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
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
            switch (requestCode)
            {
                case FILE_REQUEST_CODE:

                    if (resultCode == getActivity().RESULT_CANCELED)
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
                    if (resultCode == getActivity().RESULT_OK)
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

    private void callBackExecute(int index, String result)
    {
        xWalkView.loadUrl("javascript:app.callBackExecute(" + index + ", " + result + ")");
    }

    public void reload()
    {
        xWalkView.reload(XWalkView.RELOAD_NORMAL);
    }
}