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

import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES
import com.eightbit.samsprung.BuildConfig
import com.eightbit.samsprung.R
import com.heinrichreimersoftware.androidissuereporter.IssueReporterLauncher
import java.io.BufferedReader
import java.io.InputStreamReader

class Debug(private var context: Context) {

    var prefs: SharedPreferences? = null

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

    private val issueUrl = "https://github.com/SamSprung/SamSprung-TooUI/issues/" +
            "new?labels=logcat&template=bug_report.yml&title=[Bug]%3A+"

    private fun getDeviceProfile(isSecureDevice: Boolean): StringBuilder {
        val separator = System.getProperty("line.separator") ?: "\n"
        val log = StringBuilder(separator)
        log.append(context.getString(R.string.build_hash, BuildConfig.COMMIT))
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
        log.append(separator).append(context.getString(R.string.install_src, BuildConfig.FLAVOR))
        if (isSecureDevice) log.append(separator).append("Secure Lock Screen")
        return log
    }

    private fun submitLogcat(context: Context, logText: String) {
        val emailIntent = Intent(Intent.ACTION_SEND)
        emailIntent.type = "text/plain"
        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf("samsprungtoo@gmail.com"))
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "SamSprung-TooUI Logcat")
        emailIntent.putExtra(Intent.EXTRA_TEXT, logText)
        emailIntent.type = "message/rfc822"
        try {
            context.startActivity(Intent.createChooser(emailIntent, "Email logcat via..."))
        } catch (ex: ActivityNotFoundException) {
            val clipboard: ClipboardManager = context
                .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(
                        R.string.git_issue_title, BuildConfig.COMMIT
            ), logText))
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(issueUrl)))
        }
    }

    fun processException(isSecureDevice: Boolean, exception: String) {
        val separator = System.getProperty("line.separator") ?: "\n"
        val log = getDeviceProfile(isSecureDevice)
        log.append(separator).append(separator).append(exception)
        submitLogcat(context, log.toString())
    }

    fun captureLogcat(isSecureDevice: Boolean) : Boolean {
        val project = context.getString(R.string.samsprung)
        val repository = "SamSprung-TooUI"

        val separator = System.getProperty("line.separator") ?: "\n"
        val log = getDeviceProfile(isSecureDevice)
        try {
            var line: String?
            val mLogcatProc: Process = Runtime.getRuntime().exec(arrayOf(
                "logcat", "-d", "-t", "192", BuildConfig.APPLICATION_ID,
                "AndroidRuntime", "System.err",
                "AppIconSolution:S", "ViewRootImpl*:S",
                "IssueReporterActivity:S", "*:W"
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
        if (!logText.contains("AndroidRuntime")) {
            submitLogcat(context, logText)
            return false
        }
        return try {
            IssueReporterLauncher.forTarget(project, repository)
                .theme(R.style.Theme_SecondScreen_NoActionBar)
                .guestToken(getRepositoryToken())
                .guestEmailRequired(false)
                .publicIssueUrl(issueUrl)
                .titleTextDefault(context.getString(R.string.git_issue_title, BuildConfig.COMMIT))
                .minDescriptionLength(1)
                .putExtraInfo("logcat", logText)
                .homeAsUpEnabled(false).launch(context)
            true
        } catch (ignored: Exception) {
            submitLogcat(context, logText)
            true
        }
    }
}