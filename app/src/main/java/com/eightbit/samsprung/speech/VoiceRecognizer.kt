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

package com.eightbit.samsprung.speech

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.eightbit.samsprung.BuildConfig

@SuppressLint("SetJavaScriptEnabled")
class VoiceRecognizer(private val listener: SpeechResultsListener?) : RecognitionListener {
    override fun onReadyForSpeech(params: Bundle) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray) {}
    override fun onEndOfSpeech() {}
    override fun onError(error: Int) {}
    override fun onResults(results: Bundle) {
        val data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val grammar = StringBuilder()
        if (data != null) grammar.append(data[0])
        grammar.setCharAt(0, Character.toUpperCase(grammar[0]))
        val suggested = grammar.toString()
        listener?.onSpeechResults(suggested)
    }

    override fun onPartialResults(partialResults: Bundle) {}
    override fun onEvent(eventType: Int, params: Bundle) {}
    interface SpeechResultsListener {
        fun onSpeechResults(suggested: String)
    }

    fun getSpeechIntent(partial: Boolean): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, BuildConfig.APPLICATION_ID)
            if (partial) {
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            } else {
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                java.lang.Long.valueOf(1000)
            )
        }
    }
}
