package com.sec.android.app.shealth;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import androidx.annotation.NonNull;

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
        PackageManager pacMan = context.getPackageManager();

        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.step_widget_item);
        rv.setTextViewText(R.id.widgetItemText, application.loadLabel(pacMan).toString());
        try {
            Drawable drawable = pacMan.getApplicationIcon(
                    application.activityInfo.packageName);
            rv.setImageViewBitmap(R.id.widgetItemImage, getBitmapFromDrawable(drawable));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            rv.setImageViewBitmap(R.id.widgetItemImage, getBitmapFromDrawable(application.loadIcon(pacMan)));
        }

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

    @NonNull
    private Bitmap getBitmapFromDrawable(@NonNull Drawable drawable) {
        final Bitmap bmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bmp);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bmp;
    }
}
