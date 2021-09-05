package com.sec.android.app.shealth;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;

import com.samsung.android.app.shealth.tracker.pedometer.service.coverwidget.StepCoverAppWidget;

public class StepBroadcastReceiver extends BroadcastReceiver {

    private DisplayManager.DisplayListener mDisplayListener;

    @SuppressWarnings("unused")
    public StepBroadcastReceiver() { }

    public StepBroadcastReceiver(DisplayManager.DisplayListener mDisplayListener) {
        this.mDisplayListener = mDisplayListener;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            DisplayManager manager = (DisplayManager)
                    context.getSystemService(Context.DISPLAY_SERVICE);
            manager.unregisterDisplayListener(mDisplayListener);
            SharedPreferences sharedPref = context.getSharedPreferences(
                    "com.zflip.launcher.PREFS", Context.MODE_PRIVATE);
            String launchPackage = sharedPref.getString("launchPackage", null);
            String launchActivity = sharedPref.getString("launchActivity", null);
//            if (launchPackage != null) {
//                ActivityManager activityManager = (ActivityManager)
//                        context.getSystemService(Context.ACTIVITY_SERVICE);
//                activityManager.killBackgroundProcesses(launchPackage);
//            }
            Intent appIntent = new Intent(Intent.ACTION_MAIN);
            appIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            appIntent.setComponent(new ComponentName(launchPackage, launchActivity));
            ActivityOptions options = ActivityOptions.makeBasic().setLaunchDisplayId(0);
            appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(appIntent, options.toBundle());
        }
    }

}