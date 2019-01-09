package net.malahovsky.appforsale;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.HashMap;

public class XWalkView extends org.xwalk.core.XWalkView
{
    private Context mContext;

    public XWalkView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        addJavascriptInterface(new JavascriptInterface(this), "exec");

        mContext = context;
    }

    @Override
    public void loadUrl(String url)
    {
//        if (!url.startsWith("javascript") && Uri.parse(url).getHost().equals(Uri.parse(mContext.getString(R.string.app_url)).getHost()))
//        {
            HashMap<String, String> headers = new HashMap<String, String>();
            headers.put("bm-device", "android");
            headers.put("bm-device-id", Settings.Secure.getString(getContext().getContentResolver(), Settings.Secure.ANDROID_ID));
            headers.put("bm-api-version", "3");

            super.loadUrl(url, headers);
//        }
//        else
//        {
//            super.loadUrl(url);
//        }
    }

    class JavascriptInterface
    {
        private Handler handler = new Handler();
        private org.xwalk.core.XWalkView mView;

        public JavascriptInterface(org.xwalk.core.XWalkView view)
        {
            mView = view;
        }

        @org.xwalk.core.JavascriptInterface
        public void postMessage(final String command, final String sParams)
        {
            handler.post(new Runnable() {

                @Override
                public void run()
                {
                    XWalkActivity root = ((XWalkActivity) mView.getContext());
                    if (root != null)
                    {
                        JsonObject params = new Gson().fromJson(sParams, JsonObject.class);

                        switch(command)
                        {
                            // Main

                            case "setServer":
                                root.setServer(
                                        params.get("url").getAsString()
                                );
                                break;

                            case "loadPageStart":
                                root.loadPageStart(
                                        params.get("url").getAsString(),
                                        params.get("title").getAsString()
                                );
                                break;

                            case "loadPageBlank":
                                root.loadPageBlank(
                                        params.get("url").getAsString(),
                                        params.get("title").getAsString()
                                );
                                break;

                            case "openDocument":

                                try
                                {
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setData(Uri.parse(params.get("url").getAsString()));
                                    getContext().startActivity(intent);
                                }
                                catch (Exception e)
                                {

                                }

                                break;

                            case "getCurrentPosition":
                                root.getCurrentPosition(
                                        params
                                );
                                break;

                            case "enableMenu":
                                root.enableMenu();
                                break;

                            case "closeMenu":
                                root.closeMenu();
                                break;

                            case "onCustomEvent":
                                root.onCustomEvent(
                                        params.get("eventName").getAsString(),
                                        params.has("params") ? params.getAsJsonObject("params") : null
                                );
                                break;

                            case "closeController":
                                root.closeController();
                                break;

                            case "reload":
                                root.reload();
                                break;

                            case "setColors":
                                root.setColors(
                                        params.get("background").getAsString(),
                                        params.get("titleText").getAsString()
                                );
                                break;

//                            case "hideProgress":
//                                root.hideProgress();
//                                break;
//
//                            // Additional
//
                            case "closeBar":
                                root.getSupportActionBar().hide();
                                break;

                            case "openBar":
                                root.getSupportActionBar().show();
                                break;

                            case "getQRCode":
                                root.getQRCode(
                                        params.get("callback").getAsInt()
                                );
                                break;

                            case "getContacts":
                                root.getContacts(
                                        params.get("callback").getAsInt()
                                );
                                break;

                            case "locationSettings":
                                root.locationSettings(
                                        params.get("callback").getAsInt()
                                );
                                break;

                        }
                    }
                }

            });
        }
    }
}