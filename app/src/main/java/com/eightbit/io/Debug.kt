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
package com.eightbit.io

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Environment
import android.provider.MediaStore
import com.eightbit.samsprung.BuildConfig
import com.eightbit.samsprung.R
import com.heinrichreimersoftware.androidissuereporter.IssueReporterLauncher
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.io.StringWriter

class Debug(private var context: Context) {

    var prefs: SharedPreferences? = null

    private fun hasDebugging(): Boolean {
        return true
    }

    fun TAG(source: Class<*>): String {
        return source.simpleName
    }

    fun Log(source: Class<*>, params: String) {
        if (hasDebugging())
            android.util.Log.w(TAG(source), params)
    }

    fun Log(source: Class<*>, resource: Int) {
        Log(source, context.getString(resource))
    }

    fun Log(source: Class<*>, resource: Int, params: String) {
        Log(source, context.getString(resource, params))
    }

    fun Log(ex: Exception) {
        if (!hasDebugging()) return
        if (ex.stackTrace.isNotEmpty()) {
            val exception = StringWriter()
            ex.printStackTrace(PrintWriter(exception))
            Log(ex.javaClass, exception.toString())
        }
    }

    fun Log(resource: Int, ex: Exception) {
        if (hasDebugging())
            android.util.Log.w(TAG(ex.javaClass), context.getString(resource), ex)
    }

    private fun getRepositoryToken(): String {
        val hex = "6768705f64364552653338547a396764625874496b4748705a516544394473633679304e4c425036"
        val output = java.lang.StringBuilder()
        var i = 0
        while (i < hex.length) {
            val str = hex.substring(i, i + 2)
            output.append(str.toInt(16).toChar())
            i += 2
        }
        return output.toString()
    }

    fun captureLogcat(submitIssue: Boolean) : Boolean {
        val displayName = "samsprung_logcat"
        val project = context.getString(R.string.samsprung)
        val repository = "SamSprung-TooUI"

        val log = StringBuilder()
        val separator = System.getProperty("line.separator")
        log.append(context.getString(R.string.build_hash_full, BuildConfig.COMMIT))
        log.append(separator)
        log.append("Android ")
        val fields = VERSION_CODES::class.java.fields
        var codeName = "UNKNOWN"
        for (field in fields) {
            try {
                if (field.getInt(VERSION_CODES::class.java) == Build.VERSION.SDK_INT) {
                    codeName = field.name
                }
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }
        }
        log.append(codeName)
        log.append(" (")
        log.append(Build.VERSION.RELEASE)
        log.append(")")
        try {
            var line: String?
            val mLogcatProc: Process = Runtime.getRuntime().exec(arrayOf(
                "logcat", "-d", "-t", "256", BuildConfig.APPLICATION_ID,
                "AndroidRuntime", "System.err",
                "AppIconSolution:S", "ViewRootImpl:S", "IssueReporterActivity:S",
            ))
            val reader = BufferedReader(InputStreamReader(mLogcatProc.inputStream))
            log.append(separator).append(separator)
            while (reader.readLine().also { line = it } != null) {
                log.append(line)
                log.append(separator)
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        val logText = log.toString()
        if (submitIssue || logText.contains("AndroidRuntime", false)) {
            IssueReporterLauncher.forTarget(project, repository)
                .theme(R.style.Theme_SecondScreen_NoActionBar)
                .guestToken(getRepositoryToken())
                .guestEmailRequired(true)
                .guestAllowUsername(true)
                .titleTextDefault(context.getString(R.string.git_issue_title, BuildConfig.COMMIT))
                .minDescriptionLength(0)
                .putExtraInfo("logcat", logText)
                .homeAsUpEnabled(false).launch(context)
            return false
        } else {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)?.let {
                context.contentResolver.openOutputStream(it).use { fos ->
                    fos?.write(logText.toByteArray())
                }
            }
            return true
        }
    }
}