package com.eightbit.samsprung.launcher

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
 * 3. All advertising materials mentioning features or use of this
 *    software and redistributions of any form whatsoever
 *    must display the following acknowledgment:
 *    "This product includes software developed by AbandonedCart" unless
 *    otherwise displayed by tagged, public repository entries.
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

import android.annotation.SuppressLint
import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.BlendMode
import android.graphics.Color
import android.os.*
import android.provider.Settings
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.text.TextUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eightbit.app.CoverOptions
import com.eightbit.samsprung.NotificationReceiver
import com.eightbit.samsprung.R
import com.eightbit.samsprung.SamSprung
import com.eightbit.samsprung.SamSprungOverlay
import com.eightbit.view.OnSwipeTouchListener
import com.eightbit.widget.RecyclerViewTouch
import java.util.*

class NotificationFragment : Fragment(), NotificationAdapter.OnNoticeClickListener {

    private inline val @receiver:ColorInt Int.blended
        @ColorInt
        get() = ColorUtils.blendARGB(this,
            if ((requireActivity().resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES) Color.BLACK else Color.WHITE, 0.6f)

    private var launcherManager: LauncherManager? = null
    private var textSpeech: TextToSpeech? = null
    private var layoutManager: LinearLayoutManager? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_notices, container, false
        ) as ViewGroup
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        launcherManager = LauncherManager(requireActivity() as SamSprungOverlay)

