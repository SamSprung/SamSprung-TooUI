package com.sec.android.app.shealth;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class StepWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new StepRemoteViewsFactory(getApplicationContext());
    }
}