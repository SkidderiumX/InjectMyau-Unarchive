package myau.module.modules;

import myau.module.Module;
import myau.module.ModuleManager;
import myau.ui.clickgui.ClickGui;
import myau.util.ChatUtil;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

public class GuiModule extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private ClickGui clickGui;
    private ModuleManager clickGuiOwner;

    public GuiModule() {
        super("Click Gui", false);
        setKey(Keyboard.KEY_RSHIFT);
    }

    @Override
    public void onEnabled() {
        setEnabled(false);
        try {
            // Rebuild when the client has been (re)initialised -- a cached ClickGui
            // would still hold panels wired to the previous module instances.
            ModuleManager manager = myau.Myau.moduleManager;
            if (clickGui == null || clickGuiOwner != manager) {
                clickGui = new ClickGui();
                clickGuiOwner = manager;
            }
        } catch (Throwable failed) {
            ChatUtil.sendFormatted("&cclick gui failed to open: " + failed.getMessage());
            return;
        }
        mc.displayGuiScreen(clickGui);
    }
}
