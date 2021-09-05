package com.sec.android.app.shealth

import android.content.Intent
import android.widget.RemoteViewsService

class StepWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return StepRemoteViewsFactory(applicationContext)
    }
}