        textSpeech = TextToSpeech(requireContext().applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result: Int? = textSpeech?.setLanguage(Locale.getDefault())
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    textSpeech = null
                }
            }
        }

        val noticesView = view.findViewById<RecyclerView>(R.id.notificationList)
        noticesView.setHasFixedSize(true)

        layoutManager = LinearLayoutManager(requireContext())
        noticesView.layoutManager = layoutManager
        noticesView.adapter = NotificationAdapter(requireActivity(), this)
        if (hasNotificationListener(requireContext())) {
            NotificationReceiver.getReceiver()?.setNotificationsListener(
                noticesView.adapter as NotificationAdapter
            )
        }

        RecyclerViewTouch(noticesView).setSwipeCallback(ItemTouchHelper.DOWN,
            object: RecyclerViewTouch.SwipeCallback {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
               if (direction == ItemTouchHelper.DOWN) {
                    onSwipeClosed(noticesView)
                }
            }
        })

        noticesView.setOnTouchListener(object : OnSwipeTouchListener(requireActivity()) {
            override fun onSwipeBottom() : Boolean {
                onSwipeClosed(noticesView)
                return false
            }
        })
    }

    private fun onSwipeClosed(recyclerView: RecyclerView) {
        val manager = recyclerView.layoutManager as LinearLayoutManager
        if (manager.itemCount == 0 || manager.findFirstCompletelyVisibleItemPosition() == 0) {
            (requireActivity() as SamSprungOverlay).closeMainDrawer()
        }
    }

    private fun promptNotificationReply(action: Notification.Action) {
        for (remoteInput in action.remoteInputs) {
            if (remoteInput.allowFreeFormInput) {
                var replyDialog: Dialog? = null
                val view: View = layoutInflater.inflate(R.layout.notification_reply, null)
                val dialog = AlertDialog.Builder(
                    ContextThemeWrapper(requireActivity(), R.style.Theme_SecondScreen_NoActionBar)
                )
                val actionEntries = view.findViewById<RelativeLayout>(R.id.entries)
                val reply = actionEntries.findViewById<EditText>(R.id.reply)
                actionEntries.findViewById<AppCompatImageView>(R.id.send).setOnClickListener {
                    val replyIntent = Intent()
                    val replyBundle = Bundle()
                    replyBundle.putCharSequence(remoteInput.resultKey, reply.text.toString())
                    RemoteInput.addResultsToIntent(action.remoteInputs, replyIntent, replyBundle)
                    requireActivity().startIntentSender(action.actionIntent.intentSender,
                        replyIntent, 0, 0, 0,
                        CoverOptions(null).getActivityOptions(1).toBundle())
                    replyDialog?.dismiss()
                }
                actionEntries.findViewById<AppCompatImageView>(R.id.cancel).setOnClickListener {
                    replyDialog?.dismiss()
                }
                replyDialog = dialog.setView(view).show()
                replyDialog.window?.setLayout(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                )
            }
        }
    }

    private fun setNotificationAction(
        position: Int, actionsPanel: LinearLayout, action: Notification.Action, color: Int) {
        val actionButtons = actionsPanel.findViewById<LinearLayout>(R.id.actions)
        val button = AppCompatButton(
            ContextThemeWrapper(requireActivity(), R.style.Theme_SecondScreen_NoActionBar)
        )
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
        button.setSingleLine()
        button.text = action.title
        button.backgroundTintBlendMode = BlendMode.MODULATE
        button.backgroundTintList = ColorStateList.valueOf(color)
        button.setOnClickListener {
            if (null != action.remoteInputs && action.remoteInputs.isNotEmpty()) {
                promptNotificationReply(action)
            } else {
                try {
                    var onFinished: PendingIntent.OnFinished? = null
                    if (action.actionIntent.creatorPackage.toString() == "com.android.systemui") {
                        onFinished = PendingIntent.OnFinished { pendingIntent, _, _, _, _ ->
                            val extras = Bundle()
                            extras.putString("launchPackage", pendingIntent.creatorPackage)
                            requireContext().startForegroundService(Intent(
                                requireContext().applicationContext, AppDisplayListener::class.java
                            ).putExtras(extras))
                        }
                    }
                    action.actionIntent.send(requireContext(), SamSprung.request_code,
                        Intent().setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_NO_ANIMATION or
                                Intent.FLAG_ACTIVITY_FORWARD_RESULT
                        ), onFinished, null, null,
                        CoverOptions(null).getActivityOptions(1).toBundle())
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
                actionsPanel.visibility = View.GONE
            }
            layoutManager?.scrollToPosition(position)
        }
        actionButtons.addView(button, LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT,
            when {
                action.title.length > 14 -> 1.0f
                action.title.length > 9 -> 0.8f
                action.title.length > 4 -> 0.6f
                else -> 0.5f
            }
        ))
    }

    fun onNoticeExpanded(itemView: View, position: Int, notice: StatusBarNotification) {
        val actionsPanel = itemView.findViewById<LinearLayout>(R.id.action_panel)
        val linesText = itemView.findViewById<TextView>(R.id.lines)
        if (actionsPanel.isVisible) {
            linesText.maxLines = 3
            linesText.ellipsize = TextUtils.TruncateAt.END
            actionsPanel.visibility = View.GONE
        } else {
            linesText.maxLines = Integer.MAX_VALUE
            linesText.ellipsize = null
            val actionButtons = actionsPanel.findViewById<LinearLayout>(R.id.actions)
            if (actionButtons.childCount > 0) {
                actionsPanel.visibility = View.VISIBLE
            } else {
                if (null != notice.notification.actions) {
                    actionsPanel.visibility = View.VISIBLE
                    val prefs = requireActivity().getSharedPreferences(
                        SamSprung.prefsValue, AppCompatActivity.MODE_PRIVATE)
                    for (action in notice.notification.actions) {
                        setNotificationAction(position, actionsPanel, action, prefs.getInt(
                            SamSprung.prefColors, Color.rgb(255, 255, 255)
                        ).blended)
                    }
                    layoutManager?.scrollToPositionWithOffset(position, -(actionButtons.height))
                }
            }
        }
    }

    override fun onNoticeClicked(itemView: View, position: Int, notice: StatusBarNotification) {
        (requireActivity() as SamSprungOverlay)
            .setKeyguardListener(object: SamSprungOverlay.KeyguardListener {
            override fun onKeyguardCheck(unlocked: Boolean) {
                if (!unlocked) return
                onNoticeExpanded(itemView, position, notice)
            }
        })
    }

    override fun onNoticeLongClicked(
        itemView: View, position: Int, notice: StatusBarNotification
    ) : Boolean {
        tactileFeedback()
        val content = StringBuilder(itemView.findViewById<TextView>(R.id.title).text)
            .append(System.getProperty("line.separator") ?: "\n")
            .append(itemView.findViewById<TextView>(R.id.lines).text)
        textSpeech?.speak(content, TextToSpeech.QUEUE_ADD, null, SamSprung.notification)
        return true
    }

    override fun onLaunchClicked(pendingIntent: PendingIntent) {
        launcherManager?.launchPendingIntent(pendingIntent)
    }

    private fun tactileFeedback() {
        val prefs = requireActivity().getSharedPreferences(
            SamSprung.prefsValue, AppCompatActivity.MODE_PRIVATE)
        if (!prefs.getBoolean(SamSprung.prefReacts, true)) return
        val vibe = VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (requireActivity().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                .defaultVibrator.vibrate(vibe)
        } else {
            @Suppress("DEPRECATION")
            (requireActivity().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(vibe)
        }
    }

    private fun hasNotificationListener(context: Context): Boolean {
        val myNotificationListenerComponentName = ComponentName(
            context.applicationContext, NotificationReceiver::class.java)
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver, "enabled_notification_listeners")
        if (enabledListeners.isEmpty()) return false
        return enabledListeners.split(":").map {
            ComponentName.unflattenFromString(it)
        }.any {componentName->
            myNotificationListenerComponentName == componentName
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (null != textSpeech) {
            textSpeech?.stop()
            textSpeech?.shutdown()
        }
    }
}