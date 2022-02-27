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
import android.content.Context
import android.content.Intent
import android.os.*
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eightbit.samsprung.*
import com.eightbit.view.OnSwipeTouchListener
import com.eightbit.widget.RecyclerViewTouch
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.util.*

class NotificationFragment : Fragment(), NotificationAdapter.OnNoticeClickListener {

    private var launchManager: LaunchManager? = null
    private lateinit var noticesView: RecyclerView
    private var textSpeech: TextToSpeech? = null

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

        val prefs = requireActivity().getSharedPreferences(
            SamSprung.prefsValue, AppCompatActivity.MODE_PRIVATE)

        launchManager = LaunchManager(requireActivity() as SamSprungOverlay)

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

        noticesView = view.findViewById(R.id.notificationList)
        noticesView.layoutManager = LinearLayoutManager(requireContext())
        noticesView.adapter = NotificationAdapter(requireActivity(), this)
        if ((requireActivity() as SamSprungOverlay).hasNotificationListener()) {
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
        val bottomSheet = (requireActivity() as SamSprungOverlay).getBottomSheetMain()
        val manager = recyclerView.layoutManager as LinearLayoutManager
        if (manager.itemCount == 0 || manager.findFirstCompletelyVisibleItemPosition() == 0) {
            bottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
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
                        ActivityOptions.makeBasic().setLaunchDisplayId(1).toBundle())
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
        position: Int, actionsPanel: LinearLayout, action: Notification.Action) {
        val actionButtons = actionsPanel.findViewById<LinearLayout>(R.id.actions)
        val button = AppCompatButton(
            ContextThemeWrapper(requireActivity(),
                R.style.Theme_SecondScreen_NoActionBar
            )
        )
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
        button.setSingleLine()
        button.text = action.title
        button.setOnClickListener {
            if (null != action.remoteInputs && action.remoteInputs.isNotEmpty()) {
                promptNotificationReply(action)
            } else {
                try {
                    action.actionIntent.send()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
                actionsPanel.visibility = View.GONE
            }
            (noticesView.layoutManager as LinearLayoutManager).scrollToPosition(position)
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

    override fun onNoticeClicked(itemView: View, position: Int, notice: StatusBarNotification) {
        val actionsPanel = itemView.findViewById<LinearLayout>(R.id.action_panel)
        if (actionsPanel.isVisible) {
            actionsPanel.visibility = View.GONE
        } else {
            val actionButtons = actionsPanel.findViewById<LinearLayout>(R.id.actions)
            if (actionButtons.childCount > 0) {
                actionsPanel.visibility = View.VISIBLE
            } else {
                if (null != notice.notification.actions) {
                    actionsPanel.visibility = View.VISIBLE
                    for (action in notice.notification.actions) {
                        setNotificationAction(position, actionsPanel, action)
                    }
                    (noticesView.layoutManager as LinearLayoutManager)
                        .scrollToPositionWithOffset(position,
                            -(actionButtons.height))
                }
            }
        }
    }

    override fun onNoticeLongClicked(
        itemView: View, position: Int, notice: StatusBarNotification
    ) : Boolean {
        tactileFeedback()
        textSpeech?.speak(itemView.findViewById<TextView>(R.id.lines).text,
            TextToSpeech.QUEUE_ADD, null, SamSprung.notification)
        return true
    }

    override fun onLaunchClicked(pendingIntent: PendingIntent) {
        launchManager?.launchPendingActivity(pendingIntent)
    }

    private fun tactileFeedback() {
        val vibe = VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (requireActivity().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                .defaultVibrator.vibrate(vibe)
        } else {
            @Suppress("DEPRECATION")
            (requireActivity().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(vibe)
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