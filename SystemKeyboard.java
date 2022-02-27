package com.bhs.gondidarshanlite;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Typeface;
import android.inputmethodservice.InputMethodService;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.widget.FrameLayout;

import com.tavultesoft.kmea.KMHardwareKeyboardInterpreter;
import com.tavultesoft.kmea.KMManager;
import com.tavultesoft.kmea.KeyboardEventHandler;
import com.tavultesoft.kmea.data.Keyboard;

public class SystemKeyboard extends InputMethodService implements KeyboardEventHandler.OnKeyboardEventListener {
    private static View inputView = null;
    public static final String TAG = SystemKeyboard.class.getSimpleName();
    private KMHardwareKeyboardInterpreter interpreter = null;

    @Override
    public void onCreate() {
        super.onCreate();
        KMManager.setDebugMode(true);
        KMManager.addKeyboardEventListener(this);
        KMManager.initialize(getApplicationContext(), KMManager.KeyboardType.KEYBOARD_TYPE_SYSTEM);
        interpreter = new KMHardwareKeyboardInterpreter(getApplicationContext(), KMManager.KeyboardType.KEYBOARD_TYPE_SYSTEM);

        Keyboard kbInfo = null; // Font for OSK
        kbInfo = new Keyboard(
                "masaram_gondi", // Package ID - filename of the .kmp file
                "masaram_gondi", // Keyboard ID
                "Masaram Gondi", // Keyboard Name
                "gon-Gonm",             // Language ID
                "Gondi",          // Language Name
                "1.0",            // Keyboard Version
                null,             // URL to help documentation if available
                "",               // URL to latest .kmp file
                true,             // Boolean to show this is a new keyboard in the keyboard picker

                // Font information of the .ttf font to use in KMSample2 (for example "aava1.ttf").
                // basic_kbdtam99 doesn't include a font. Can set blank "" or KMManager.KMDefault_KeyboardFont
                // KMEA will use the font for the OSK, but the Android device determines the system font used for keyboard output
                "notosansmasaramgondiregular.ttf",  // Font for KMSample2
                "notosansmasaramgondiregular.ttf");
        KMManager.addKeyboard(this, kbInfo);

    }

    @Override
    public void onDestroy() {
        inputView = null;
        KMManager.removeKeyboardEventListener(this);
        interpreter = null; // Throw it away, since we're losing our application's context.
        KMManager.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onInitializeInterface() {
        super.onInitializeInterface();
    }

    @Override
    public View onCreateInputView() {
        if (inputView == null)
            inputView = KMManager.createInputView(this);

        // we must remove the inputView from its previous parent before returning it
        ViewGroup parent = (ViewGroup) inputView.getParent();
        if (parent != null)
            parent.removeView(inputView);

        return inputView;
    }

    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
        KMManager.updateSelectionRange(KMManager.KeyboardType.KEYBOARD_TYPE_SYSTEM, newSelStart, newSelEnd);
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        attribute.imeOptions |= EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_FLAG_NO_FULLSCREEN;
        super.onStartInput(attribute, restarting);
        KMManager.onStartInput(attribute, restarting);
        KMManager.resetContext(KMManager.KeyboardType.KEYBOARD_TYPE_SYSTEM);
        // User switched to a new input field so we should extract the text from input field
        // and pass it to Keyman Engine together with selection range
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ExtractedText icText = ic.getExtractedText(new ExtractedTextRequest(), 0);
            if (icText != null) {
                KMManager.updateText(KMManager.KeyboardType.KEYBOARD_TYPE_SYSTEM, icText.text.toString());
                int selStart = icText.startOffset + icText.selectionStart;
                int selEnd = icText.startOffset + icText.selectionEnd;
                KMManager.updateSelectionRange(KMManager.KeyboardType.KEYBOARD_TYPE_SYSTEM, selStart, selEnd);
            }
        }
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        setInputView(onCreateInputView());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        KMManager.onConfigurationChanged(newConfig);
    }

    @Override
    public void onConfigureWindow(Window win, boolean isFullscreen, boolean isCandidatesOnly) {
        super.onConfigureWindow(win, isFullscreen, isCandidatesOnly);
        win.setLayout(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
    }

    @Override
    public void onComputeInsets(Insets outInsets) {
        super.onComputeInsets(outInsets);
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Point size = new Point(0, 0);
        wm.getDefaultDisplay().getSize(size);

        int inputViewHeight = 0;
        if (inputView != null)
            inputViewHeight = inputView.getHeight();

        int bannerHeight = KMManager.getBannerHeight(this);
        int kbHeight = KMManager.getKeyboardHeight(this);
        outInsets.contentTopInsets = inputViewHeight - bannerHeight - kbHeight;
        outInsets.visibleTopInsets = outInsets.contentTopInsets;
        outInsets.touchableInsets = InputMethodService.Insets.TOUCHABLE_INSETS_REGION;
        outInsets.touchableRegion.set(0, outInsets.contentTopInsets, size.x, size.y);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    // Dismiss the keyboard if currently shown
                    if (isInputViewShown()) {
                        KMManager.hideSystemKeyboard();
                        return true;
                    }
                    break;
            }
        }

        return interpreter.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return interpreter.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
        return interpreter.onKeyMultiple(keyCode, count, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return interpreter.onKeyLongPress(keyCode, event);
    }

    @Override
    public void onKeyboardLoaded(KMManager.KeyboardType keyboardType) {

    }

    @Override
    public void onKeyboardChanged(String s) {

    }

    @Override
    public void onKeyboardShown() {

    }

    @Override
    public void onKeyboardDismissed() {

    }
}
