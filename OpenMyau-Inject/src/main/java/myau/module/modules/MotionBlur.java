package myau.module.modules;

import myau.event.EventTarget;
import myau.events.Render3DEvent;
import myau.events.ResizeEvent;
import myau.module.Module;
import myau.property.properties.FloatProperty;
import myau.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;

/**
 * Blends the previous rendered world frame over the current one right after
 * the world has drawn (before the hand and HUD), leaving a trailing smear
 * behind anything that moves. Hand, HUD, chat and GUIs stay perfectly crisp
 * because they are drawn after the blend. "blur-amount" controls how strong
 * the trail is (0 = off, 10 = nearly frozen world).
 */
public class MotionBlur extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    /**
     * The blend never reaches 1.0 so the world keeps updating even at max blur.
     */
    private static final float MAX_PREV_WEIGHT = 0.9F;

    private Framebuffer previous = null;
    private boolean stale = true;

    public final FloatProperty blurAmount = new FloatProperty("blur-amount", 7.0F, 0.0F, 10.0F, 0.1F);

    public MotionBlur() {
        super("Motion Blur", false);
    }

    @EventTarget
    public void onResize(ResizeEvent event) {
        this.destroyBuffer();
        this.stale = true;
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (mc.theWorld == null || !OpenGlHelper.isFramebufferEnabled()) {
            return;
        }
        if (this.ensureBuffer()) {
            return;
        }
        Framebuffer main = mc.getFramebuffer();
        if (!this.isEnabled()) {
            // While off, only refresh the copy once so re-enabling never ghosts
            // an old frame.
            if (this.stale) {
                this.copyMainToPrevious(main);
                this.stale = false;
            }
            return;
        }
        if (this.stale) {
            this.copyMainToPrevious(main);
            this.stale = false;
            return;
        }
        float weight = this.blurAmount.getValue().floatValue() / 10.0F * MAX_PREV_WEIGHT;
        if (weight <= 0.0F) {
            return;
        }
        GlStateManager.pushMatrix();
        GlStateManager.pushAttrib();
        try {
            mc.entityRenderer.disableLightmap();
            mc.entityRenderer.setupOverlayRendering();
            main.bindFramebuffer(true);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
            GlStateManager.disableDepth();
            GlStateManager.enableTexture2D();
            GlStateManager.color(1.0F, 1.0F, 1.0F, weight);
            RenderUtil.drawFramebuffer(this.previous);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.enableDepth();
            GlStateManager.disableBlend();
            GlStateManager.resetColor();
            this.copyMainToPrevious(main);
        } finally {
            main.bindFramebuffer(false);
            mc.entityRenderer.setupOverlayRendering();
            GlStateManager.popAttrib();
            GlStateManager.popMatrix();
        }
    }

    @Override
    public void onDisabled() {
        this.stale = true;
    }

    private boolean ensureBuffer() {
        if (this.previous == null
                || this.previous.framebufferWidth != mc.displayWidth
                || this.previous.framebufferHeight != mc.displayHeight) {
            this.destroyBuffer();
            if (mc.displayWidth <= 0 || mc.displayHeight <= 0) {
                return true;
            }
            this.previous = new Framebuffer(mc.displayWidth, mc.displayHeight, false);
            this.stale = true;
        }
        return false;
    }

    private void copyMainToPrevious(Framebuffer main) {
        GlStateManager.bindTexture(this.previous.framebufferTexture);
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0,
                main.framebufferWidth, main.framebufferHeight);
    }

    private void destroyBuffer() {
        if (this.previous != null) {
            this.previous.deleteFramebuffer();
            this.previous = null;
        }
    }
}
