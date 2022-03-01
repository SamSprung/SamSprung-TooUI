/* ====================================================================
 * Copyright (c) 2012-2018 Abandoned Cart.  All rights reserved.
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
 *    distribution or a referenced public source respository.
 *
 * 3. All advertising materials mentioning features or use of this
 *    software and redistributions of any form whatsoever
 *    must display the following acknowledgment:
 *    "This product includes software developed by Abandoned Cart" unless
 *    otherwise displayed by tagged, public repository entries.
 *
 * 4. The names "Ender's Games", "TwistedUmbrella", and "Abandoned Cart"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written permission,
 *    please contact enderinexiledc@gmail.com.
 *
 * THIS SOFTWARE IS PROVIDED BY ABANDONED CART ``AS IS'' AND ANY
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
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, BuildConfig.APPLICATION_ID)
        if (partial) {
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        } else {
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        intent.putExtra(
            RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
            java.lang.Long.valueOf(1000)
        )
        return intent
    }
}