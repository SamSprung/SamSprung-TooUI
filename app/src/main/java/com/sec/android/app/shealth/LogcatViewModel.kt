package com.sec.android.app.shealth

import android.os.Build
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class LogcatViewModel : ViewModel() {

    suspend fun printLogcat(): String = withContext(Dispatchers.IO) {
        var mLogcatProc: Process
        var reader: BufferedReader
        val log = StringBuilder()
        val separator = System.getProperty("line.separator")
        log.append(Build.MANUFACTURER)
        log.append(" ")
        log.append(Build.MODEL)
        log.append(separator)
        log.append("Android SDK ")
        log.append(Build.VERSION.SDK_INT)
        log.append(" (")
        log.append(Build.VERSION.RELEASE)
        log.append(")")
        try {
            var line: String?
            mLogcatProc =
                Runtime.getRuntime().exec(arrayOf("logcat", "-ds", "AndroidRuntime:E"))
            reader = BufferedReader(
                InputStreamReader(
                    mLogcatProc.inputStream
                )
            )
            log.append(separator)
            log.append(separator)
            log.append("AndroidRuntime Logs")
            log.append(separator)
            log.append(separator)
            while (reader.readLine().also { line = it } != null) {
                log.append(line)
                log.append(separator)
            }
            reader.close()
            mLogcatProc =
                Runtime.getRuntime().exec(arrayOf("logcat", "-d", BuildConfig.APPLICATION_ID))
            reader = BufferedReader(
                InputStreamReader(
                    mLogcatProc.inputStream
                )
            )
            log.append(separator)
            log.append(separator)
            log.append("SamSprung Default Logs")
            log.append(separator)
            log.append(separator)
            while (reader.readLine().also { line = it } != null) {
                log.append(line)
                log.append(separator)
            }
            reader.close()
            mLogcatProc =
                Runtime.getRuntime().exec(
                    arrayOf(
                        "logcat", "-d",
                        "com.samsung.android.app.shealth.tracker.pedometer.service.coverwidget"
                    )
                )
            reader = BufferedReader(
                InputStreamReader(
                    mLogcatProc.inputStream
                )
            )
            log.append(separator)
            log.append(separator)
            log.append("SamSprung Widget Logs")
            log.append(separator)
            log.append(separator)
            while (reader.readLine().also { line = it } != null) {
                log.append(line)
                log.append(separator)
            }
            reader.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return@withContext log.toString()
    }
}