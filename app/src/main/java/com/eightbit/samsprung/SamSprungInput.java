package com.eightbit.samsprung;

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

@SuppressWarnings("deprecation")
public class SamSprungInput extends InputMethodService
        implements KeyboardView.OnKeyboardActionListener {

    private static KeyboardView mKeyboardView;
    private static Keyboard mKeyboard;
    private static CoordinatorLayout parent;

    private boolean caps = false;

    @Override
    public void onBindInput() {
        super.onBindInput();
    }

    @Override
    public void onInitializeInterface() {
        mKeyboardView.setOnKeyboardActionListener(this);
        super.onInitializeInterface();
    }

    @Override
    public boolean onShowInputRequested(int flags, boolean configChange) {
        return true;
    }

    public static void setKeyboard(Keyboard keyboard, KeyboardView keyBoardView, CoordinatorLayout coordinator) {
        mKeyboard = keyboard;
        mKeyboardView = keyBoardView;
        parent = coordinator;
    }

    @Override
    public View onCreateInputView() {
        // mKeyboardView.setOnKeyboardActionListener(this);
        return null;
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        InputConnection ic = getCurrentInputConnection();
        switch(primaryCode){
            case Keyboard.KEYCODE_DELETE :
                ic.deleteSurroundingText(1, 0);
                break;
            case Keyboard.KEYCODE_SHIFT:
                caps = !caps;
                mKeyboard.setShifted(caps);
                mKeyboardView.invalidateAllKeys();
                break;
            case Keyboard.KEYCODE_DONE:
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                parent.removeView(mKeyboardView);
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
    public void swipeLeft() {
        parent.removeView(mKeyboardView);
    }

    @Override
    public void swipeRight() {
        parent.removeView(mKeyboardView);
    }

    @Override
    public void swipeUp() { }
}
