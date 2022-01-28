package com.samsung.android.app.shealth.tracker.pedometer.service.coverwidget;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sec.android.app.shealth.R;

import java.util.List;

public class StepRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private final Context context;
    private final List<ResolveInfo> packages;

    public StepRemoteViewsFactory(Context context) {
        this.context = context;

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        packages = context.getPackageManager().queryIntentActivities(mainIntent, 0);
    }

    @Override
    public void onCreate() {

    }

    @Override
    public void onDataSetChanged() {

    }

    @Override
    public void onDestroy() {
        packages.clear();
    }

    @Override
    public int getCount() {
        return packages.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        ResolveInfo application = packages.get(position);

        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.step_widget_item);
        rv.setTextViewText(R.id.widgetItemText, application.nonLocalizedLabel);
        rv.setImageViewResource(R.id.widgetItemImage, application.icon);

        Bundle extras = new Bundle();
        extras.putString("launchPackage", application.activityInfo.packageName);
        extras.putString("launchActivity", application.activityInfo.name);
        Intent fillInIntent = new Intent();
        fillInIntent.putExtras(extras);
        rv.setOnClickFillInIntent(R.id.widgetItemContainer, fillInIntent);

        return rv;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
