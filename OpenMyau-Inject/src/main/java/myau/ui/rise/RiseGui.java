package myau.ui.rise;

import myau.Myau;
import myau.util.GuiFonts;
import myau.config.Config;
import myau.module.Module;
import myau.module.modules.GuiModule;
import myau.module.modules.Theme;
import myau.ui.clickgui.Categories;
import myau.ui.clickgui.Category;
import myau.ui.clickgui.GuiRender;
import myau.util.Themes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RiseGui extends GuiScreen {
    private static final float WINDOW_WIDTH = 440.0F;
    private static final float WINDOW_HEIGHT = 350.0F;
    private static final float SIDEBAR_WIDTH = 110.0F;
    private static final float TITLE_HEIGHT = 20.0F;

    private static final int SEARCH = -1;
    private static final int CONFIGS = -2;
    private static final int THEMES = -3;

    private float windowX;
    private float windowY;
    private boolean dragging;
    private float grabX;
    private float grabY;
    private int selection = SEARCH;
    private float scroll;
    private final List<RiseModuleRow> rows = new ArrayList<RiseModuleRow>();
    private final Map<Module, String> categoryOf = new LinkedHashMap<Module, String>();
    private final List<ThemeCell> themeCells = new ArrayList<ThemeCell>();
    private String searchText = "";
    private boolean searchFocused;
    private final List<String> configNames = new ArrayList<String>();

    public RiseGui() {
        for (Module module : Myau.moduleManager.modules.values()) {
            this.rows.add(new RiseModuleRow(module));
        }
        for (Category category : Category.values()) {
            for (Module module : Categories.modulesOf(category)) {
                if (!this.categoryOf.containsKey(module)) {
                    this.categoryOf.put(module, category.getLabel());
                }
            }
        }
        this.reloadConfigs();
    }

    private void reloadConfigs() {
        this.configNames.clear();
        File dir = new File("./config/Myau/");
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".json")) {
                String name = file.getName();
                this.configNames.add(name.substring(0, name.length() - 5));
            }
        }
    }

    private static int accent() {
        try {
            Theme theme = (Theme) Myau.moduleManager.modules.get(Theme.class);
            if (theme != null) {
                return theme.getTheme().getPrimary().getRGB();
            }
        } catch (Throwable ignored) {
        }
        return new Color(24, 154, 255).getRGB();
    }

    private List<RiseModuleRow> visibleRows(String query) {
        List<RiseModuleRow> visible = new ArrayList<RiseModuleRow>();
        for (RiseModuleRow row : this.rows) {
            if (this.selection >= 0) {
                String label = this.categoryOf.get(row.getModule());
                Category category = Category.values()[this.selection];
                if (label == null || !label.equals(category.getLabel())) {
                    continue;
                }
            } else if (this.selection == SEARCH) {
                if (query != null && !row.getModule().getName().toLowerCase().contains(query)) {
                    continue;
                }
            } else {
                continue;
            }
            visible.add(row);
        }
        return visible;
    }

    public static float guiZoom() {
        try {
            GuiModule gui = (GuiModule) Myau.moduleManager.modules.get(GuiModule.class);
            if (gui != null) {
                return Math.max(0.5F, Math.min(2.0F, gui.scale.getValue()));
            }
        } catch (Throwable ignored) {
        }
        return 1.0F;
    }

    private static int zoomedMouse(int mouse, float zoom) {
        return Math.round(mouse / zoom);
    }

    @Override
    public void initGui() {
        super.initGui();
        float zoom = guiZoom();
        this.windowX = (this.width / zoom - WINDOW_WIDTH) / 2.0F;
        this.windowY = (this.height / zoom - WINDOW_HEIGHT) / 2.0F;
    }

    private float contentLeft() {
        return this.windowX + SIDEBAR_WIDTH + 8.0F;
    }

    private float contentWidth() {
        return WINDOW_WIDTH - SIDEBAR_WIDTH - 16.0F;
    }

    private float contentTop() {
        return this.windowY + TITLE_HEIGHT + 8.0F + (this.selection == SEARCH ? 24.0F : 0.0F);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        float zoom = guiZoom();
        GuiRender.setRenderScale(zoom);
        mouseX = zoomedMouse(mouseX, zoom);
        mouseY = zoomedMouse(mouseY, zoom);
        drawRect(0, 0, this.width, this.height, new Color(0, 0, 0, 100).getRGB());
        int accent = accent();
        GL11.glPushMatrix();
        GL11.glScalef(zoom, zoom, 1.0F);
        GuiRender.drawRoundedRect(this.windowX, this.windowY,
                this.windowX + WINDOW_WIDTH, this.windowY + WINDOW_HEIGHT,
                8.0F, new Color(12, 12, 16, 230).getRGB());
        GuiRender.drawRoundedRect(this.windowX, this.windowY,
                this.windowX + SIDEBAR_WIDTH, this.windowY + WINDOW_HEIGHT,
                8.0F, new Color(18, 18, 24, 230).getRGB());
        GuiFonts.drawStringWithShadow("Myau " + Myau.version,
                this.windowX + 10.0F, this.windowY + 6.0F, accent);

        float entryY = this.windowY + TITLE_HEIGHT + 6.0F;
        entryY = this.sidebarEntry("Search", SEARCH, mouseX, mouseY, entryY);
        for (int i = 0; i < Category.values().length; i++) {
            entryY = this.sidebarEntry(Category.values()[i].getLabel(), i, mouseX, mouseY, entryY);
        }
        entryY = this.sidebarEntry("Configs", CONFIGS, mouseX, mouseY, entryY);
        entryY = this.sidebarEntry("Themes", THEMES, mouseX, mouseY, entryY);

        String query = this.selection == SEARCH ? this.searchText.trim().toLowerCase() : null;
        if (this.selection == SEARCH) {
            this.drawSearchBox();
        }

        float top = this.contentTop();
        float bottom = this.windowY + WINDOW_HEIGHT - 8.0F;
        float contentHeight = this.contentHeight(query);
        float maxScroll = Math.max(0.0F, contentHeight - (bottom - top));
        if (this.scroll > maxScroll) {
            this.scroll = maxScroll;
        }
        if (this.scroll < 0.0F) {
            this.scroll = 0.0F;
        }
        GuiRender.scissorPush(this.contentLeft(), top, this.contentWidth(), bottom - top);
        if (this.selection == CONFIGS) {
            this.renderConfigs(top - this.scroll, accent);
        } else if (this.selection == THEMES) {
            this.renderThemes(top - this.scroll, accent);
        } else {
            float rowY = top - this.scroll;
            for (RiseModuleRow row : this.visibleRows(query)) {
                row.layout(this.contentLeft(), rowY, this.contentWidth());
                boolean tag = this.selection == SEARCH;
                row.render(mouseX, mouseY, accent,
                        tag ? this.categoryOf.get(row.getModule()) : null);
                row.onDrag(mouseX, mouseY);
                rowY += row.getHeight() + 6.0F;
            }
        }
        GuiRender.scissorPop();

        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            this.scroll -= (wheel / 120.0F) * 20.0F;
        }
        GL11.glPopMatrix();
        GuiRender.setRenderScale(1.0F);
    }

    private float sidebarEntry(String label, int id, int mouseX, int mouseY, float y) {
        boolean selected = this.selection == id;
        boolean hovered = mouseX >= this.windowX + 4.0F && mouseX <= this.windowX + SIDEBAR_WIDTH - 4.0F
                && mouseY >= y && mouseY <= y + 14.0F;
        if (selected) {
            drawRect((int) (this.windowX + 4.0F), (int) y,
                    (int) (this.windowX + SIDEBAR_WIDTH - 4.0F), (int) (y + 14.0F),
                    new Color(30, 30, 40, 230).getRGB());
        }
        GuiFonts.drawStringWithShadow(label, this.windowX + 10.0F, y + 3.0F,
                selected ? accent() : (hovered ? new Color(220, 220, 220).getRGB() : new Color(140, 140, 150).getRGB()));
        return y + 17.0F;
    }

    private void drawSearchBox() {
        float x = this.contentLeft();
        float y = this.windowY + TITLE_HEIGHT + 8.0F;
        drawRect((int) x, (int) y, (int) (x + this.contentWidth()), (int) (y + 16.0F),
                new Color(25, 25, 32, 230).getRGB());
        String shown = this.searchText + (this.searchFocused && (System.currentTimeMillis() / 500L) % 2L == 0L ? "_" : "");
        GuiFonts.drawStringWithShadow(shown.isEmpty() ? "Search..." : shown,
                x + 4.0F, y + 4.0F, new Color(200, 200, 200).getRGB());
    }

    private boolean overSearchBox(int mouseX, int mouseY) {
        float x = this.contentLeft();
        float y = this.windowY + TITLE_HEIGHT + 8.0F;
        return mouseX >= x && mouseX <= x + this.contentWidth() && mouseY >= y && mouseY <= y + 16.0F;
    }

    private float contentHeight(String query) {
        if (this.selection == CONFIGS) {
            return this.configNames.size() * 17.0F;
        }
        if (this.selection == THEMES) {
            return this.layoutThemes(0.0F);
        }
        float height = 0.0F;
        for (RiseModuleRow row : this.visibleRows(query)) {
            height += row.getHeight() + 6.0F;
        }
        return height;
    }

    private void renderConfigs(float y, int accent) {
        for (String name : this.configNames) {
            boolean current = name.equals(Config.lastConfig);
            GuiFonts.drawStringWithShadow((current ? "> " : "") + name,
                    this.contentLeft(), y + 3.0F,
                    current ? accent : new Color(200, 200, 200).getRGB());
            y += 17.0F;
        }
    }

    private static final class ThemeCell {
        final int index;
        final float x;
        final float y;
        final float width;
        final float height;

        ThemeCell(int index, float x, float y, float width, float height) {
            this.index = index;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    private float layoutThemes(float top) {
        this.themeCells.clear();
        float cellWidth = 56.0F;
        float cellHeight = 26.0F;
        float gap = 6.0F;
        int columns = Math.max(1, (int) ((this.contentWidth() + gap) / (cellWidth + gap)));
        float y = top;
        Themes.ThemeGroup[] groups = Themes.ThemeGroup.values();
        Themes[] values = Themes.values();
        for (int g = 0; g < groups.length; g++) {
            java.util.List<Integer> indices = new java.util.ArrayList<Integer>();
            for (int i = 0; i < values.length; i++) {
                if (values[i].getGroup() == groups[g]) {
                    indices.add(i);
                }
            }
            if (indices.isEmpty()) {
                continue;
            }
            y += 18.0F;
            int col = 0;
            float rowY = y;
            for (int k = 0; k < indices.size(); k++) {
                float x = this.contentLeft() + col * (cellWidth + gap);
                this.themeCells.add(new ThemeCell(indices.get(k), x, rowY, cellWidth, cellHeight));
                col++;
                if (col >= columns) {
                    col = 0;
                    rowY += cellHeight + gap;
                }
            }
            if (col != 0) {
                rowY += cellHeight + gap;
            }
            y = rowY + 4.0F;
        }
        return y - top;
    }

    private void renderThemes(float top, int accent) {
        this.layoutThemes(top);
        Theme theme = (Theme) Myau.moduleManager.modules.get(Theme.class);
        int current = theme == null ? -1 : theme.theme.getValue();
        Themes[] values = Themes.values();
        Themes.ThemeGroup lastGroup = null;
        for (ThemeCell cell : this.themeCells) {
            Themes.ThemeGroup group = values[cell.index].getGroup();
            if (group != lastGroup) {
                lastGroup = group;
                GuiFonts.drawStringWithShadow(group.getLabel(), cell.x, cell.y - 15.0F,
                        new Color(140, 140, 150).getRGB());
            }
            GuiRender.drawHorizontalGradientRect(cell.x, cell.y, cell.x + cell.width, cell.y + 12.0F,
                    values[cell.index].getPrimary().getRGB(),
                    values[cell.index].getSecondary().getRGB());
            boolean selected = cell.index == current;
            GuiFonts.drawStringWithShadow((selected ? "> " : "") + values[cell.index].getThemeName(),
                    cell.x, cell.y + 14.0F, selected ? accent : new Color(200, 200, 200).getRGB());
            if (selected) {
                GuiRender.drawOutline(cell.x - 1.0F, cell.y - 1.0F,
                        cell.x + cell.width + 1.0F, cell.y + 12.0F + 1.0F, 1.0F,
                        new Color(255, 255, 255).getRGB());
            }
        }
    }

    private boolean overTitle(int mouseX, int mouseY) {
        return mouseX >= this.windowX && mouseX <= this.windowX + WINDOW_WIDTH
                && mouseY >= this.windowY && mouseY <= this.windowY + TITLE_HEIGHT;
    }

    private Integer sidebarHit(int mouseX, int mouseY) {
        float y = this.windowY + TITLE_HEIGHT + 6.0F;
        if (this.hitEntry(mouseX, mouseY, y)) {
            return SEARCH;
        }
        y += 17.0F;
        for (int i = 0; i < Category.values().length; i++) {
            if (this.hitEntry(mouseX, mouseY, y)) {
                return i;
            }
            y += 17.0F;
        }
        if (this.hitEntry(mouseX, mouseY, y)) {
            return CONFIGS;
        }
        y += 17.0F;
        if (this.hitEntry(mouseX, mouseY, y)) {
            return THEMES;
        }
        return null;
    }

    private boolean hitEntry(int mouseX, int mouseY, float y) {
        return mouseX >= this.windowX + 4.0F && mouseX <= this.windowX + SIDEBAR_WIDTH - 4.0F
                && mouseY >= y && mouseY <= y + 14.0F;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        float zoom = guiZoom();
        mouseX = zoomedMouse(mouseX, zoom);
        mouseY = zoomedMouse(mouseY, zoom);
        if (this.selection == SEARCH) {
            this.searchFocused = this.overSearchBox(mouseX, mouseY);
            if (this.searchFocused) {
                return;
            }
        }
        if (this.overTitle(mouseX, mouseY)) {
            if (button == 0) {
                this.dragging = true;
                this.grabX = mouseX - this.windowX;
                this.grabY = mouseY - this.windowY;
            }
            return;
        }
        Integer hit = this.sidebarHit(mouseX, mouseY);
        if (hit != null) {
            if (hit != this.selection) {
                this.selection = hit;
                this.scroll = 0.0F;
                if (hit == CONFIGS) {
                    this.reloadConfigs();
                }
            }
            return;
        }
        float top = this.contentTop();
        if (this.selection == CONFIGS) {
            float y = top - this.scroll;
            for (String name : this.configNames) {
                if (mouseX >= this.contentLeft() && mouseX <= this.contentLeft() + this.contentWidth()
                        && mouseY >= y && mouseY <= y + 14.0F && button == 0) {
                    new Config(name, false).load();
                    return;
                }
                y += 17.0F;
            }
            return;
        }
        if (this.selection == THEMES) {
            Theme theme = (Theme) Myau.moduleManager.modules.get(Theme.class);
            if (theme != null && button == 0) {
                for (ThemeCell cell : this.themeCells) {
                    if (mouseX >= cell.x && mouseX <= cell.x + cell.width
                            && mouseY >= cell.y && mouseY <= cell.y + cell.height) {
                        theme.theme.setValue(cell.index);
                        return;
                    }
                }
            }
            return;
        }
        String query = this.selection == SEARCH ? this.searchText.trim().toLowerCase() : null;
        float rowY = top - this.scroll;
        for (RiseModuleRow row : this.visibleRows(query)) {
            row.layout(this.contentLeft(), rowY, this.contentWidth());
            if (row.onClick(mouseX, mouseY, button)) {
                return;
            }
            rowY += row.getHeight() + 6.0F;
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int button) {
        this.dragging = false;
        for (RiseModuleRow row : this.rows) {
            row.onRelease();
        }
    }

    @Override
    protected void keyTyped(char typed, int key) {
        for (RiseModuleRow row : this.rows) {
            row.onKey(typed, key);
            if (row.isBinding()) {
                return;
            }
        }
        if (this.selection == SEARCH && this.searchFocused) {
            if (key == Keyboard.KEY_BACK && this.searchText.length() > 0) {
                this.searchText = this.searchText.substring(0, this.searchText.length() - 1);
            } else if (key == Keyboard.KEY_ESCAPE) {
                this.searchFocused = false;
            } else if (typed >= 32 && typed < 127 && this.searchText.length() < 32) {
                this.searchText += typed;
            }
            return;
        }
        if (key == Keyboard.KEY_ESCAPE) {
            this.mc.displayGuiScreen(null);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (this.dragging) {
            float zoom = guiZoom();
            int mouseX = org.lwjgl.input.Mouse.getX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - org.lwjgl.input.Mouse.getY() * this.height / this.mc.displayHeight - 1;
            this.windowX = zoomedMouse(mouseX, zoom) - this.grabX;
            this.windowY = zoomedMouse(mouseY, zoom) - this.grabY;
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
