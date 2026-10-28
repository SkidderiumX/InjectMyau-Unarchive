package myau.ui.rise.rows;

import myau.property.properties.KeyProperty;
import myau.util.GuiFonts;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

import java.awt.Color;

public class BindRow extends RiseRow {
    private final KeyProperty property;
    private boolean binding;

    public BindRow(KeyProperty property) {
        this.property = property;
    }

    @Override
    public float getHeight() {
        return 14.0F;
    }

    @Override
    public void render(int mouseX, int mouseY) {
        String text = this.binding ? "Press a key..."
                : "Bind: '" + this.property.getKeyName() + "'";
        GuiFonts.drawStringWithShadow(
                text, this.x, this.y + 2.0F, new Color(150, 170, 220).getRGB());
    }

    @Override
    public boolean onClick(int mouseX, int mouseY, int button) {
        if (!this.over(mouseX, mouseY, this.getHeight())) {
            return false;
        }
        if (button == 0) {
            this.binding = !this.binding;
            return true;
        }
        if (this.binding && button > 1) {
            this.property.setValue(button + KeyProperty.MOUSE_OFFSET);
            this.binding = false;
            return true;
        }
        return false;
    }

    @Override
    public void onKey(char typed, int key) {
        if (!this.binding) {
            return;
        }
        this.property.setValue(key == Keyboard.KEY_ESCAPE ? 0 : key);
        this.binding = false;
    }

    @Override
    public boolean isBinding() {
        return this.binding;
    }

    @Override
    public boolean isVisible() {
        return this.property.isVisible();
    }
}
