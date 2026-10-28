package myau.module.modules;

import myau.module.Module;
import myau.property.properties.FloatProperty;
import myau.property.properties.ModeProperty;
import myau.ui.clickgui.ClickGui;
import myau.ui.rise.RiseGui;
import myau.util.ChatUtil;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

public class GuiModule extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private ClickGui clickGui;
    private RiseGui riseGui;
    public final ModeProperty style = new ModeProperty("Style", 0, new String[]{"CLASSIC", "RISE"});
    public final ModeProperty font = new ModeProperty("Font", 0,
            new String[]{"VANILLA", "SF-BOLD", "SF-REGULAR", "SF-UI", "PRODUCT-SANS",
                    "MSDF-SF", "MSDF-SF-BOLD", "MSDF-PRODUCT-SANS",
                    "MSDF-GOOGLE-SANS", "MSDF-GOOGLE-SANS-BOLD", "MSDF-TAHOMA",
                    "MSDF-TAHOMA-BOLD", "MSDF-VERDANA"});
    public final FloatProperty scale = new FloatProperty("GUI-Scale", 1.0F, 0.5F, 2.0F, 0.05F);
    public final FloatProperty fontSize = new FloatProperty("Font-Size", 1.0F, 0.5F, 2.0F, 0.05F);

    public GuiModule() {
        super("Click Gui", false);
        setKey(Keyboard.KEY_RSHIFT);
    }

    @Override
    public void onEnabled() {
        setEnabled(false);
        try {
            if (this.style.getValue() == 1) {
                if (riseGui == null) {
                    riseGui = new RiseGui();
                }
                mc.displayGuiScreen(riseGui);
            } else {
                if (clickGui == null) {
                    clickGui = new ClickGui();
                }
                mc.displayGuiScreen(clickGui);
            }
        } catch (Throwable failed) {
            failed.printStackTrace();
            ChatUtil.sendFormatted("&cclick gui failed to open: " + failed.getClass().getName()
                    + (failed.getMessage() == null ? "" : ": " + failed.getMessage()));
            StackTraceElement[] trace = failed.getStackTrace();
            for (int i = 0; i < Math.min(trace.length, 8); i++) {
                ChatUtil.sendFormatted("&7  at " + trace[i].toString());
            }
            Throwable cause = failed.getCause();
            while (cause != null && cause != failed) {
                ChatUtil.sendFormatted("&cCaused by: " + cause.getClass().getName()
                        + (cause.getMessage() == null ? "" : ": " + cause.getMessage()));
                failed = cause;
                cause = cause.getCause();
            }
        }
    }
}
