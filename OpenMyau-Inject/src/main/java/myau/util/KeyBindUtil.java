package myau.util;

import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.HashMap;
import java.util.HashSet;

public class KeyBindUtil {
    /**
     * Two down-edge observations closer together than this are considered the
     * same physical press seen by two separate hooks, while anything further
     * apart is treated as a genuinely new press.
     */
    private static final long DUPLICATE_WINDOW_MS = 80L;
    public static String getKeyName(int keyCode) {
        if (keyCode < 0) {
            int mouseButton = keyCode + 100;
            switch (mouseButton) {
                case 0:
                    return "LMB";
                case 1:
                    return "RMB";
                case 2:
                    return "MMB";
                case 3:
                    return "MOUSE3";
                case 4:
                    return "MOUSE4";
                case 5:
                    return "MOUSE5";
                case 6:
                    return "MOUSE6";
                case 7:
                    return "MOUSE7";
                default:
                    String buttonName = Mouse.getButtonName(mouseButton);
                    return buttonName != null ? buttonName : "MOUSE" + mouseButton;
            }
        }
        if (keyCode >= Keyboard.KEYBOARD_SIZE) {
            return "KEY" + keyCode;
        }
        String name = Keyboard.getKeyName(keyCode);
        return name != null ? name : "KEY" + keyCode;
    }

    public static boolean isKeyDown(int keyCode) {
        if (keyCode < 0) {
            return Mouse.isButtonDown(keyCode + 100);
        }
        return keyCode > 0 && keyCode < Keyboard.KEYBOARD_SIZE && Keyboard.isKeyDown(keyCode);
    }

    public static void updateKeyState(int keyCode) {
        KeyBindUtil.setKeyBindState(keyCode, KeyBindUtil.isKeyDown(keyCode));
    }

    private static final ThreadLocal<Boolean> SYNTHETIC = new ThreadLocal<Boolean>();
    private static final HashSet<Integer> HELD_KEYS = new HashSet<Integer>();
    private static final HashMap<Integer, Long> LAST_DOWN_MS = new HashMap<Integer, Long>();
    public static boolean isSynthetic() {
        return Boolean.TRUE.equals(SYNTHETIC.get());
    }

    /**
     * Press-edge tracking for real (non-synthetic) key input. The same physical
     * press can be observed more than once: separate hooks listen to
     * KeyBinding.setKeyBindState and GuiScreen.handleKeyboardInput (plus the mixin
     * redirect inside runTick), and screens with repeat events re-fire held keys.
     * Returns true only for the first down edge of a key so a bound module toggles
     * once per press; releases clear the tracked state and always return true.
     *
     * Releases can also be lost entirely (window unfocused, KeyBinding.unPressAllKeys
     * when a GUI opens, cancelled mouse events), which would leave a stale "held"
     * entry and swallow the next real press -- the "press twice to toggle" bug.
     * Therefore a down edge on an already-held key is re-accepted as a fresh press
     * when it cannot be a typematic duplicate: repeat events are disabled (vanilla
     * in-game state) and it is well outside the duplicate window.
     */
    public static synchronized boolean trackKeyEvent(int keyCode, boolean pressed) {
        if (pressed) {
            long now = System.currentTimeMillis();
            if (HELD_KEYS.add(keyCode)) {
                LAST_DOWN_MS.put(keyCode, now);
                return true;
            }
            long age = now - recentDownTime(keyCode);
            LAST_DOWN_MS.put(keyCode, now);
            return onePressPerKey() && age >= DUPLICATE_WINDOW_MS;
        }
        HELD_KEYS.remove(keyCode);
        LAST_DOWN_MS.remove(keyCode);
        return true;
    }

    private static long recentDownTime(int keyCode) {
        Long time = LAST_DOWN_MS.get(keyCode);
        return time == null ? 0L : time;
    }

    /**
     * True when repeat events are disabled, so a down edge on a held key cannot be
     * a typematic repeat and is more likely a stale entry followed by a real press.
     */
    private static boolean onePressPerKey() {
        try {
            return !Keyboard.areRepeatEventsEnabled();
        } catch (Throwable unknownLwjgl) {
            return false;
        }
    }

    /** Forgets every tracked key. Safe even when the key is still physically down. */
    public static synchronized void clearHeldKeys() {
        HELD_KEYS.clear();
        LAST_DOWN_MS.clear();
    }

    public static void setKeyBindState(int keyCode, boolean pressed) {
        SYNTHETIC.set(Boolean.TRUE);
        try {
            KeyBinding.setKeyBindState(keyCode, pressed);
        } finally {
            SYNTHETIC.remove();
        }
    }
    public static void pressKeyOnce(int keyCode) {
        KeyBinding.onTick(keyCode);
    }
}
