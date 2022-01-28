package com.samsung.android.app.shealth.tracker.pedometer.service.coverwidget;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sec.android.app.shealth.R;

import java.util.List;

public class StepWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new StepRemoteViewsFactory(getApplicationContext());
    }
}