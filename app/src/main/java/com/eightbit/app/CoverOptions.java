package com.eightbit.app;

import android.app.ActivityOptions;
import android.content.Intent;
import android.view.View;

public class CoverOptions {
    public static ActivityOptions getActivityOptions(int display) {
        return ActivityOptions.makeBasic().setLaunchDisplayId(display).setLaunchBounds(null);
    }

    public static ActivityOptions getAnimatedOptions(int display, View anchor, Intent intent) {
        if (null != intent.getSourceBounds()) {
            return ActivityOptions.makeScaleUpAnimation(anchor,
                    intent.getSourceBounds().left,
                    intent.getSourceBounds().top,
                    intent.getSourceBounds().width(),
                    intent.getSourceBounds().height()
            ).setLaunchDisplayId(display).setLaunchBounds(null);
        } else {
            return getActivityOptions(display);
        }
    }
}


