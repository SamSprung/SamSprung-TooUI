package com.eightbit.samsprung

import android.content.IntentSender
import android.graphics.drawable.Drawable

class SamSprungNotice {

    private var text: String = ""
    private var intentSender: IntentSender? = null
    private var drawable: Drawable? = null

    fun setString(lines: String) {
        text += "\n" + lines
    }

    fun getString(): String {
        return text
    }

    fun setIntentSender(sender: IntentSender) {
        intentSender = sender
    }

    fun getIntentSender() : IntentSender? {
        return intentSender
    }

    fun setDrawable(icon: Drawable) {
        drawable = icon
    }

    fun getDrawable() : Drawable? {
        return drawable
    }
}