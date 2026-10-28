package myau.ui.rise.rows;

import myau.property.Property;
import myau.util.GuiFonts;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.PercentProperty;
import myau.ui.clickgui.GuiRender;
import net.minecraft.client.Minecraft;

import java.awt.Color;

public class SliderRow extends RiseRow {
    private static final int TRACK = new Color(40, 40, 48).getRGB();
    private final Property<?> property;
    private final double minimum;
    private final double maximum;
    private boolean held;

    public SliderRow(Property<?> property) {
        this.property = property;
        if (property instanceof IntProperty) {
            this.minimum = ((IntProperty) property).getMinimum();
            this.maximum = ((IntProperty) property).getMaximum();
        } else if (property instanceof PercentProperty) {
            this.minimum = ((PercentProperty) property).getMinimum();
            this.maximum = ((PercentProperty) property).getMaximum();
        } else {
            this.minimum = ((FloatProperty) property).getMinimum();
            this.maximum = ((FloatProperty) property).getMaximum();
        }
    }

    public static boolean handles(Property<?> property) {
        return property instanceof IntProperty
                || property instanceof FloatProperty
                || property instanceof PercentProperty;
    }

    private double current() {
        return ((Number) this.property.getValue()).doubleValue();
    }

    @Override
    public float getHeight() {
        return 18.0F;
    }

    @Override
    public void render(int mouseX, int mouseY) {
        double span = this.maximum - this.minimum;
        double filled = span <= 0.0 ? 0.0 : (this.current() - this.minimum) / span;
        String label = this.property.getName() + ": " + this.label();
        GuiFonts.drawStringWithShadow(
                label, this.x, this.y + 1.0F, new Color(200, 200, 200).getRGB());
        float top = this.y + 12.0F;
        GuiRender.drawRoundedRect(this.x, top, this.x + this.width, top + 3.0F, 1.5F, TRACK);
        GuiRender.drawRoundedRect(this.x, top,
                (float) (this.x + this.width * Math.max(0.0, Math.min(1.0, filled))), top + 3.0F,
                1.5F, new Color(24, 154, 255).getRGB());
    }

    private String label() {
        double value = this.current();
        if (value == Math.rint(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.format("%.2f", value);
    }

    @Override
    public boolean onClick(int mouseX, int mouseY, int button) {
        if (button == 0 && this.over(mouseX, mouseY, this.getHeight())) {
            this.held = true;
            this.onDrag(mouseX, mouseY);
            return true;
        }
        return false;
    }

    @Override
    public void onDrag(int mouseX, int mouseY) {
        if (!this.held) {
            return;
        }
        double fraction = (mouseX - this.x) / (double) this.width;
        double raw = this.minimum + Math.max(0.0, Math.min(1.0, fraction)) * (this.maximum - this.minimum);
        if (this.property instanceof FloatProperty) {
            this.property.setValue(((FloatProperty) this.property).snap((float) raw));
        } else {
            int rounded = (int) Math.round(raw);
            this.property.setValue(this.property instanceof IntProperty
                    ? ((IntProperty) this.property).snap(rounded)
                    : rounded);
        }
    }

    @Override
    public void onRelease() {
        this.held = false;
    }

    @Override
    public boolean isVisible() {
        return this.property.isVisible();
    }
}
