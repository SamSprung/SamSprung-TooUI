package com.eightbit.samsprung

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

class RequestLatestCommit(url: String) {
    private lateinit var listener: ResultListener

    init {
        Executors.newSingleThreadExecutor().execute {
            try {
                var conn = URL(url).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.useCaches = false
                conn.defaultUseCaches = false
                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM) conn = URL(
                    conn.getHeaderField("Location")
                ).openConnection() as HttpURLConnection
                else if (200 != responseCode) return@execute
                val `in` = conn.inputStream
                val streamReader = BufferedReader(
                    InputStreamReader(`in`, StandardCharsets.UTF_8)
                )
                val responseStrBuilder = StringBuilder()
                var inputStr: String?
                while (streamReader.readLine().also { inputStr = it } != null
                ) responseStrBuilder.append(inputStr)
                listener.onResults(responseStrBuilder.toString())
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