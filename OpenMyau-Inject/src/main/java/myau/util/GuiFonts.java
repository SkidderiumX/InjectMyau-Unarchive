package myau.util;

import myau.Myau;
import myau.module.modules.GuiModule;
import myau.util.font.Fonts;
import myau.util.font.RavenFontRenderer;

/**
 * Font used by both ClickGUIs. Follows the GuiModule "Font" setting, which
 * offers the same fonts as the HUD module's arraylist-font. Index 0 renders
 * with the vanilla font, so the default look is unchanged.
 */
public final class GuiFonts {
    private GuiFonts() {
    }

    public static RavenFontRenderer get() {
        int selection = 0;
        float size = 1.0F;
        try {
            if (Myau.moduleManager != null) {
                GuiModule gui = (GuiModule) Myau.moduleManager.modules.get(GuiModule.class);
                if (gui != null) {
                    selection = gui.font.getValue();
                    size = gui.fontSize.getValue();
                }
            }
        } catch (Throwable ignored) {
        }
        if (size < 0.5F || size > 2.0F || Float.isNaN(size)) {
            size = 1.0F;
        }
        try {
            return Fonts.arraylist(selection, size);
        } catch (Throwable ignored) {
            return Fonts.arraylist(0, 1.0F);
        }
    }

    public static int drawString(String text, float x, float y, int color) {
        return get().drawString(text, x, y, color, false);
    }

    public static int drawStringWithShadow(String text, float x, float y, int color) {
        return get().drawStringWithShadow(text, x, y, color);
    }

    public static int getStringWidth(String text) {
        return get().getStringWidth(text);
    }
}
