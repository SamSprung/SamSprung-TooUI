package com.samsung.android.app.shealth.tracker.pedometer.service.coverwidget;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class StepBroadcastReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            SharedPreferences sharedPref = context.getSharedPreferences(
                    "com.zflip.launcher.PREFS", Context.MODE_PRIVATE);
            String launchPackage = sharedPref.getString("launchPackage", null);
//            String launchActivity = sharedPref.getString("launchActivity", null);
            if (launchPackage != null) {
                ActivityManager activityManager = (ActivityManager)
                        context.getSystemService(Context.ACTIVITY_SERVICE);
                activityManager.killBackgroundProcesses(launchPackage);
            }
        }
    }

}