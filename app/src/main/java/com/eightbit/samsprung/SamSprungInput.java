package com.eightbit.samsprung;

import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputConnection;

import androidx.appcompat.view.ContextThemeWrapper;

import com.eightbit.content.ScaledContext;

import java.lang.ref.SoftReference;

@SuppressWarnings("deprecation")
public class SamSprungInput extends InputMethodService
        implements KeyboardView.OnKeyboardActionListener {

    private static InputMethodListener listener;
    private static SoftReference<ViewGroup> parent;

    private KeyboardView mKeyboardView;
    private SpeechRecognizer voice;
    private Keyboard mKeyPad;
    private Keyboard mKeyboard;
    private Keyboard mNumPad;

    private int kbIndex = 0;
    private boolean caps = false;

    public static void setInputListener(InputMethodListener inputListener, ViewGroup anchor) {
        listener = inputListener;
        parent = new SoftReference<>(anchor);
    }

    public static void setParent(ViewGroup anchor) {
        parent = new SoftReference<>(anchor);
    }

    private void animateKeyboardShow() {
        TranslateAnimation animate = new TranslateAnimation(
                0f, 0f, mKeyboardView.getHeight(), 0f
        );
        animate.setDuration(100);
        animate.setFillAfter(true);
        animate.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) { }

            @Override
            public void onAnimationEnd(Animation animation) {
                animation.setAnimationListener(null);
                swapKeyboardLayout(kbIndex);
            }

            @Override
            public void onAnimationRepeat(Animation animation) { }
        });
        parent.get().addView(mKeyboardView, 0);
        mKeyboardView.startAnimation(animate);
    }

    private void animateKeyboardHide() {
        TranslateAnimation animate = new TranslateAnimation(
                0f, 0f, 0f, mKeyboardView.getHeight()
        );
        animate.setDuration(100);
        animate.setFillAfter(false);
        animate.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) { }

            @Override
            public void onAnimationEnd(Animation animation) {
                animation.setAnimationListener(null);
                parent.get().removeView(mKeyboardView);
                if (null != listener)
                    listener.onKeyboardHidden(SamSprungInput.this);
            }

            @Override
            public void onAnimationRepeat(Animation animation) { }
        });
        mKeyboardView.startAnimation(animate);
    }

    private void swapKeyboardLayout(int newIndex) {
        if (null == mKeyboardView) return;
        switch (newIndex) {
            case 0:
                kbIndex = 0;
                mKeyboardView.setKeyboard(mKeyPad);
                break;
            case 1:
                kbIndex = 1;
                mKeyboardView.setKeyboard(mKeyboard);
                break;
            case 2:
                kbIndex = 2;
                mKeyboardView.setKeyboard(mNumPad);
                break;
            default:
                if (newIndex > 2) {
                    kbIndex = 0;
                    mKeyboardView.setKeyboard(mKeyPad);
                }
                if (newIndex < 0) {
                    kbIndex = 2;
                    mKeyboardView.setKeyboard(mNumPad);
                }
                break;
        }
    }

    private Intent getSpeechIntent(boolean partial) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, BuildConfig.APPLICATION_ID);
        if (partial) {
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        } else {
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        }
        intent.putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                Long.valueOf(1000)
        );
        return intent;
    }

    private Context getDisplayContext() {
        Context displayContext = createDisplayContext(((DisplayManager)
                getSystemService(Context.DISPLAY_SERVICE)).getDisplay(1));
        WindowManager wm = (WindowManager) displayContext.getSystemService(WINDOW_SERVICE);
        return new ContextThemeWrapper(displayContext, R.style.Theme_SecondScreen) {
            @Override
            public Object getSystemService(String name) {
                if (WINDOW_SERVICE.equals(name))
                    return wm;
                else
                    return super.getSystemService(name);
            }
        };
    }

    @Override
    public void onInitializeInterface() {
        mKeyPad = new Keyboard(ScaledContext.cover(this), R.xml.keyboard_predictive);
        mKeyboard = new Keyboard(ScaledContext.cover(this), R.xml.keyboard_qwerty);
        mNumPad = new Keyboard(ScaledContext.cover(this), R.xml.keyboard_numpad);
        super.onInitializeInterface();
    }

    @Override
    public boolean onShowInputRequested(int flags, boolean configChange) {
        if (null != listener) mKeyboardView = listener.onInputRequested(this);
        mKeyboardView.setPreviewEnabled(false);
        mKeyboardView.setOnKeyboardActionListener(this);
        if (null != mKeyboardView) {
            if (null != mKeyboardView.getParent())
                ((ViewGroup) mKeyboardView.getParent()).removeView(mKeyboardView);
            if (null != parent) {
                animateKeyboardShow();
            }
            voice = SpeechRecognizer.createSpeechRecognizer(this);
            voice.setRecognitionListener(new VoiceRecognizer(suggested ->
                    getCurrentInputConnection().commitText(suggested,1)));
        }
        return true;
    }

    @Override
    public View onCreateInputView() {
        return null;
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        InputConnection ic = getCurrentInputConnection();
        switch(primaryCode) {
            case 555:
                animateKeyboardHide();
                break;
            case -998:
                swapKeyboardLayout(kbIndex - 1);
                break;
            case -999:
                swapKeyboardLayout(kbIndex + 1);
                break;
            case -997:
                voice.startListening(getSpeechIntent(false));
                break;
            case Keyboard.KEYCODE_DELETE:
                ic.deleteSurroundingText(1, 0);
                break;
            case Keyboard.KEYCODE_SHIFT:
                caps = !caps;
                mKeyboard.setShifted(caps);
                mKeyboardView.invalidateAllKeys();
                break;
            case Keyboard.KEYCODE_DONE:
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                animateKeyboardHide();
                break;
            default:
                char code = (char)primaryCode;
                if(Character.isLetter(code) && caps){
                    code = Character.toUpperCase(code);
                }
                ic.commitText(String.valueOf(code),1);
        }
    }

    @Override
    public void onPress(int primaryCode) { }

    @Override
    public void onRelease(int primaryCode) { }

    @Override
    public void onText(CharSequence text) { }

    @Override
    public void swipeDown() { }

    @Override
    public void swipeLeft() { }

    @Override
    public void swipeRight() { }

    @Override
    public void swipeUp() { }

    interface InputMethodListener {
        KeyboardView onInputRequested(SamSprungInput instance);
        void onKeyboardHidden(SamSprungInput instance);
    }
}
