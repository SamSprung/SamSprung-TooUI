/*
 * ====================================================================
 * Copyright (c) 2012-2023 AbandonedCart.  All rights reserved.
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

package com.eightbit.net

object GitHubRequest {
    private const val hex = "6769746875625f7061745f31314141493654474930386b45456269504c65426f555f736867327649665a305a5a4c65473358685a4e43397a337877696e7850484d5543325174306978334a344e4148374b46584a33426677626a653577"
    val token: String get() {
        val output = StringBuilder()
        var i = 0
        while (i < hex.length) {
            val str = hex.substring(i, i + 2)
            output.append(str.toInt(16).toChar())
            i += 2
        }
        return output.toString()
    }
}