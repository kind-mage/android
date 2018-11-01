package net.malahovsky.appforsale;

import org.xwalk.core.XWalkApplication;
import org.xwalk.core.XWalkPreferences;

public class XWalkAppication extends XWalkApplication
{
    @Override
    public void onCreate()
    {
        super.onCreate();

        XWalkPreferences.setValue(XWalkPreferences.SUPPORT_MULTIPLE_WINDOWS, true);
        XWalkPreferences.setValue(XWalkPreferences.JAVASCRIPT_CAN_OPEN_WINDOW, true);
    }
}
