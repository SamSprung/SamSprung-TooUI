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

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

class RequestGitHubAPI(url: String) {
    private lateinit var listener: ResultListener

    init {
        Executors.newSingleThreadExecutor().execute {
            try {
                var conn = URL(url).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.useCaches = false
                conn.defaultUseCaches = false
                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM) {
                    conn.disconnect()
                    conn = URL(conn.getHeaderField("Location"))
                        .openConnection() as HttpURLConnection
                } else if (200 != responseCode) {
                    conn.disconnect()
                    return@execute
                }
                conn.inputStream.use { inStream ->
                    BufferedReader(
                        InputStreamReader(inStream, StandardCharsets.UTF_8)
                    ).use { streamReader ->
                        val responseStrBuilder = StringBuilder()
                        var inputStr: String?
                        while (null != streamReader.readLine()
                                .also { inputStr = it }
                        ) responseStrBuilder.append(inputStr)
                        listener.onResults(responseStrBuilder.toString())
                        conn.disconnect()
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    interface ResultListener {
        fun onResults(result: String)
    }

    fun setResultListener(listener: ResultListener) {
        this.listener = listener
    }
}
