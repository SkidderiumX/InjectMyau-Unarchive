package myau.ui.clickgui.components;

import myau.config.FeatureConfigStore;
import myau.ui.clickgui.GuiRender;
import myau.ui.clickgui.Timer;
import myau.util.GuiFonts;
import net.minecraft.client.Minecraft;

import java.awt.Color;

public class ConfigRowComponent extends Component {
    private static final float ROW_HEIGHT = 16.0F;
    private static final long HOVER_DURATION = 75L;
    private static final int HOVER_ALPHA = 120;
    private static final int HOVER_COLOR = new Color(0, 0, 0, HOVER_ALPHA).getRGB();
    private static final int NAME_COLOR = new Color(220, 220, 220).getRGB();
    public final String configName;
    public final CategoryComponent category;
    private float offset;
    private boolean hovering;
    private boolean hoverStarted;
    private Timer hoverTimer;

    public ConfigRowComponent(String configName, CategoryComponent category, float offset) {
        this.configName = configName;
        this.category = category;
        this.offset = offset;
    }

    private float hoverAlpha() {
        if (this.hoverTimer == null) {
            return this.hovering ? HOVER_ALPHA : 0.0F;
        }
        return this.hovering
                ? this.hoverTimer.getValueFloat(0.0F, HOVER_ALPHA, Timer.CUBIC_IN_OUT)
                : HOVER_ALPHA - this.hoverTimer.getValueFloat(0.0F, HOVER_ALPHA, Timer.CUBIC_IN_OUT);
    }

    private boolean overRow(int mouseX, int mouseY) {
        float x = this.category.getX();
        float y = this.category.getModuleY() + this.offset;
        return mouseX > x && mouseX < x + this.category.getWidth()
                && mouseY > y && mouseY < y + ROW_HEIGHT;
    }

    @Override
    public void render() {
        float x = this.category.getX();
        float y = this.category.getY() + this.offset;
        if (this.hovering || this.hoverTimer != null) {
            float alpha = this.hoverAlpha();
            if (alpha <= 0.0F) {
                this.hoverTimer = null;
            } else {
                GuiRender.drawRoundedRect(x, y, x + this.category.getWidth(), y + ROW_HEIGHT, 8.0F,
                        HOVER_COLOR & 0xFFFFFF | (int) alpha << 24);
            }
        }
        GuiFonts.drawStringWithShadow(this.configName,
                x + this.category.getWidth() / 2.0F - GuiFonts.getStringWidth(this.configName) / 2.0F,
                y + 4.0F,
                NAME_COLOR);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        if (this.overRow(mouseX, mouseY) && this.category.isOpened()) {
            if (!this.hovering && this.hoverTimer == null) {
                (this.hoverTimer = new Timer(HOVER_DURATION)).start();
                this.hoverStarted = true;
            }
            this.hovering = true;
        } else {
            if (this.hovering && this.hoverStarted) {
                (this.hoverTimer = new Timer(HOVER_DURATION)).start();
            }
            this.hoverStarted = false;
            this.hovering = false;
        }
    }

    @Override
    public boolean onClick(int mouseX, int mouseY, int button) {
        if (button == 0 && this.overRow(mouseX, mouseY)) {
            FeatureConfigStore.load(this.configName);
            return true;
        }
        return false;
    }

    @Override
    public void updateHeight(float offset) {
        this.offset = offset;
    }

    @Override
    public float getOffset() {
        return this.offset;
    }

    @Override
    public float getHeightF() {
        return ROW_HEIGHT;
    }
}
