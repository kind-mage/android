package net.malahovsky.appforsale.lib.cookie;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

public class CookieJarHandler implements CookieJar
{
    private Context mContext;

    public CookieJarHandler(Context context)
    {
        mContext = context;
    }

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies)
    {

    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url)
    {
        List<Cookie> result = new ArrayList<Cookie>();

        String cookieString = mContext.getSharedPreferences(mContext.getPackageName(), Context.MODE_PRIVATE).getString("cookies", null);
        if (cookieString != null && cookieString.length() > 0)
        {
            String[] cookies = cookieString.split("; ");
            for (int i = cookies.length; i > 0; i--)
            {
                result.add(Cookie.parse(url, cookies[i-1]));
            }
        }

        return result;
    }
}