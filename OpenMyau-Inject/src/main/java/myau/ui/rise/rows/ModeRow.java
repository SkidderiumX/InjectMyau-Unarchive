package myau.ui.rise.rows;

import myau.property.properties.ModeProperty;
import myau.util.GuiFonts;
import net.minecraft.client.Minecraft;

import java.awt.Color;

public class ModeRow extends RiseRow {
    private final ModeProperty property;

    public ModeRow(ModeProperty property) {
        this.property = property;
    }

    @Override
    public float getHeight() {
        return 14.0F;
    }

    @Override
    public void render(int mouseX, int mouseY) {
        GuiFonts.drawStringWithShadow(
                this.property.getName(), this.x, this.y + 2.0F, new Color(200, 200, 200).getRGB());
        String value = this.property.getModeString();
        GuiFonts.drawStringWithShadow(value,
                this.x + this.width - GuiFonts.getStringWidth(value) - 2.0F,
                this.y + 2.0F, new Color(85, 170, 255).getRGB());
    }

    @Override
    public boolean onClick(int mouseX, int mouseY, int button) {
        if (!this.over(mouseX, mouseY, this.getHeight())) {
            return false;
        }
        if (button == 0) {
            this.property.nextMode();
            return true;
        }
        if (button == 1) {
            this.property.previousMode();
            return true;
        }
        return false;
    }

    @Override
    public boolean isVisible() {
        return this.property.isVisible();
    }
}
