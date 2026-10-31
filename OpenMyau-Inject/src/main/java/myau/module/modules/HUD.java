package myau.module.modules;

import myau.Myau;
import myau.enums.BlinkModules;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.Render2DEvent;
import myau.events.TickEvent;
import myau.access.AccessorGuiChat;
import myau.module.Module;
import myau.ui.clickgui.Categories;
import myau.ui.clickgui.Category;
import myau.util.ColorUtil;
import myau.util.RenderUtil;
import myau.util.font.Fonts;
import myau.util.font.RavenFontRenderer;
import myau.util.font.MinecraftFontAdapter;
import myau.property.properties.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class HUD extends Module {
    private static final java.util.regex.Pattern WATCHDOG_WORD =
            java.util.regex.Pattern.compile("watchdog", java.util.regex.Pattern.CASE_INSENSITIVE);
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final Map<Module, Entry> entries = new LinkedHashMap<>();
    private List<Entry> activeEntries = new ArrayList<>();
    private long lastAnimationMs = 0L;

    private static final float ANIMATION_MS = 100.0F;

    private static final double LERP_RATE = 0.015;

    private static final float MAX_FRAME_MS = 200.0F;

    private static final float BASE_PADDING = 2.0F;
    private static final float BASE_OUTLINE = 1.0F;
    private static final int OUTLINE_NONE = 0;
    private static final int OUTLINE_FULL = 1;
    private static final int OUTLINE_SIDE = 2;
    private static final int OUTLINE_SIDEBAR = 3;
    private static final int COLOR_THEME = 6;
    private static final int IFACE_MODERN = 0;
    private static final int IFACE_WURST = 1;
    private static final int IFACE_CLASSIC = 2;
    private static final int IFACE_CREIDA = 3;
    public final ModeProperty colorMode = new ModeProperty(
            "color", 3, new String[]{"RAINBOW", "CHROMA", "ASTOLFO", "CUSTOM1", "CUSTOM12", "CUSTOM123", "THEME"}
    );
    public final FloatProperty colorSpeed = new FloatProperty("color-speed", 1.0F, 0.5F, 1.5F,
            () -> this.colorMode.getValue() != COLOR_THEME);
    public final PercentProperty colorSaturation = new PercentProperty("color-saturation", 50,
            () -> this.colorMode.getValue() != COLOR_THEME);
    public final PercentProperty colorBrightness = new PercentProperty("color-brightness", 100,
            () -> this.colorMode.getValue() != COLOR_THEME);
    public final ColorProperty custom1 = new ColorProperty("custom-color-1", Color.WHITE.getRGB(), () -> this.colorMode.getValue() == 3 || this.colorMode.getValue() == 4 || this.colorMode.getValue() == 5);
    public final ColorProperty custom2 = new ColorProperty("custom-color-2", Color.WHITE.getRGB(), () -> this.colorMode.getValue() == 4 || this.colorMode.getValue() == 5);
    public final ColorProperty custom3 = new ColorProperty("custom-color-3", Color.WHITE.getRGB(), () -> this.colorMode.getValue() == 5);
    public final ModeProperty posX = new ModeProperty("position-x", 0, new String[]{"LEFT", "RIGHT"});
    public final ModeProperty posY = new ModeProperty("position-y", 0, new String[]{"TOP", "BOTTOM"});
    public final IntProperty offsetX = new IntProperty("offset-x", 2, 0, 255);
    public final IntProperty offsetY = new IntProperty("offset-y", 2, 0, 255);
    public final FloatProperty scale = new FloatProperty("scale", 1.0F, 0.5F, 1.5F);
    public final PercentProperty background = new PercentProperty("background", 43);
    public final ModeProperty arraylistOutline = new ModeProperty("arraylist-outline", OUTLINE_SIDE,
            new String[]{"NONE", "FULL", "SIDE", "SIDEBAR"});
    public final BooleanProperty shadow = new BooleanProperty("shadow", true);
    public final ModeProperty arraylistFont = new ModeProperty("arraylist-font", 0,
            new String[]{"VANILLA", "SF-BOLD", "SF-REGULAR", "SF-UI", "PRODUCT-SANS",
                    "MSDF-SF", "MSDF-SF-BOLD", "MSDF-PRODUCT-SANS",
                    "MSDF-GOOGLE-SANS", "MSDF-GOOGLE-SANS-BOLD", "MSDF-TAHOMA",
                    "MSDF-TAHOMA-BOLD", "MSDF-VERDANA"});
    public final BooleanProperty suffixes = new BooleanProperty("suffixes", true);
    public final BooleanProperty preferHypixel = new BooleanProperty("prefer-hypixel", false,
            () -> this.suffixes.getValue());
    public final BooleanProperty lowerCase = new BooleanProperty("lower-case", false);
    public final BooleanProperty removeSpaces = new BooleanProperty("remove-spaces", false);
    public final BooleanProperty hideCombat = new BooleanProperty("hide-combat", false);
    public final BooleanProperty hideMovement = new BooleanProperty("hide-movement", false);
    public final BooleanProperty hideRender = new BooleanProperty("hide-render", false);
    public final BooleanProperty hidePlayer = new BooleanProperty("hide-player", false);
    public final BooleanProperty hideWorld = new BooleanProperty("hide-world", false);
    public final BooleanProperty hideMisc = new BooleanProperty("hide-misc", false);
    public final BooleanProperty hideClient = new BooleanProperty("hide-client", false);
    public final BooleanProperty boundOnly = new BooleanProperty("bound-only", false);
    public final BooleanProperty chatOutline = new BooleanProperty("chat-outline", true);
    public final BooleanProperty blinkTimer = new BooleanProperty("blink-timer", true);
    public final BooleanProperty toggleSound = new BooleanProperty("toggle-sounds", true);
    public final BooleanProperty toggleAlerts = new BooleanProperty("toggle-alerts", false);
    public final ModeProperty interfaceStyle = new ModeProperty("interface", 0,
            new String[]{"Modern Rise", "Wurst", "Classic Rise", "Creida"});
    public final BooleanProperty showInfo = new BooleanProperty("show-info", true,
            () -> this.interfaceStyle.getValue() == IFACE_CLASSIC);
    private static boolean shows(Module module) {
        return module.isEnabled() && !module.isHidden();
    }
    /**
     * True when the module's whole click-GUI category is hidden from the
     * arraylist through the "hide-[category]" options.
     */
    private boolean isCategoryHidden(Category category) {
        if (category == null) {
            return false;
        }
        switch (category) {
            case COMBAT: return this.hideCombat.getValue();
            case MOVEMENT: return this.hideMovement.getValue();
            case RENDER: return this.hideRender.getValue();
            case PLAYER: return this.hidePlayer.getValue();
            case WORLD: return this.hideWorld.getValue();
            case MISC: return this.hideMisc.getValue();
            case CLIENT: return this.hideClient.getValue();
            default: return false;
        }
    }
    private boolean showsInList(Entry entry) {
        if (entry.category == null) {
            entry.category = Categories.categoryOf(entry.module);
        }
        return shows(entry.module) && !this.isCategoryHidden(entry.category)
                && (!this.boundOnly.getValue() || entry.module.getKey() != 0);
    }
    private String formatEntryText(String raw) {
        String text = this.lowerCase.getValue() ? raw.toLowerCase(Locale.ROOT) : raw;
        return this.removeSpaces.getValue() ? text.replace(" ", "") : text;
    }
    private float renderScale() {
        return this.scale.getValue();
    }
    private RavenFontRenderer getArraylistFont() {
        int selection = this.interfaceStyle.getValue() == IFACE_CLASSIC
                ? 0 : this.arraylistFont.getValue();
        return Fonts.arraylist(selection, this.scale.getValue());
    }
    private float hudPixels(float base) {
        return Math.max(1.0F, Math.round(base * this.scale.getValue()));
    }
    private float horizontalPadding() {
        return this.hudPixels(BASE_PADDING);
    }
    private float topPadding() {
        return this.hudPixels(BASE_PADDING);
    }
    private float outlineThickness() {
        return this.hudPixels(BASE_OUTLINE);
    }
    private float arraylistStringWidth(String string) {
        return this.getArraylistFont().getStringWidth(string);
    }
    private float arraylistLineHeight() {
        RavenFontRenderer font = this.getArraylistFont();
        return Math.max(1, font.getTextBottomOffset() - font.getTextTopOffset());
    }
    private float arraylistTextY(float rowY) {
        return rowY - this.getArraylistFont().getTextTopOffset();
    }
    private void arraylistDraw(String text, float x, float y, int color) {
        int opaque = (color & 0xFF000000) == 0 ? color | 0xFF000000 : color;
        RavenFontRenderer font = this.getArraylistFont();
        if (this.shadow.getValue()) {
            float offset = font instanceof MinecraftFontAdapter ? 1.0F : 0.5F;
            int shadowColor = opaque & 0xFF000000;
            font.drawString(stripColorCodes(text), x + offset, y + offset, shadowColor, false);
        }
        font.drawString(text, x, y, opaque, false);
    }

    private static String stripColorCodes(String text) {
        StringBuilder builder = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (character == '§' && i + 1 < text.length()) {
                i++;
                continue;
            }
            builder.append(character);
        }
        return builder.toString();
    }
    private float getColorCycle(long long3, long long4) {
        long speed = (long) (3000.0 / Math.pow(Math.min(Math.max(0.5F, this.colorSpeed.getValue()), 1.5F), 3.0));
        return 1.0F - (float) (Math.abs(long3 - long4 * 300L) % speed) / (float) speed;
    }
    public HUD() {
        super("HUD", true, true);
    }
    public Color getColor(long time) {
        return this.getColor(time, 0L);
    }
    public Color getStaticColor() {
        int mode = this.colorMode.getValue();
        if (mode == COLOR_THEME) {
            Theme theme = (Theme) Myau.moduleManager.modules.get(Theme.class);
            return theme == null ? Color.WHITE : theme.getTheme().getPrimary();
        }
        if (mode == 3 || mode == 4 || mode == 5) {
            return new Color(this.custom1.getValue());
        }
        return this.getColor(0L);
    }
    public Color getColor(long time, long offset) {
        if (this.colorMode.getValue() == COLOR_THEME) {
            Theme theme = (Theme) Myau.moduleManager.modules.get(Theme.class);
            if (theme != null) {
                return theme.getColor(0.0, offset * 11.0);
            }
        }
        Color color = Color.white;
        switch (this.colorMode.getValue()) {
            case 0:
                color = ColorUtil.fromHSB(this.getColorCycle(time, offset), 1.0F, 1.0F);
                break;
            case 1:
                color = ColorUtil.fromHSB(this.getColorCycle(time / 3L, 0L), 1.0F, 1.0F);
                break;
            case 2:
                float cycle = this.getColorCycle(time, offset);
                if (cycle % 1.0F < 0.5F) {
                    cycle = 1.0F - cycle % 1.0F;
                }
                color = ColorUtil.fromHSB(cycle, 1.0F, 1.0F);
                break;
            case 3:
                color = new Color(this.custom1.getValue());
                break;
            case 4:
                double cycle1 = this.getColorCycle(time, offset);
                color = ColorUtil.interpolate(
                        (float) (2.0 * Math.abs(cycle1 - Math.floor(cycle1 + 0.5))),
                        new Color(this.custom1.getValue()),
                        new Color(this.custom2.getValue())
                );
                break;
            case 5:
                double cycle2 = this.getColorCycle(time, offset);
                float floor = (float) (2.0 * Math.abs(cycle2 - Math.floor(cycle2 + 0.5)));
                if (floor <= 0.5F) {
                    color = ColorUtil.interpolate(floor * 2.0F, new Color(this.custom1.getValue()), new Color(this.custom2.getValue()));
                } else {
                    color = ColorUtil.interpolate((floor - 0.5F) * 2.0F, new Color(this.custom2.getValue()), new Color(this.custom3.getValue()));
                }
        }
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        return Color.getHSBColor(
                hsb[0],
                hsb[1] * (this.colorSaturation.getValue().floatValue() / 100.0F),
                hsb[2] * (this.colorBrightness.getValue().floatValue() / 100.0F)
        );
    }
    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.POST) {
            return;
        }
        for (Module module : Myau.moduleManager.modules.values()) {
            if (!module.isHidden()) {
                this.entries.computeIfAbsent(module, Entry::new);
            }
        }
        for (Entry entry : this.entries.values()) {
            if (!this.showsInList(entry) && entry.animationTime == 0.0F) {
                continue;
            }
            String name = this.formatEntryText(entry.module.getName());
            String[] suffix = this.applySuffixPreference(entry.module.getSuffix());
            boolean hasTag = this.suffixes.getValue() && suffix.length > 0;

            entry.displayText = hasTag
                    ? name + " \u00a77" + this.formatEntryText(String.join(" ", suffix))
                    : name;
            entry.width = this.arraylistStringWidth(entry.displayText);
        }
        this.activeEntries = this.entries
                .values()
                .stream()
                .filter(entry -> this.showsInList(entry) || entry.animationTime > 0.0F)
                .sorted(Comparator.comparingDouble(entry -> -entry.width))
                .collect(Collectors.toList());
    }

    private String[] applySuffixPreference(String[] suffix) {
        if (!this.preferHypixel.getValue() || suffix.length == 0) {
            return suffix;
        }
        String[] result = new String[suffix.length];
        for (int i = 0; i < suffix.length; i++) {
            result[i] = WATCHDOG_WORD.matcher(suffix[i]).replaceAll("Hypixel");
        }
        return result;
    }

    private float outlineSpace() {
        return this.arraylistOutline.getValue() == OUTLINE_NONE ? 0.0F : this.outlineThickness();
    }

    private double entryX(float width, boolean leaving, float screenWidth, float edgeX) {

        if (this.posX.getValue() == 0) {
            float inset = edgeX + this.outlineSpace() + this.horizontalPadding();
            return leaving ? inset - width * 2.0F : inset;
        }
        float inset = edgeX + this.outlineSpace() + this.horizontalPadding();
        return leaving ? screenWidth - inset + width : screenWidth - inset - width;
    }
    private void drawArrayListOutline(float boxLeft, float boxRight, float boxTop, float boxBottom,
                                      int color, boolean firstRow, float prevLeft, float prevRight) {
        int mode = this.arraylistOutline.getValue();
        if (mode == OUTLINE_NONE) {
            return;
        }
        float thickness = this.outlineThickness();
        float outerLeft = boxLeft - thickness;
        float outerRight = boxRight + thickness;
        if (mode == OUTLINE_SIDEBAR) {
            RenderUtil.drawRect(outerLeft, boxTop, boxLeft, boxBottom, color);
            if (!firstRow) {
                float linkTop = this.posY.getValue() == 0 ? boxTop - thickness : boxBottom;
                float linkBottom = linkTop + thickness;
                float segLeft = Math.min(prevLeft, outerLeft);
                float segRight = Math.max(prevLeft, outerLeft) + thickness;
                RenderUtil.drawRect(segLeft, linkTop, segRight, linkBottom, color);
            }
            return;
        }
        boolean anchoredLeft = this.posX.getValue() == 0;
        if (anchoredLeft) {
            RenderUtil.drawRect(outerLeft, boxTop, boxLeft, boxBottom, color);
        } else {
            RenderUtil.drawRect(boxRight, boxTop, outerRight, boxBottom, color);
        }
        if (mode != OUTLINE_FULL) {
            return;
        }
        if (anchoredLeft) {
            RenderUtil.drawRect(boxRight, boxTop, outerRight, boxBottom, color);
        } else {
            RenderUtil.drawRect(outerLeft, boxTop, boxLeft, boxBottom, color);
        }
        float bandTop = this.posY.getValue() == 0 ? boxTop - thickness : boxBottom;
        float bandBottom = bandTop + thickness;
        if (firstRow) {
            RenderUtil.drawRect(outerLeft, bandTop, outerRight, bandBottom, color);
            return;
        }
        if (prevLeft < outerLeft) {
            RenderUtil.drawRect(prevLeft, bandTop, outerLeft + thickness, bandBottom, color);
        }
        if (prevRight > outerRight) {
            RenderUtil.drawRect(outerRight - thickness, bandTop, prevRight, bandBottom, color);
        }
    }
    private long renderArrayList(long now) {
        float topPadding = this.topPadding();
        float spacing = this.arraylistLineHeight() + topPadding;
        ScaledResolution resolution = new ScaledResolution(mc);
        float screenWidth = resolution.getScaledWidth();
        float screenHeight = resolution.getScaledHeight();
        float edgeX = this.offsetX.getValue();
        float edgeY = this.offsetY.getValue();
        float elapsed = this.lastAnimationMs == 0L ? 0.0F : Math.min(now - this.lastAnimationMs, MAX_FRAME_MS);
        this.lastAnimationMs = now;
        for (Entry entry : this.entries.values()) {
            if (this.showsInList(entry)) {
                entry.animationTime = Math.min(entry.animationTime + elapsed / ANIMATION_MS, 10.0F);
            } else {
                entry.animationTime = Math.max(entry.animationTime - elapsed / ANIMATION_MS, 0.0F);
            }
        }
        float row = 0.0F;
        for (Entry entry : this.activeEntries) {
            if (entry.animationTime == 0.0F) {
                continue;
            }
            float width = entry.width;
            boolean leaving = !this.showsInList(entry) && entry.animationTime < 10.0F;
            entry.targetX = this.entryX(width, leaving, screenWidth, edgeX);
            entry.targetY = this.posY.getValue() == 0
                    ? edgeY + topPadding + row
                    : screenHeight - edgeY - spacing + topPadding - row;
            if (!leaving) {
                row += spacing;
            }
            if (!entry.positioned) {
                entry.positioned = true;
                entry.x = this.entryX(width, true, screenWidth, edgeX);
                entry.y = entry.targetY;
            }
            if (Math.abs(entry.x - entry.targetX) <= 0.5
                    && Math.abs(entry.y - entry.targetY) <= 0.5
                    && (entry.animationTime == 0.0F || entry.animationTime == 10.0F)) {
                entry.x = entry.targetX;
                entry.y = entry.targetY;
            } else {
                entry.x += LERP_RATE * elapsed * (entry.targetX - entry.x);
                entry.y += LERP_RATE * elapsed * (entry.targetY - entry.y);
            }
        }

        GlStateManager.disableDepth();
        long index = 0L;
        float horizontalPadding = this.horizontalPadding();
        float thickness = this.outlineThickness();
        boolean plain = this.interfaceStyle.getValue() == IFACE_WURST;
        boolean modern = this.interfaceStyle.getValue() == IFACE_MODERN;
        boolean creida = this.interfaceStyle.getValue() == IFACE_CREIDA;
        boolean firstRow = true;
        float prevLeft = 0.0F;
        float prevRight = 0.0F;
        float lastBoxTop = 0.0F;
        float lastBoxBottom = 0.0F;
        int lastColor = 0;
        float sidebarBottom = -Float.MAX_VALUE;
        float sidebarBottomLeft = 0.0F;
        float sidebarBottomRight = 0.0F;
        int sidebarBottomColor = 0;
        for (Entry entry : this.activeEntries) {
            if (entry.animationTime == 0.0F) {
                continue;
            }
            float x = (float) entry.x;
            float y = (float) entry.y;
            int color = plain ? Color.WHITE.getRGB() : this.getColor(now, index).getRGB();
            float boxLeft = x - horizontalPadding;
            float boxRight = x + entry.width + horizontalPadding;
            float boxTop = y - topPadding;
            float boxBottom = boxTop + spacing;
            RenderUtil.enableRenderState();
            if (!plain && this.background.getValue() > 0) {
                RenderUtil.drawRect(
                        boxLeft,
                        boxTop,
                        boxRight,
                        boxBottom,
                        new Color(0.0F, 0.0F, 0.0F, this.background.getValue().floatValue() / 100.0F).getRGB()
                );
            }
            if (!plain) {
                this.drawArrayListOutline(boxLeft, boxRight, boxTop, boxBottom, color, firstRow, prevLeft, prevRight);
            }
            if (modern) {
                RenderUtil.drawRect(boxRight, boxTop, boxRight + thickness, boxBottom, color);
            } else if (creida) {
                RenderUtil.drawRect(boxLeft - thickness, boxTop, boxLeft, boxBottom, color);
            }
            RenderUtil.disableRenderState();
            this.arraylistDraw(entry.displayText, x, this.arraylistTextY(y), color);
            firstRow = false;
            prevLeft = boxLeft - thickness;
            prevRight = boxRight + thickness;
            lastBoxTop = boxTop;
            lastBoxBottom = boxBottom;
            lastColor = color;
            if (boxBottom >= sidebarBottom) {
                sidebarBottom = boxBottom;
                sidebarBottomLeft = boxLeft - thickness;
                sidebarBottomRight = boxRight;
                sidebarBottomColor = color;
            }
            index++;
        }
        if (!firstRow && !plain && this.arraylistOutline.getValue() == OUTLINE_SIDEBAR) {
            RenderUtil.enableRenderState();
            RenderUtil.drawRect(sidebarBottomLeft, sidebarBottom, sidebarBottomRight,
                    sidebarBottom + thickness, sidebarBottomColor);
            RenderUtil.disableRenderState();
        }
        if (!firstRow && !plain && this.arraylistOutline.getValue() == OUTLINE_FULL) {
            float capTop = this.posY.getValue() == 0 ? lastBoxBottom : lastBoxTop - thickness;
            RenderUtil.enableRenderState();
            RenderUtil.drawRect(prevLeft, capTop, prevRight, capTop + thickness, lastColor);
            RenderUtil.disableRenderState();
        }
        return index;
    }
    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (this.chatOutline.getValue() && mc.currentScreen instanceof GuiChat) {
            String text = AccessorGuiChat.getInputField((GuiChat) mc.currentScreen).getText().trim();
            if (Myau.commandManager != null && Myau.commandManager.isTypingCommand(text)) {
                RenderUtil.enableRenderState();
                RenderUtil.drawOutlineRect(
                        2.0F,
                        (float) (mc.currentScreen.height - 14),
                        (float) (mc.currentScreen.width - 2),
                        (float) (mc.currentScreen.height - 2),
                        1.5F,
                        0,
                        this.getColor(System.currentTimeMillis()).getRGB()
                );
                RenderUtil.disableRenderState();
            }
        }
        if (this.isEnabled() && !mc.gameSettings.showDebugInfo) {
            long l = System.currentTimeMillis();

            long offset = this.renderArrayList(l);
            this.renderWatermark(l);
            if (this.blinkTimer.getValue()) {
                BlinkModules blinkingModule = Myau.blinkManager.getBlinkingModule();
                if (blinkingModule != BlinkModules.NONE && blinkingModule != BlinkModules.AUTO_BLOCK) {
                    long movementPacketSize = Myau.blinkManager.countMovement();
                    if (movementPacketSize > 0L) {
                        GlStateManager.pushMatrix();
                        GlStateManager.scale(this.renderScale(), this.renderScale(), 1.0F);
                        GlStateManager.enableBlend();
                        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                        mc.fontRendererObj
                                .drawString(
                                        String.valueOf(movementPacketSize),
                                        (float) new ScaledResolution(mc).getScaledWidth() / 2.0F / this.renderScale()
                                                - (float) mc.fontRendererObj.getStringWidth(String.valueOf(movementPacketSize)) / 2.0F,
                                        (float) new ScaledResolution(mc).getScaledHeight() / 5.0F * 3.0F / this.renderScale(),
                                        this.getColor(l, offset).getRGB() & 16777215 | -1090519040,
                                        this.shadow.getValue()
                                );
                        GlStateManager.disableBlend();
                        GlStateManager.popMatrix();
                    }
                }
            }
            GlStateManager.enableDepth();
        }
    }
    private void renderWatermark(long now) {
        int style = this.interfaceStyle.getValue();
        RavenFontRenderer font = this.getArraylistFont();
        if (style == IFACE_MODERN) {
            font.drawString("Myau " + Myau.version, 4.0F, 4.0F, this.getColor(now).getRGB(), true);
        } else if (style == IFACE_WURST) {
            String text = "Myau " + Myau.version;
            float width = font.getStringWidth(text) + 8.0F;
            RenderUtil.enableRenderState();
            RenderUtil.drawRect(2.0F, 4.0F, 2.0F + width, 19.0F, Color.WHITE.getRGB());
            font.drawString(text, 6.0F, 8.0F, Color.BLACK.getRGB(), false);
            RenderUtil.disableRenderState();
        } else if (style == IFACE_CLASSIC) {
            mc.fontRendererObj.drawStringWithShadow("Myau",
                    4, 4, Color.WHITE.getRGB());
            if (this.showInfo.getValue() && mc.thePlayer != null) {
                String info = String.format("X: %d Y: %d Z: %d  |  %d fps",
                        MathHelper.floor_double(mc.thePlayer.posX),
                        MathHelper.floor_double(mc.thePlayer.posY),
                        MathHelper.floor_double(mc.thePlayer.posZ),
                        Minecraft.getDebugFPS());
                mc.fontRendererObj.drawStringWithShadow(info, 4,
                        4 + mc.fontRendererObj.FONT_HEIGHT + 2, Color.LIGHT_GRAY.getRGB());
            }
        } else {
            String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
            String name = mc.thePlayer == null ? "player" : mc.thePlayer.getName();
            String text = "Myau | " + Myau.version + " | " + name + " | " + time;
            float width = font.getStringWidth(text) + 10.0F;
            RenderUtil.enableRenderState();
            RenderUtil.drawRect(2.0F, 3.0F, 2.0F + width, 18.0F,
                    new Color(0.0F, 0.0F, 0.0F, 0.6F).getRGB());
            font.drawString(text, 7.0F, 7.0F, this.getColor(now).getRGB(), true);
            RenderUtil.disableRenderState();
        }
    }

    private static final class Entry {
        final Module module;
        Category category;
        float animationTime;
        boolean positioned;
        double x;
        double y;
        double targetX;
        double targetY;
        float width;
        String displayText = "";
        Entry(Module module) {
            this.module = module;
        }
    }
}
