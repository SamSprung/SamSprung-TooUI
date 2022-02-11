package com.eightbit.samsprung;

import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputConnection;

import com.eightbit.content.ScaledContext;

import java.lang.ref.SoftReference;

@SuppressWarnings("deprecation")
public class SamSprungInput extends InputMethodService
        implements KeyboardView.OnKeyboardActionListener {

    private static InputMethodListener listener;
    private static SoftReference<ViewGroup> parent;

    private SoftReference<KeyboardView> mKeyboardView;
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
                0, 0f, mKeyboardView.get().getHeight() + 10, 0f
        );
        animate.setDuration(250);
        animate.setFillAfter(false);
        animate.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) { }

            @Override
            public void onAnimationEnd(Animation animation) {
                mKeyboardView.get().setAnimation(null);
                mKeyboardView.get().setOnKeyboardActionListener(SamSprungInput.this);
                swapKeyboardLayout(kbIndex);
            }

            @Override
            public void onAnimationRepeat(Animation animation) { }
        });
        parent.get().addView(mKeyboardView.get(), 0);
        mKeyboardView.get().startAnimation(animate);
    }

    private void animateKeyboardHide() {
        TranslateAnimation animate = new TranslateAnimation(
                0, 0f, 0, mKeyboardView.get().getHeight()
        );
        animate.setDuration(250);
        animate.setFillAfter(false);
        animate.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) { }

            @Override
            public void onAnimationEnd(Animation animation) {
                mKeyboardView.get().setAnimation(null);
                parent.get().removeView(mKeyboardView.get());
                if (null != listener)
                    listener.onKeyboardHidden();
            }

            @Override
            public void onAnimationRepeat(Animation animation) { }
        });
        mKeyboardView.get().startAnimation(animate);
    }

    private void swapKeyboardLayout(int newIndex) {
        if (null == mKeyboardView) return;
        switch (newIndex) {
            case 0:
                kbIndex = 0;
                mKeyboardView.get().setKeyboard(mKeyPad);
                break;
            case 1:
                kbIndex = 1;
                mKeyboardView.get().setKeyboard(mKeyboard);
                break;
            case 2:
                kbIndex = 2;
                mKeyboardView.get().setKeyboard(mNumPad);
                break;
            default:
                if (newIndex > 2) {
                    kbIndex = 0;
                    mKeyboardView.get().setKeyboard(mKeyPad);
                }
                if (newIndex < 0) {
                    kbIndex = 2;
                    mKeyboardView.get().setKeyboard(mNumPad);
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

    @Override
    public void onInitializeInterface() {
        mKeyPad = new Keyboard(ScaledContext.cover(this), R.xml.keyboard_predictive);
        mKeyboard = new Keyboard(ScaledContext.cover(this), R.xml.keyboard_qwerty);
        mNumPad = new Keyboard(ScaledContext.cover(this), R.xml.keyboard_numpad);
        super.onInitializeInterface();
    }

    @Override
    public boolean onShowInputRequested(int flags, boolean configChange) {
        if (null != listener) mKeyboardView =
                new SoftReference<>(listener.onInputRequested(this));
        if (null != mKeyboardView) {
            if (null != mKeyboardView.get().getParent())
                ((ViewGroup) mKeyboardView.get().getParent()).removeView(mKeyboardView.get());
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
                mKeyboardView.get().invalidateAllKeys();
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
        void onKeyboardHidden();
    }
}
