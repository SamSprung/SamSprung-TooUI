/* ====================================================================
 * Copyright (c) 2012-2022 AbandonedCart.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. All materials mentioning features or use of this software and
 *    redistributions of any form whatsoever must display the following
 *    acknowledgment unless made available by tagged, public "commits":
 *    "This product includes software developed by AbandonedCart"
 *
 * 4. The names "8-Bit Dream", "TwistedUmbrella" and "AbandonedCart"
 *    must not be used in any form to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact enderinexiledc@gmail.com
 *
 * 5. Products derived from this software may not be called "8-Bit Dream",
 *    "TwistedUmbrella" or "AbandonedCart" nor may these labels appear
 *    in their names without prior written permission of AbandonedCart.
 *
 * THIS SOFTWARE IS PROVIDED BY AbandonedCart ``AS IS'' AND ANY
 * EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE OpenSSL PROJECT OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 *
 * The license and distribution terms for any publicly available version or
 * derivative of this code cannot be changed.  i.e. this code cannot simply be
 * copied and put under another distribution license
 * [including the GNU Public License.] Content not subject to these terms is
 * subject to to the terms and conditions of the Apache License, Version 2.0.
 */

package com.eightbit.content;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

public class ScaledContext extends ContextWrapper {

    public ScaledContext(Context base) {
        super(base);
    }

    @SuppressWarnings("unused")
    public static ScaledContext wrap(Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();

        int orientation = resources.getConfiguration().orientation;

        metrics.density = 1.5f;
        metrics.densityDpi = 360;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            metrics.heightPixels = 2640;
            metrics.widthPixels = 1080;
        }
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            metrics.heightPixels = 1080;
            metrics.widthPixels = 2640;
        }
        metrics.scaledDensity = 1.5f;
        metrics.xdpi = 425.0f;
        metrics.ydpi = 425.0f;
        metrics.setTo(metrics);

        return new ScaledContext(context);
    }

    public static ScaledContext cover(Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();

        int orientation = resources.getConfiguration().orientation;

        metrics.density = 1f;
        metrics.densityDpi = 160; // 160
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            metrics.heightPixels = 512; // 512
            metrics.widthPixels = 260; // 260
        }
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            metrics.heightPixels = 260; // 260
            metrics.widthPixels = 512; // 512
        }
        metrics.scaledDensity = 1f;
        metrics.xdpi = 302.0f; // 302
        metrics.ydpi = 302.0f; // 302
        metrics.setTo(metrics);

        return new ScaledContext(context);
    }

    @SuppressWarnings("unused")
    public static Context restore(Context context) {
        context.getResources().getDisplayMetrics().setToDefaults();
        return context;
    }
}