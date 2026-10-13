package myau.util;

import myau.access.AccessorItemRenderer;
import myau.config.AnimationConfig;
import myau.config.AnimationMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

/**
 * First-person item/block animations shared by the Mixin path (mod version)
 * and the inject path (Badlion/Lunar).
 * Original logic by syuto/animations-1.6, integrated into Uzi.
 *
 * All private ItemRenderer members go through {@link AccessorItemRenderer}
 * so this works under every launcher mapping.
 */
public final class ItemAnimations {
    private ItemAnimations() {
    }

    public interface FirstPersonState {
        float equippedProgress();
        float prevEquippedProgress();
        ItemStack itemToRender();
        void transformFirstPersonItem(float equipProgress, float swingProgress);
        void doBlockTransformations();
    }

    public static FirstPersonState wrap(final Object renderer) {
        return new FirstPersonState() {
            @Override
            public float equippedProgress() {
                return AccessorItemRenderer.getEquippedProgress(renderer);
            }
            @Override
            public float prevEquippedProgress() {
                return AccessorItemRenderer.getPrevEquippedProgress(renderer);
            }
            @Override
            public ItemStack itemToRender() {
                return AccessorItemRenderer.getItemToRender(renderer);
            }
            @Override
            public void transformFirstPersonItem(float equipProgress, float swingProgress) {
                AccessorItemRenderer.callTransformFirstPersonItem(renderer, equipProgress, swingProgress);
            }
            @Override
            public void doBlockTransformations() {
                AccessorItemRenderer.callDoBlockTransformations(renderer);
            }
        };
    }

    private static float spin = 0.0F;
    private static float delay = 0.0F;
    private static long lastUpdateTime = System.currentTimeMillis();

    public static void applyFirstPersonTransforms(FirstPersonState r, float partialTicks) {
        AnimationConfig.sync();
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) {
            return;
        }
        float equippedProgress = r.equippedProgress();
        float prevEquippedProgress = r.prevEquippedProgress();
        float f = 1.0F - (prevEquippedProgress + (equippedProgress - prevEquippedProgress) * partialTicks);

