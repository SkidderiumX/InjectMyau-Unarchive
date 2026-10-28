package myau.ui.rise.rows;

import myau.property.properties.BooleanProperty;
import myau.util.GuiFonts;
import net.minecraft.client.Minecraft;

import java.awt.Color;

public class BooleanRow extends RiseRow {
    private static final int ENABLED = new Color(24, 154, 255).getRGB();
    private static final int DISABLED = new Color(130, 130, 130).getRGB();
    private final BooleanProperty property;

    public BooleanRow(BooleanProperty property) {
        this.property = property;
    }

    @Override
    public float getHeight() {
        return 14.0F;
    }

    @Override
    public void render(int mouseX, int mouseY) {
        boolean value = this.property.getValue();
        GuiFonts.drawStringWithShadow(
                this.property.getName(), this.x, this.y + 2.0F, new Color(200, 200, 200).getRGB());
        String state = value ? "ON" : "OFF";
        GuiFonts.drawStringWithShadow(state,
                this.x + this.width - GuiFonts.getStringWidth(state) - 2.0F,
                this.y + 2.0F, value ? ENABLED : DISABLED);
    }

    @Override
    public boolean onClick(int mouseX, int mouseY, int button) {
        if (button == 0 && this.over(mouseX, mouseY, this.getHeight())) {
            this.property.setValue(!this.property.getValue());
            return true;
        }
        return false;
    }

    @Override
    public boolean isVisible() {
        return this.property.isVisible();
    }
}
