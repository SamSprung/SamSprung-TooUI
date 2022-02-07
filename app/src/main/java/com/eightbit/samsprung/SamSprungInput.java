package com.eightbit.samsprung;

import android.annotation.SuppressLint;
import android.content.Context;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputConnection;

import com.eightbit.content.ScaledContext;

import java.lang.ref.SoftReference;

@SuppressWarnings("deprecation")
public class SamSprungInput extends InputMethodService
        implements KeyboardView.OnKeyboardActionListener {

    private static InputMethodListener listener;

    private SoftReference<KeyboardView> mKeyboardView;
    private Keyboard mKeyboard;
    private Keyboard mNumPad;
    private static SoftReference<ViewGroup> parent;
    private static boolean isNumPad = false;

    private boolean caps = false;

    @Override
    public void onInitializeInterface() {
        mKeyboard = new Keyboard(ScaledContext.cover(this), R.xml.keyboard_qwerty);
        mNumPad = new Keyboard(ScaledContext.cover(this), R.xml.keyboard_numpad);
        super.onInitializeInterface();
    }

    @Override
    public boolean onShowInputRequested(int flags, boolean configChange) {
        if (null != listener) mKeyboardView =
                new SoftReference<>(listener.onInputRequested(this));
        if (null != mKeyboardView) {
            mKeyboardView.get().setKeyboard(mKeyboard);
            mKeyboardView.get().setOnKeyboardActionListener(this);
            if (null != mKeyboardView.get().getParent())
                ((ViewGroup) mKeyboardView.get().getParent()).removeView(mKeyboardView.get());
            if (null != parent) {
                parent.get().addView(mKeyboardView.get(), 0);
            }
        }
        return true;
    }

    public static void setInputListener(InputMethodListener inputListener, ViewGroup anchor) {
        listener = inputListener;
        parent = new SoftReference<>(anchor);
        isNumPad = false;
    }

    @Override
    public View onCreateInputView() {
        return null;
    }

    private void swapKeyboardLayout() {
        if (null == mKeyboardView) return;
        if (isNumPad) {
            isNumPad = false;
            mKeyboardView.get().setKeyboard(mKeyboard);
        } else {
            isNumPad = true;
            mKeyboardView.get().setKeyboard(mNumPad);
        }
    }

    private void disconnectKeyboard() {
        parent.get().removeView(mKeyboardView.get());
        if (null != listener)
            listener.onKeyboardHidden();
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        InputConnection ic = getCurrentInputConnection();
        switch(primaryCode) {
            case 555:
                disconnectKeyboard();
                break;
            case -999:
                swapKeyboardLayout();
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
                disconnectKeyboard();
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
