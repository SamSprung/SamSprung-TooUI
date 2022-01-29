package com.eightbit.samsprung;

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import java.lang.ref.SoftReference;

@SuppressWarnings("deprecation")
public class SamSprungInput extends InputMethodService
        implements KeyboardView.OnKeyboardActionListener {

    private static SoftReference<KeyboardView> mKeyboardView;
    private static Keyboard mKeyboard;
    private static CoordinatorLayout parent;

    private boolean caps = false;

    @Override
    public void onInitializeInterface() {
        mKeyboardView.get().setOnKeyboardActionListener(this);
        super.onInitializeInterface();
    }

    @Override
    public boolean onShowInputRequested(int flags, boolean configChange) {
        return true;
    }

    public static void setInputMethod(Keyboard keyboard, KeyboardView keyBoardView, CoordinatorLayout coordinator) {
        mKeyboard = keyboard;
        mKeyboardView = new SoftReference<>(keyBoardView);
        parent = coordinator;
    }

    @Override
    public View onCreateInputView() {
        return null;
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        InputConnection ic = getCurrentInputConnection();
        if (keyCodes[0] == -999) return;
        switch(primaryCode){
            case Keyboard.KEYCODE_DELETE :
                ic.deleteSurroundingText(1, 0);
                break;
            case Keyboard.KEYCODE_SHIFT:
                caps = !caps;
                mKeyboard.setShifted(caps);
                mKeyboardView.get().invalidateAllKeys();
                break;
            case Keyboard.KEYCODE_DONE:
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                parent.removeView(mKeyboardView.get());
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
    public void swipeDown() {
        parent.removeView(mKeyboardView.get());
    }

    @Override
    public void swipeLeft() {
        mKeyboard = new Keyboard(parent.getContext(), R.xml.keyboard_numpad);
        mKeyboardView.get().setKeyboard(mKeyboard);
    }

    @Override
    public void swipeRight() {
        mKeyboard = new Keyboard(parent.getContext(), R.xml.keyboard_qwerty);
        mKeyboardView.get().setKeyboard(mKeyboard);
    }

    @Override
    public void swipeUp() { }
}
