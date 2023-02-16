/*
 * ====================================================================
 * Copyright (c) 2021-2023 AbandonedCart.  All rights reserved.
 *
 * See https://github.com/SamSprung/.github/blob/main/LICENSE#L5
 * ====================================================================
 *
 * The license and distribution terms for any publicly available version or
 * derivative of this code cannot be changed.  i.e. this code cannot simply be
 * copied and put under another distribution license
 * [including the GNU Public License.] Content not subject to these terms is
 * subject to to the terms and conditions of the Apache License, Version 2.0.
 */

package com.eightbit.samsprung

import android.content.Context
import android.provider.Settings
import java.io.File

class AccessibilityObserver {

    companion object {
        fun hasEnabledService(context: Context): Boolean {
            if (BuildConfig.GOOGLE_PLAY) return false
            val serviceString = Settings.Secure.getString(context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            return serviceString != null && serviceString.contains(context.packageName
                    + File.separator + AccessibilityObserver::class.java.name)
        }
        fun performBackAction() {}

    }
}
