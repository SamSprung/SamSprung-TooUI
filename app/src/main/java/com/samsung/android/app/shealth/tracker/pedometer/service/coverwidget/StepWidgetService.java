package com.samsung.android.app.shealth.tracker.pedometer.service.coverwidget;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class StepWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new StepRemoteViewsFactory(getApplicationContext());
    }
}