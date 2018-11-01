package net.malahovsky.appforsale.lib.cookie;

import org.xwalk.core.XWalkCookieManager;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

public class CookieJarHandler implements CookieJar
{
    XWalkCookieManager cookieManager = new XWalkCookieManager();

    public CookieJarHandler()
    {
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptFileSchemeCookies(true);
    }

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies)
    {
        for (Cookie cookie : cookies)
        {
            cookieManager.setCookie(url.toString(), cookie.name() + "=" + cookie.value());
        }
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url)
    {
        List<Cookie> result = new ArrayList<Cookie>();

        String cookieString = cookieManager.getCookie(url.toString());
        if (cookieString != null && cookieString.length() > 0)
        {
            String[] cookies = cookieString.split(";");
            for (String cookie : cookies)
            {
                result.add(Cookie.parse(url, cookie.trim()));
            }
        }

        return result;
    }
}