package so.cym.crashhandlerdemo;


import android.app.Application;
import android.os.Handler;
import android.util.Log;

public class CrashApplication extends Application{
    /** TAG */
    public static final String TAG = "CrashApplication";
    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler.getInstance().init(this);
        Log.v(TAG, "application created");
    }
}