        AbstractClientPlayer player = mc.thePlayer;
        float swingProgress = player.getSwingProgress(partialTicks);
        float sine = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        float sqrtSwing = MathHelper.sqrt_float(swingProgress);
        float sine1 = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);

        GL11.glTranslated(
                AnimationConfig.getBlockPosX(),
                AnimationConfig.getBlockPosY(),
                AnimationConfig.getBlockPosZ()
        );

        // NOTE: using if-else instead of switch to avoid generating a synthetic
        // class that the mixin system incorrectly tries to load as a mixin.
        AnimationMode m = AnimationConfig.getMode();
        if (m == AnimationMode.EXHIBITION) {
            GL11.glTranslated(0.0D, -0.1D, 0.0D);
            r.transformFirstPersonItem(f / 2.0F, 0.0F);
            GL11.glTranslatef(0.1F, 0.4F, -0.1F);
            GL11.glRotated(-sine * 30.0F, sine / 2.0F, 0.0D, 9.0D);
            GL11.glRotated(-sine * 50.0F, 0.8D, sine / 2.0F, 0.0D);
        } else if (m == AnimationMode.SIGMA) {
            r.transformFirstPersonItem(f * 0.5F, 0.0F);
            GL11.glRotated(-sine * 27.5F, -8.0D, 0.0D, 9.0D);
            GL11.glRotated(-sine * 45.0F, 1.0D, sine / 2.0F, 0.0D);
            GL11.glTranslated(-0.1D, 0.3D, 0.1D);
        } else if (m == AnimationMode.VANILLA) {
            GL11.glTranslated(0.0D, 0.05D, -0.1D);
            r.transformFirstPersonItem(f, swingProgress);
        } else if (m == AnimationMode.PLAIN) {
            GL11.glTranslated(0.0D, 0.05D, 0.0D);
            r.transformFirstPersonItem(f, 0.0F);
        } else if (m == AnimationMode.SPIN) {
            GL11.glRotated(spin, 0.0D, 0.0D, -0.1D);
            r.transformFirstPersonItem(f, 0.0F);
            spin = -(System.currentTimeMillis() / 2L % 360L);
        } else if (m == AnimationMode.ETB) {
            GL11.glTranslated(0.0D, -0.1D, 0.0D);
            r.transformFirstPersonItem(f, 0.0F);
            GL11.glTranslatef(0.1F, 0.4F, -0.1F);
            GL11.glRotated(-sine * 35.0F, -8.0D, 0.0D, 9.0D);
            GL11.glRotated(-sine * 70.0F, 1.5D, -0.4D, 0.0D);
        } else if (m == AnimationMode.DORTWARE) {
            float alt = MathHelper.sin(sqrtSwing * 3.1415927F - 3.0F);
            r.transformFirstPersonItem(f, 0.0F);
            GL11.glRotated(-sine * 10.0F, 0.0D, 15.0D, 200.0D);
            GL11.glRotated(-sine * 10.0F, 300.0D, sine / 2.0F, 1.0D);
            GL11.glTranslated(3.4D, 0.3D, -0.4D);
            GL11.glTranslatef(-2.1F, -0.2F, 0.1F);
            GL11.glRotated(alt * 13.0F, -10.0D, -1.4D, -10.0D);
        } else if (m == AnimationMode.AVATAR) {
            GL11.glTranslatef(0.56F, -0.52F, -0.72F);
            GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(sine1 * -20.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(sine * -20.0F, 0.0F, 0.0F, 1.0F);
            GL11.glRotatef(sine * -40.0F, 1.0F, 0.0F, 0.0F);
            GL11.glScalef(0.4F, 0.4F, 0.4F);
        } else if (m == AnimationMode.SWONG) {
            r.transformFirstPersonItem(f / 2.0F, 0.0F);
            GL11.glRotated(-sine * 20.0F, sine / 2.0F, 0.0D, 9.0D);
            GL11.glRotated(-sine * 30.0F, 1.0D, sine / 2.0F, 0.0D);
        } else if (m == AnimationMode.SWANG) {
            r.transformFirstPersonItem(f / 2.0F, swingProgress);
            GL11.glRotated(sine * 15.0F, -sine, 0.0D, 9.0D);
            GL11.glRotated(sine * 40.0F, 1.0D, -sine / 2.0F, 0.0D);
        } else if (m == AnimationMode.SWANK) {
            r.transformFirstPersonItem(f / 2.0F, swingProgress);
            GL11.glRotated(sine * 30.0F, -sine, 0.0D, 9.0D);
            GL11.glRotated(sine * 40.0F, 1.0D, -sine, 0.0D);
        } else if (m == AnimationMode.STYLES) {
            r.transformFirstPersonItem(f, 0.0F);
            GL11.glTranslatef(-0.05F, 0.2F, 0.0F);
            GL11.glRotated(-sine * 35.0F, -8.0D, 0.0D, 9.0D);
            GL11.glRotated(-sine * 70.0F, 1.0D, -0.4D, 0.0D);
        } else if (m == AnimationMode.NUDGE) {
            GL11.glTranslated(-0.1D, 0.09D, 0.0D);
            GL11.glRotated(0.0D, -320.0D, 320.0D, 0.0D);
            r.transformFirstPersonItem(0.0F, 1.0F);
            float ns1 = MathHelper.sin(sqrtSwing * 3.0F);
            float ns2 = MathHelper.sin(sqrtSwing * 4.9415927F);
            GL11.glRotated(-ns1 * 60.0F, -90.0D, -ns2, 10.0D);
            GL11.glRotated(-ns1 * 110.0F, 15.0D, ns2, 0.0D);
        } else if (m == AnimationMode.PUNCH) {
            r.transformFirstPersonItem(f, 0.0F);
            GL11.glTranslatef(0.1F, 0.2F, 0.3F);
            GL11.glRotated(-sine * 30.0F, -5.0D, 0.0D, 9.0D);
            GL11.glRotated(-sine * 10.0F, 1.0D, -0.4D, -0.5D);
        } else if (m == AnimationMode.SLIDE) {
            GL11.glTranslated(-0.1D, 0.15D, 0.0D);
            r.transformFirstPersonItem(0.0F, 0.0F);
            float ss = MathHelper.sin(sqrtSwing * 2.9415927F);
            GL11.glTranslatef(-0.05F, 0.0F, 0.35F);
            GL11.glRotated(-ss * 30.0F, -15.0D, ss, 10.0D);
            GL11.glRotated(-ss * 70.0D, 5.0D, -ss, 0.0D);
        } else if (m == AnimationMode.JIGSAW) {
            GL11.glTranslatef(0.56F, -0.42F, -0.72F);
            GL11.glTranslatef(0.1F * sine, 0.0F, -0.22F * sine);
            GL11.glTranslatef(0.0F, sine1 * -0.15F, 0.0F);
            GL11.glRotated(sine1 * 45.0F, 0.0D, 1.0D, 0.0D);
            GL11.glRotated(sine1 * -20.0F, 0.0D, 1.0D, 0.0D);
            GL11.glRotated(sine * -20.0F, 0.0D, 0.0D, 1.0D);
            GL11.glRotated(sine * -80.0F, 1.0D, 0.0D, 0.0D);
        } else if (m == AnimationMode.SWING) {
            r.transformFirstPersonItem(f, 0.0F);
            r.doBlockTransformations();
        } else if (m == AnimationMode.OLD) {
            r.transformFirstPersonItem(f, 0.0F);
            applyOldAnimation(r);
        } else if (m == AnimationMode.PUSH) {
            r.transformFirstPersonItem(f, 0.0F);
            applyPushAnimation(swingProgress);
        } else if (m == AnimationMode.DASH) {
            r.transformFirstPersonItem(f, 0.0F);
            applyDashAnimation(swingProgress);
        } else if (m == AnimationMode.SLASH) {
            r.transformFirstPersonItem(f, 0.0F);
            applySlashAnimation(swingProgress);
        } else if (m == AnimationMode.SCALE) {
            r.transformFirstPersonItem(f, 0.0F);
            applyScaleAnimation(swingProgress);
        } else if (m == AnimationMode.SWONK) {
            r.transformFirstPersonItem(f, 0.0F);
            applySwonkAnimation(swingProgress);
        } else if (m == AnimationMode.STELLA) {
            r.transformFirstPersonItem(f, 0.0F);
            applyStellaAnimation(r);
        } else if (m == AnimationMode.SMALL) {
            r.transformFirstPersonItem(f, 0.0F);
            applySmallAnimation(r);
        } else if (m == AnimationMode.EDIT) {
            r.transformFirstPersonItem(f, 0.0F);
            applyEditAnimation(swingProgress);
        } else if (m == AnimationMode.RHYS) {
            r.transformFirstPersonItem(f, 0.0F);
            applyRhysAnimation(swingProgress);
        } else if (m == AnimationMode.STAB) {
            r.transformFirstPersonItem(f, 0.0F);
            applyStabAnimation(swingProgress);
        } else if (m == AnimationMode.FLOAT) {
            r.transformFirstPersonItem(f, 0.0F);
            applyFloatAnimation(swingProgress);
        } else if (m == AnimationMode.REMIX) {
            r.transformFirstPersonItem(f, 0.0F);
            applyRemixAnimation(swingProgress);
        } else if (m == AnimationMode.XIV) {
            r.transformFirstPersonItem(f, 0.0F);
            applyXivAnimation(swingProgress);
        } else if (m == AnimationMode.WINTER) {
            r.transformFirstPersonItem(f, 0.0F);
            applyWinterAnimation(r);
        } else if (m == AnimationMode.YAMATO) {
            r.transformFirstPersonItem(f, 0.0F);
            applyYamatoAnimation(swingProgress);
        } else if (m == AnimationMode.SLIDE_SWING) {
            r.transformFirstPersonItem(f, 0.0F);
            applySlideSwingAnimation(r, swingProgress);
        } else if (m == AnimationMode.SMALL_PUSH) {
            r.transformFirstPersonItem(f, 0.0F);
            applySmallPushAnimation(r, swingProgress);
        } else if (m == AnimationMode.REVERSE) {
            r.transformFirstPersonItem(f, 0.0F);
            applyReverseAnimation(r);
        } else if (m == AnimationMode.INVENT) {
            r.transformFirstPersonItem(f, 0.0F);
            applyInventAnimation(swingProgress);
        } else if (m == AnimationMode.LEAKED) {
            r.transformFirstPersonItem(f, 0.0F);
            applyLeakedAnimation(swingProgress);
        } else if (m == AnimationMode.AQUA) {
            r.transformFirstPersonItem(f, 0.0F);
            applyAquaAnimation(swingProgress);
        } else if (m == AnimationMode.ASTRO) {
            r.transformFirstPersonItem(f, 0.0F);
            applyAstroAnimation(swingProgress);
        } else if (m == AnimationMode.FADEAWAY) {
            r.transformFirstPersonItem(f, 0.0F);
            applyFadeawayAnimation(swingProgress);
        } else if (m == AnimationMode.ASTOLFO) {
            r.transformFirstPersonItem(f, 0.0F);
            applyAstolfoAnimation(swingProgress);
        } else if (m == AnimationMode.ASTOLFO_SPIN) {
            r.transformFirstPersonItem(f, 0.0F);
            applyAstolfoSpinAnimation(r);
        } else if (m == AnimationMode.MOON) {
            r.transformFirstPersonItem(f, 0.0F);
            applyMoonAnimation(swingProgress);
        } else if (m == AnimationMode.MOON_PUSH) {
            r.transformFirstPersonItem(f, 0.0F);
            applyMoonPushAnimation(swingProgress);
        } else if (m == AnimationMode.SMOOTH) {
            r.transformFirstPersonItem(f, 0.0F);
            applySmoothAnimation(swingProgress);
        } else if (m == AnimationMode.TAP1) {
            r.transformFirstPersonItem(f, 0.0F);
            applyTap1Animation(swingProgress);
        } else if (m == AnimationMode.TAP2) {
            r.transformFirstPersonItem(f, 0.0F);
            applyTap2Animation(swingProgress);
        } else if (m == AnimationMode.SIGMA3) {
            r.transformFirstPersonItem(f, 0.0F);
            applySigma3Animation(swingProgress);
        } else if (m == AnimationMode.SIGMA4) {
            r.transformFirstPersonItem(f, 0.0F);
            applySigma4Animation(r, swingProgress);
        } else if (m == AnimationMode.MYAU_1_8) {
            r.transformFirstPersonItem(f, 0.0F);
        } else if (m == AnimationMode.MYAU_SLIDE) {
            r.transformFirstPersonItem(f, 0.0F);
            applyMyauSlideAnimation(swingProgress);
        } else if (m == AnimationMode.MYAU_SWANK) {
            r.transformFirstPersonItem(f, 0.0F);
            applyMyauSwankAnimation(swingProgress);
        } else if (m == AnimationMode.MYAU_SWANG) {
            r.transformFirstPersonItem(f, 0.0F);
            applyMyauSwangAnimation(swingProgress);
        } else if (m == AnimationMode.MYAU_AVATAR) {
            r.transformFirstPersonItem(f, 0.0F);
            applyMyauAvatarAnimation(r, swingProgress);
        } else if (m == AnimationMode.MYAU_JIGSAW) {
            r.transformFirstPersonItem(f, 0.0F);
            applyMyauJigsawAnimation(r);
        }
    }

    public static boolean shouldApplyNormalAnimations(FirstPersonState r) {
        Minecraft mc = Minecraft.getMinecraft();
        return AnimationConfig.getRenderMode() == 1
                && mc != null
                && mc.thePlayer != null
                && r.itemToRender() != null
                && r.itemToRender().getItem() instanceof ItemSword
                && mc.thePlayer.getItemInUseCount() <= 0
                && !mc.thePlayer.isBlocking();
    }

    public static void applyFirstPersonScale(FirstPersonState r, float partialTicks) {
        AnimationConfig.sync();
        if (shouldApplyNormalAnimations(r)) {
            applyFirstPersonTransforms(r, partialTicks);
        }
        double s = (double) AnimationConfig.getScale() / 100.0D * (1.0D + AnimationConfig.getItemSize());
        GL11.glScaled(s, s, s);
    }

    public static int swingAnimationEnd() {
        if (!AnimationConfig.isEnabled()) {
            return 6;
        }
        AnimationConfig.sync();
        int pct = Math.max(0, Math.min(AnimationConfig.getSwingSpeed(), 100));
        return (int) (6.0D + (double) pct / 100.0D * 14.0D);
    }

    private static void applyOldAnimation(FirstPersonState r) {
        GL11.glTranslated(0.08D, -0.14D, -0.05D);
        GlStateManager.translate(-0.35F, 0.2F, 0.0F);
        r.doBlockTransformations();
    }

    private static void applyPushAnimation(float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(-swing * 20.0F, swing / 2.0F, 1.0F, 4.0F);
        GlStateManager.rotate(-swing * 30.0F, 1.0F, swing / 3.0F, -0.0F);
    }

    private static void applyDashAnimation(float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GL11.glRotated(-swing * 22.0F, swing / 2.0F, 0.0F, 9.0D);
        GL11.glRotated(-swing * 50.0F, 0.8F, swing / 2.0F, 0.0D);
    }

    private static void applySlashAnimation(float swingProgress) {
        GL11.glTranslated(0.08D, 0.08D, 0.0D);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(-swing * 70.0F, 5.0F, 13.0F, 50.0F);
    }

    private static void applyScaleAnimation(float swingProgress) {
        GL11.glTranslated(0.84D, -0.77D, -1.1D);
        GlStateManager.translate(0.56F, -0.52F, -0.71999997F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float sine1 = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
        float sine = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(sine1 * -27.0F, 0.0F, 0.0F, 0.0F);
        GlStateManager.rotate(sine * -27.0F, 0.0F, 0.0F, 0.0F);
        GlStateManager.rotate(sine * -27.0F, 0.0F, 0.0F, 0.0F);
    }

    private static void applyMyauSlideAnimation(float swingProgress) {
        GL11.glTranslated(0.08D, -0.11D, -0.07D);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.translate(-0.4F, 0.28F, 0.0F);
        GlStateManager.rotate(-swing * 35.0F, -8.0F, -0.0F, 9.0F);
        GlStateManager.rotate(-swing * 70.0F, 1.0F, -0.4F, -0.0F);
    }

    private static void applyMyauSwankAnimation(float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GL11.glRotatef(swing * 15.0F, -swing, -0.0F, 9.0F);
        GL11.glRotatef(swing * 40.0F, 1.0F, -swing / 2.0F, -0.0F);
    }

    private static void applyMyauSwangAnimation(float swingProgress) {
        GL11.glTranslated(0.0D, 0.03D, 0.0D);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(-swing * 37.0F, swing / 2.0F, 1.0F, 4.0F);
        GlStateManager.rotate(-swing * 52.0F, 1.0F, swing / 3.0F, -0.0F);
    }

    private static void applyMyauAvatarAnimation(FirstPersonState r, float swingProgress) {
        GlStateManager.translate(0.56F, -0.52F, -0.72F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float sine1 = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
        float sine = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(sine1 * -20.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(sine * -20.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(sine * -40.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(0.4F, 0.4F, 0.4F);
        r.doBlockTransformations();
    }

    private static void applyMyauJigsawAnimation(FirstPersonState r) {
        GL11.glTranslated(0.0D, -0.18D, -0.1D);
        GlStateManager.translate(-0.5F, 0.0F, 0.0F);
        r.doBlockTransformations();
    }

    private static void applySwonkAnimation(float swingProgress) {
        GL11.glTranslated(0.0D, 0.03D, 0.0D);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(-swing * -15.0F, swing / 2.0F, 1.0F, 4.0F);
        GlStateManager.rotate(-swing * 7.5F, 1.0F, swing / 3.0F, -0.0F);
    }

    private static void applyStellaAnimation(FirstPersonState r) {
        GlStateManager.translate(-0.5F, 0.3F, -0.2F);
        GlStateManager.rotate(32.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-70.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(40.0F, 0.0F, 1.0F, 0.0F);
        r.doBlockTransformations();
    }

    private static void applySmallAnimation(FirstPersonState r) {
        GL11.glTranslated(-0.01D, 0.03D, -0.24D);
        r.doBlockTransformations();
    }

    private static void applyEditAnimation(float swingProgress) {
        GL11.glTranslated(-0.04D, 0.06D, 0.0D);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(swing * 8.0F, -swing, -0.0F, 2.0F);
        GlStateManager.rotate(swing * 22.0F, 1.0F, -swing / 3.0F, -0.0F);
    }

    private static void applyRhysAnimation(float swingProgress) {
        GL11.glTranslated(0.0D, 0.19D, 0.0D);
        GlStateManager.translate(0.41F, -0.25F, -0.5555557F);
        GlStateManager.rotate(35.0F, 0.0F, 1.5F, 0.0F);
        float slowSwing = MathHelper.sin(swingProgress * swingProgress / 64.0F * 3.1415927F);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(slowSwing * -5.0F, 0.0F, 0.0F, 0.0F);
        GlStateManager.rotate(swing * -12.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(swing * -65.0F, 1.0F, 0.0F, 0.0F);
    }

    private static void applyStabAnimation(float swingProgress) {
        GL11.glTranslated(-0.25D, 0.45D, 0.8D);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.translate(0.6F, 0.3F, -0.6F + -swing * 0.7F);
        GlStateManager.rotate(6090.0F, 0.0F, 0.0F, 0.1F);
        GlStateManager.rotate(6085.0F, 0.0F, 0.1F, 0.0F);
        GlStateManager.rotate(6110.0F, 0.1F, 0.0F, 0.0F);
    }

    private static void applyFloatAnimation(float swingProgress) {
        float swing = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
        GlStateManager.rotate(-swing * 20.0F, swing / 2.0F, -0.0F, 9.0F);
        GlStateManager.rotate(-swing * 30.0F, 1.0F, swing / 2.0F, -0.0F);
    }

    private static void applyRemixAnimation(float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(0.0F, -2.0F, 0.0F, 10.0F);
        GlStateManager.rotate(-swing * 25.0F, 0.5F, 0.0F, 1.0F);
    }

    private static void applyXivAnimation(float swingProgress) {
        float sine = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        float sine1 = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
        GlStateManager.rotate(-sine1 * 20.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-sine * 20.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(-sine * 80.0F, 1.0F, 0.0F, 0.0F);
    }

    private static void applyWinterAnimation(FirstPersonState r) {
        GL11.glTranslated(0.0D, -0.16D, 0.0D);
        GL11.glTranslatef(-0.35F, 0.1F, 0.0F);
        GL11.glTranslatef(-0.05F, -0.1F, 0.1F);
        r.doBlockTransformations();
    }

    private static void applyYamatoAnimation(float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GL11.glRotatef(-swing * 100.0F, -9.0F, 5.0F, 9.0F);
    }

    private static void applySlideSwingAnimation(FirstPersonState r, float swingProgress) {
        GlStateManager.translate(0.56F, -0.52F, -0.72F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(swing * -80.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(0.4F, 0.4F, 0.4F);
        r.doBlockTransformations();
    }

    private static void applySmallPushAnimation(FirstPersonState r, float swingProgress) {
        GlStateManager.translate(0.56F, -0.52F, -0.72F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float sine1 = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
        float sine = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(sine1 * -10.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.rotate(sine * -10.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.rotate(sine * -10.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.scale(0.4F, 0.4F, 0.4F);
        r.doBlockTransformations();
    }

    private static void applyReverseAnimation(FirstPersonState r) {
        GL11.glTranslated(0.0D, 0.1D, -0.12D);
        GL11.glTranslated(0.08D, -0.1D, -0.3D);
        r.doBlockTransformations();
    }

    private static void applyInventAnimation(float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(-swing * 30.0F, -8.0F, -0.2F, 9.0F);
    }

    private static void applyLeakedAnimation(float swingProgress) {
        GL11.glTranslated(0.08D, 0.02D, 0.0D);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(-swing * 41.0F, 1.1F, 0.8F, -0.3F);
    }

    private static void applyAquaAnimation(float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(-swing * 8.5F, swing / 2.0F, 1.0F, 4.0F);
        GlStateManager.rotate(-swing * 6.0F, 1.0F, swing / 3.0F, -0.0F);
    }

    private static void applyAstroAnimation(float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(swing * 50.0F / 9.0F, -swing, -0.0F, 90.0F);
        GlStateManager.rotate(swing * 50.0F, 200.0F, -swing / 2.0F, -0.0F);
    }

    private static void applyFadeawayAnimation(float swingProgress) {
        float sine1 = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
        GlStateManager.rotate(-sine1 * 45.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(0.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(0.0F, 1.5F, 0.0F, 0.0F);
    }

    private static void applyAstolfoAnimation(float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(-swing * 29.0F, swing / 2.0F, 1.0F, 0.5F);
        GlStateManager.rotate(-swing * 43.0F, 1.0F, swing / 3.0F, -0.0F);
    }

    private static void applyAstolfoSpinAnimation(FirstPersonState r) {
        GlStateManager.rotate(delay, 0.0F, 0.0F, -0.1F);
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastUpdateTime;
        delay += elapsedTime * 360.0F / 850.0F;
        lastUpdateTime = currentTime;
        if (delay > 360.0F) {
            delay = 0.0F;
        }
        r.doBlockTransformations();
    }

    private static void applyMoonAnimation(float swingProgress) {
        GL11.glTranslated(-0.08D, 0.12D, 0.0D);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(-swing * 32.5F, swing / 2.0F, 1.0F, 4.0F);
        GlStateManager.rotate(-swing * 60.0F, 1.0F, swing / 3.0F, -0.0F);
    }

    private static void applyMoonPushAnimation(float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.translate(-0.2F, 0.45F, 0.25F);
        GlStateManager.rotate(-swing * 20.0F, -5.0F, -5.0F, 9.0F);
    }

    private static void applySmoothAnimation(float swingProgress) {
        GL11.glTranslated(0.14D, -0.1D, -0.24D);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.translate(-0.36F, 0.25F, -0.06F);
        GlStateManager.rotate(-swing * 35.0F, -8.0F, -0.0F, 9.0F);
        GlStateManager.rotate(-swing * 70.0F, 1.0F, 0.4F, -0.0F);
    }

    private static void applyTap1Animation(float swingProgress) {
        GlStateManager.translate(0.56F, -0.52F, -0.71999997F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate((swingProgress * 0.8F - swingProgress * swingProgress * 0.8F) * -90.0F,
                0.0F, 1.0F, 0.0F);
        GlStateManager.scale(0.37F, 0.37F, 0.37F);
    }

    private static void applyTap2Animation(float swingProgress) {
        GL11.glTranslated(0.0D, -0.1D, 0.0D);
        GlStateManager.translate(0.56F, -0.42F, -0.71999997F);
        GlStateManager.rotate(30.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F) * -30.0F,
                0.0F, 1.0F, 0.0F);
        GlStateManager.scale(0.4F, 0.4F, 0.4F);
    }

    private static void applySigma3Animation(float swingProgress) {
        GL11.glTranslated(0.02D, 0.02D, 0.0D);
        GL11.glTranslated(0.4D, -0.06D, -0.46D);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(swing * 12.5F, -swing, -0.0F, 9.0F);
        GlStateManager.rotate(swing * 15.0F, 1.0F, -swing / 2.0F, -0.0F);
    }

    private static void applySigma4Animation(FirstPersonState r, float swingProgress) {
        GL11.glTranslated(-0.6D, 0.2D, 0.11D);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(-swing * 27.5F, -8.0F, -0.0F, 9.0F);
        GlStateManager.rotate(-swing * 45.0F, 1.0F, swing / 2.0F, 0.0F);
        r.doBlockTransformations();
        GL11.glTranslated(-0.08D, -1.25D, 1.25D);
    }
}
