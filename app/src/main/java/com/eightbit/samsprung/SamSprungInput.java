package com.eightbit.samsprung;

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.eightbit.content.ScaledContext;

import java.lang.ref.SoftReference;

interface InputMethodListener {
    void onInputRequested(SamSprungInput instance);
}

@SuppressWarnings("deprecation")
public class SamSprungInput extends InputMethodService
        implements KeyboardView.OnKeyboardActionListener {

    private static InputMethodListener listener;

    private static SoftReference<KeyboardView> mKeyboardView;
    private static Keyboard mKeyboard;
    private Keyboard mNumPad;
    private static CoordinatorLayout parent;
    private static boolean isNumPad = false;

    private boolean caps = false;

    @Override
    public void onInitializeInterface() {
        mNumPad = new Keyboard(ScaledContext.cover(this), R.xml.keyboard_numpad);
        if (null != mKeyboardView)
            mKeyboardView.get().setOnKeyboardActionListener(this);
        super.onInitializeInterface();
    }

    @Override
    public boolean onShowInputRequested(int flags, boolean configChange) {
        if (null != listener) listener.onInputRequested(this);
        return true;
    }

    public static void setInputListener(InputMethodListener inputListener) {
        listener = inputListener;
    }

    public static void setInputMethod(
            KeyboardView keyBoardView,
            Keyboard keyboard,
            CoordinatorLayout coordinator
    ) {
        mKeyboardView = new SoftReference<>(keyBoardView);
        mKeyboard = keyboard;
        parent = coordinator;
        isNumPad = false;
    }

    @Override
    public View onCreateInputView() {
        return null;
    }

    private void swapKeyboardLayout() {
        if (isNumPad) {
            isNumPad = false;
            mKeyboardView.get().setKeyboard(mKeyboard);
        } else {
            isNumPad = true;
            mKeyboardView.get().setKeyboard(mNumPad);
        }

    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        InputConnection ic = getCurrentInputConnection();
        switch(primaryCode) {
            case 555:
                parent.removeView(mKeyboardView.get());
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
    public void swipeDown() { }

    @Override
    public void swipeLeft() { }

    @Override
    public void swipeRight() { }

    @Override
    public void swipeUp() { }
}
