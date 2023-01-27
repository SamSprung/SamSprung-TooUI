/*
 * ====================================================================
 * Copyright (c) 2012-2022 AbandonedCart.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * For the purpose of this license, the phrase "SamSprung labels" shall
 * be used to refer to the labels "8-bit Dream", "TwistedUmbrella",
 * "SamSprung" and "AbandonedCart" and these labels should be considered
 * the equivalent of any usage of the aforementioned phrase.
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
 *    "This product includes software developed for SamSprung by AbandonedCart"
 *
 * 4. The SamSprung labels must not be used in any form to endorse or promote
 *    products derived from this software without prior written permission.
 *    For written permission, please contact enderinexiledc@gmail.com
 *
 * 5. Products derived from this software may not be called by the SamSprung
 *    labels nor may these labels appear in their names or product information
 *    without prior written permission of AbandonedCart.
 *
 * THIS SOFTWARE IS PROVIDED BY AbandonedCart AND SamSprung ``AS IS'' AND ANY
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

    private val hex = "6768705f64364552653338547a39676462587449" +
                      "6b4748705a516544394473633679304e4c425036"
    private val repositoryToken: String
        get() {
            val output = StringBuilder()
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
        log.append(context.getString(R.string.build_hash, BuildConfig.FLAVOR, BuildConfig.COMMIT))
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
        if (isSecureDevice) log.append(separator).append("Secure Lock Screen")
        return log
    }

    private fun setEmailParams(action: String, subject: String, text: String): Intent {
        return Intent(action).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("samsprungtoo@gmail.com"))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
    }

    private fun submitLogcat(context: Context, logText: String) {
        val subject = context.getString(R.string.git_issue_title, BuildConfig.COMMIT)
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).run {
            setPrimaryClip(ClipData.newPlainText(subject, logText))
        }

        try {
            val emailIntent: Intent = setEmailParams(Intent.ACTION_SENDTO, subject, logText)
            context.startActivity(Intent.createChooser(
                emailIntent, context.getString(R.string.logcat_crash)
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (anf: ActivityNotFoundException) {
            try {
                val emailIntent = setEmailParams(Intent.ACTION_SEND, subject, logText)
                context.startActivity(Intent.createChooser(
                    emailIntent, context.getString(R.string.logcat_crash)
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (ex: ActivityNotFoundException) {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(issueUrl)))
                } catch (ignored: Exception) { }
            }
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
                .guestToken(repositoryToken)
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
