package macrosnik.util;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;

import java.awt.event.KeyEvent;

public final class NativeKeyCodeMapper {
    private NativeKeyCodeMapper() {
    }

    public static int toAwt(int nativeKeyCode) {
        return switch (nativeKeyCode) {
            case NativeKeyEvent.VC_ESCAPE -> KeyEvent.VK_ESCAPE;
            case NativeKeyEvent.VC_ENTER -> KeyEvent.VK_ENTER;
            case NativeKeyEvent.VC_TAB -> KeyEvent.VK_TAB;
            case NativeKeyEvent.VC_SPACE -> KeyEvent.VK_SPACE;
            case NativeKeyEvent.VC_BACKSPACE -> KeyEvent.VK_BACK_SPACE;
            case NativeKeyEvent.VC_DELETE -> KeyEvent.VK_DELETE;
            case NativeKeyEvent.VC_INSERT -> KeyEvent.VK_INSERT;
            case NativeKeyEvent.VC_HOME -> KeyEvent.VK_HOME;
            case NativeKeyEvent.VC_END -> KeyEvent.VK_END;
            case NativeKeyEvent.VC_PAGE_UP -> KeyEvent.VK_PAGE_UP;
            case NativeKeyEvent.VC_PAGE_DOWN -> KeyEvent.VK_PAGE_DOWN;
            case NativeKeyEvent.VC_UP -> KeyEvent.VK_UP;
            case NativeKeyEvent.VC_DOWN -> KeyEvent.VK_DOWN;
            case NativeKeyEvent.VC_LEFT -> KeyEvent.VK_LEFT;
            case NativeKeyEvent.VC_RIGHT -> KeyEvent.VK_RIGHT;

            case NativeKeyEvent.VC_SHIFT -> KeyEvent.VK_SHIFT;
            case NativeKeyEvent.VC_CONTROL -> KeyEvent.VK_CONTROL;
            case NativeKeyEvent.VC_ALT -> KeyEvent.VK_ALT;
            case NativeKeyEvent.VC_META -> KeyEvent.VK_WINDOWS;
            case NativeKeyEvent.VC_CAPS_LOCK -> KeyEvent.VK_CAPS_LOCK;
            case NativeKeyEvent.VC_NUM_LOCK -> KeyEvent.VK_NUM_LOCK;
            case NativeKeyEvent.VC_SCROLL_LOCK -> KeyEvent.VK_SCROLL_LOCK;
            case NativeKeyEvent.VC_PRINTSCREEN -> KeyEvent.VK_PRINTSCREEN;
            case NativeKeyEvent.VC_PAUSE -> KeyEvent.VK_PAUSE;

            case NativeKeyEvent.VC_F1 -> KeyEvent.VK_F1;
            case NativeKeyEvent.VC_F2 -> KeyEvent.VK_F2;
            case NativeKeyEvent.VC_F3 -> KeyEvent.VK_F3;
            case NativeKeyEvent.VC_F4 -> KeyEvent.VK_F4;
            case NativeKeyEvent.VC_F5 -> KeyEvent.VK_F5;
            case NativeKeyEvent.VC_F6 -> KeyEvent.VK_F6;
            case NativeKeyEvent.VC_F7 -> KeyEvent.VK_F7;
            case NativeKeyEvent.VC_F8 -> KeyEvent.VK_F8;
            case NativeKeyEvent.VC_F9 -> KeyEvent.VK_F9;
            case NativeKeyEvent.VC_F10 -> KeyEvent.VK_F10;
            case NativeKeyEvent.VC_F11 -> KeyEvent.VK_F11;
            case NativeKeyEvent.VC_F12 -> KeyEvent.VK_F12;

            case NativeKeyEvent.VC_0 -> KeyEvent.VK_0;
            case NativeKeyEvent.VC_1 -> KeyEvent.VK_1;
            case NativeKeyEvent.VC_2 -> KeyEvent.VK_2;
            case NativeKeyEvent.VC_3 -> KeyEvent.VK_3;
            case NativeKeyEvent.VC_4 -> KeyEvent.VK_4;
            case NativeKeyEvent.VC_5 -> KeyEvent.VK_5;
            case NativeKeyEvent.VC_6 -> KeyEvent.VK_6;
            case NativeKeyEvent.VC_7 -> KeyEvent.VK_7;
            case NativeKeyEvent.VC_8 -> KeyEvent.VK_8;
            case NativeKeyEvent.VC_9 -> KeyEvent.VK_9;

            case NativeKeyEvent.VC_A -> KeyEvent.VK_A;
            case NativeKeyEvent.VC_B -> KeyEvent.VK_B;
            case NativeKeyEvent.VC_C -> KeyEvent.VK_C;
            case NativeKeyEvent.VC_D -> KeyEvent.VK_D;
            case NativeKeyEvent.VC_E -> KeyEvent.VK_E;
            case NativeKeyEvent.VC_F -> KeyEvent.VK_F;
            case NativeKeyEvent.VC_G -> KeyEvent.VK_G;
            case NativeKeyEvent.VC_H -> KeyEvent.VK_H;
            case NativeKeyEvent.VC_I -> KeyEvent.VK_I;
            case NativeKeyEvent.VC_J -> KeyEvent.VK_J;
            case NativeKeyEvent.VC_K -> KeyEvent.VK_K;
            case NativeKeyEvent.VC_L -> KeyEvent.VK_L;
            case NativeKeyEvent.VC_M -> KeyEvent.VK_M;
            case NativeKeyEvent.VC_N -> KeyEvent.VK_N;
            case NativeKeyEvent.VC_O -> KeyEvent.VK_O;
            case NativeKeyEvent.VC_P -> KeyEvent.VK_P;
            case NativeKeyEvent.VC_Q -> KeyEvent.VK_Q;
            case NativeKeyEvent.VC_R -> KeyEvent.VK_R;
            case NativeKeyEvent.VC_S -> KeyEvent.VK_S;
            case NativeKeyEvent.VC_T -> KeyEvent.VK_T;
            case NativeKeyEvent.VC_U -> KeyEvent.VK_U;
            case NativeKeyEvent.VC_V -> KeyEvent.VK_V;
            case NativeKeyEvent.VC_W -> KeyEvent.VK_W;
            case NativeKeyEvent.VC_X -> KeyEvent.VK_X;
            case NativeKeyEvent.VC_Y -> KeyEvent.VK_Y;
            case NativeKeyEvent.VC_Z -> KeyEvent.VK_Z;

            case NativeKeyEvent.VC_COMMA -> KeyEvent.VK_COMMA;
            case NativeKeyEvent.VC_PERIOD -> KeyEvent.VK_PERIOD;
            case NativeKeyEvent.VC_SLASH -> KeyEvent.VK_SLASH;
            case NativeKeyEvent.VC_SEMICOLON -> KeyEvent.VK_SEMICOLON;
            case NativeKeyEvent.VC_QUOTE -> KeyEvent.VK_QUOTE;
            case NativeKeyEvent.VC_OPEN_BRACKET -> KeyEvent.VK_OPEN_BRACKET;
            case NativeKeyEvent.VC_CLOSE_BRACKET -> KeyEvent.VK_CLOSE_BRACKET;
            case NativeKeyEvent.VC_BACK_SLASH -> KeyEvent.VK_BACK_SLASH;
            case NativeKeyEvent.VC_MINUS -> KeyEvent.VK_MINUS;
            case NativeKeyEvent.VC_EQUALS -> KeyEvent.VK_EQUALS;
            case NativeKeyEvent.VC_BACKQUOTE -> KeyEvent.VK_BACK_QUOTE;

            default -> KeyEvent.VK_UNDEFINED;
        };
    }
}