package myau.ui.rise;

import myau.Myau;
import myau.util.GuiFonts;
import myau.module.Module;
import myau.property.Property;
import myau.property.properties.BooleanProperty;
import myau.property.properties.KeyProperty;
import myau.property.properties.ModeProperty;
import myau.ui.clickgui.GuiRender;
import myau.ui.rise.rows.BindRow;
import myau.ui.rise.rows.BooleanRow;
import myau.ui.rise.rows.ModeRow;
import myau.ui.rise.rows.RiseRow;
import myau.ui.rise.rows.SliderRow;
import net.minecraft.client.Minecraft;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class RiseModuleRow {
    private static final float CARD_HEIGHT = 38.0F;
    private final Module module;
    private final List<RiseRow> rows = new ArrayList<RiseRow>();
    private boolean expanded;
    private float x;
    private float y;
    private float width;

    public RiseModuleRow(Module module) {
        this.module = module;
        ArrayList<Property<?>> properties =
                Myau.propertyManager.properties.get(module.getClass());
        if (properties != null) {
            for (Property<?> property : properties) {
                RiseRow row = rowFor(property);
                if (row != null) {
                    this.rows.add(row);
                }
            }
        }
    }

    private static RiseRow rowFor(Property<?> property) {
        if (property instanceof BooleanProperty) {
            return new BooleanRow((BooleanProperty) property);
        }
        if (property instanceof KeyProperty) {
            return new BindRow((KeyProperty) property);
        }
        if (property instanceof ModeProperty) {
            return new ModeRow((ModeProperty) property);
        }
        if (SliderRow.handles(property)) {
            return new SliderRow(property);
        }
        return null;
    }

    public Module getModule() {
        return this.module;
    }

    public boolean isExpanded() {
        return this.expanded;
    }

    public float getHeight() {
        float height = CARD_HEIGHT;
        if (this.expanded) {
            for (RiseRow row : this.rows) {
                if (row != null && row.isVisible()) {
                    height += row.getHeight() + 2.0F;
                }
            }
        }
        return height;
    }

    public void layout(float x, float y, float width) {
        this.x = x;
        this.y = y;
        this.width = width;
        float rowY = y + CARD_HEIGHT + 2.0F;
        for (RiseRow row : this.rows) {
            if (!row.isVisible()) {
                continue;
            }
            row.layout(x + 8.0F, rowY, width - 16.0F);
            rowY += row.getHeight() + 2.0F;
        }
    }

    public void render(int mouseX, int mouseY, int accent, String tag) {
        GuiRender.drawRoundedRect(this.x, this.y, this.x + this.width,
                this.y + CARD_HEIGHT, 6.0F, new Color(22, 22, 26, 220).getRGB());
        boolean enabled = this.module.isEnabled();
        GuiFonts.drawStringWithShadow(
                this.module.getName(), this.x + 6.0F, this.y + 7.0F,
                enabled ? accent : new Color(200, 200, 200).getRGB());
        String[] suffix = this.module.getSuffix();
        if (suffix.length > 0 && !suffix[0].isEmpty()) {
            GuiFonts.drawStringWithShadow(suffix[0],
                    this.x + this.width - GuiFonts
                            .getStringWidth(suffix[0]) - 6.0F,
                    this.y + 7.0F, new Color(120, 120, 130).getRGB());
        } else if (tag != null) {
            GuiFonts.drawStringWithShadow(tag,
                    this.x + this.width - GuiFonts
                            .getStringWidth(tag) - 6.0F,
                    this.y + 7.0F, new Color(120, 120, 130).getRGB());
        }
        String hint = this.rows.isEmpty() ? ""
                : (this.expanded ? "-" : "+");
        if (!hint.isEmpty()) {
            GuiFonts.drawStringWithShadow(hint,
                    this.x + this.width - 12.0F, this.y + CARD_HEIGHT - 12.0F,
                    new Color(120, 120, 130).getRGB());
        }
        if (this.expanded) {
            for (RiseRow row : this.rows) {
                if (row.isVisible()) {
                    row.render(mouseX, mouseY);
                }
            }
        }
    }

    public boolean onClick(int mouseX, int mouseY, int button) {
        if (mouseX >= this.x && mouseX <= this.x + this.width
                && mouseY >= this.y && mouseY <= this.y + CARD_HEIGHT) {
            if (button == 0) {
                this.module.toggle();
                return true;
            }
            if (button == 1 && !this.rows.isEmpty()) {
                this.expanded = !this.expanded;
                return true;
            }
            return false;
        }
        if (this.expanded) {
            for (RiseRow row : this.rows) {
                if (row.isVisible() && row.onClick(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void onDrag(int mouseX, int mouseY) {
        if (!this.expanded) {
            return;
        }
        for (RiseRow row : this.rows) {
            row.onDrag(mouseX, mouseY);
        }
    }

    public void onRelease() {
        for (RiseRow row : this.rows) {
            row.onRelease();
        }
    }

    public void onKey(char typed, int key) {
        if (!this.expanded) {
            return;
        }
        for (RiseRow row : this.rows) {
            row.onKey(typed, key);
            if (row.isBinding()) {
                return;
            }
        }
    }

    public boolean isBinding() {
        if (!this.expanded) {
            return false;
        }
        for (RiseRow row : this.rows) {
            if (row.isBinding()) {
                return true;
            }
        }
        return false;
    }
}
