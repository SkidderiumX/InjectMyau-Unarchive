package myau.module.modules;

import myau.event.EventTarget;
import myau.events.Render3DEvent;
import myau.module.Module;
import myau.property.properties.FloatProperty;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;

public class MotionBlur extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private int texId = -1;
    private int texW;
    private int texH;
    private boolean hasFrame = false;

    public final FloatProperty strength = new FloatProperty("strength", 0.6F, 0.0F, 0.9F, 0.05F);

    public MotionBlur() {
        super("MotionBlur", false);
    }

    @Override
    public void onDisabled() {
        this.hasFrame = false;
        if (this.texId != -1) {
            GL11.glDeleteTextures(this.texId);
            this.texId = -1;
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!this.isEnabled()) {
            this.hasFrame = false;
            return;
        }

        int w = mc.displayWidth, h = mc.displayHeight;
        if (w <= 0 || h <= 0) return;

        this.ensureTexture(w, h);

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, 1, 0, 1, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        if (this.hasFrame) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_FOG);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glDepthMask(false);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.texId);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, this.strength.getValue());

            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(0, 0); GL11.glVertex2f(0, 0);
            GL11.glTexCoord2f(1, 0); GL11.glVertex2f(1, 0);
            GL11.glTexCoord2f(1, 1); GL11.glVertex2f(1, 1);
            GL11.glTexCoord2f(0, 1); GL11.glVertex2f(0, 1);
            GL11.glEnd();
        }

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.texId);
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);
        this.hasFrame = true;

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    private void ensureTexture(int w, int h) {
        if (this.texId != -1 && w == this.texW && h == this.texH) return;
        if (this.texId != -1) GL11.glDeleteTextures(this.texId);

        this.texId = GL11.glGenTextures();
        this.texW = w;
        this.texH = h;
        this.hasFrame = false;

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.texId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, w, h, 0,
                GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
    }
}
