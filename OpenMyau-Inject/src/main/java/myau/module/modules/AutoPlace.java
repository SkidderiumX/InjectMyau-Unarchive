package myau.module.modules;

import myau.Myau;
import myau.access.AccessorItemRenderer;
import myau.access.AccessorMinecraft;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.UpdateEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import org.lwjgl.input.Mouse;

/**
 * Ported from Raven B4 AutoPlace.
 * The Forge DrawBlockHighlightEvent was replaced with per-tick polling of
 * mc.objectMouseOver so it works on every launcher, and the mouse-button
 * pulse around the manual place call was dropped (no equivalent here --
 * the direct onPlayerRightClick call still places).
 */
public class AutoPlace extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final IntProperty frameDelay = new IntProperty("Frame delay", 8, 0, 30);
    public final IntProperty minPlaceDelay = new IntProperty("Min place delay", 60, 25, 500);
    public final BooleanProperty disableLeft = new BooleanProperty("Disable left", false);
    public final BooleanProperty holdRight = new BooleanProperty("Hold right", true);
    public final BooleanProperty fastPlaceJump = new BooleanProperty("Fast place on jump", true);
    public final BooleanProperty pitchCheck = new BooleanProperty("Pitch check", false);

    private int shownFrameDelay = 8;
    private long lastPlaceMs = 0L;
    private int frameCounter = 0;
    private MovingObjectPosition lastTarget = null;
    private BlockPos lastPos = null;

    public AutoPlace() {
        super("AutoPlace", false);
    }

    @Override
    public void onDisabled() {
        if (this.holdRight.getValue()) {
            AccessorMinecraft.setRightClickDelayTimer(mc, 4);
        }
        this.resetVariables();
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE
                || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }
        if (this.shownFrameDelay != this.frameDelay.getValue()) {
            this.resetVariables();
        }
        this.shownFrameDelay = this.frameDelay.getValue();
        if (mc.currentScreen != null || mc.thePlayer.capabilities.isFlying) {
            return;
        }
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null || !(held.getItem() instanceof ItemBlock)) {
            return;
        }
        FastPlace fastPlace = (FastPlace) Myau.moduleManager.modules.get(FastPlace.class);
        if (this.fastPlaceJump.getValue() && this.holdRight.getValue()
                && !fastPlace.isEnabled() && Mouse.isButtonDown(1)) {
            if (mc.thePlayer.motionY > 0.0D) {
                AccessorMinecraft.setRightClickDelayTimer(mc, 1);
            } else if (!this.pitchCheck.getValue() || mc.thePlayer.rotationPitch >= 70.0F) {
                AccessorMinecraft.setRightClickDelayTimer(mc, 1000);
            }
        }
        this.tryPlace(held);
    }

    private void tryPlace(ItemStack held) {
        if (this.disableLeft.getValue() && Mouse.isButtonDown(0)) {
            return;
        }
        MovingObjectPosition target = mc.objectMouseOver;
        if (target == null || target.typeOfHit != MovingObjectType.BLOCK
                || target.sideHit == EnumFacing.UP || target.sideHit == EnumFacing.DOWN) {
            return;
        }
        if (this.lastTarget != null && this.frameCounter < this.frameDelay.getValue()) {
            this.frameCounter++;
            return;
        }
        this.lastTarget = target;
        BlockPos pos = target.getBlockPos();
        if (this.lastPos == null || pos.getX() != this.lastPos.getX()
                || pos.getY() != this.lastPos.getY() || pos.getZ() != this.lastPos.getZ()) {
            Block block = mc.theWorld.getBlockState(pos).getBlock();
            if (block != null && block != Blocks.air && !(block instanceof BlockLiquid)) {
                if (!this.holdRight.getValue() || Mouse.isButtonDown(1)) {
                    long now = System.currentTimeMillis();
                    if (now - this.lastPlaceMs >= this.minPlaceDelay.getValue()) {
                        this.lastPlaceMs = now;
                        if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld,
                                held, pos, target.sideHit, target.hitVec)) {
                            mc.thePlayer.swingItem();
                            if (mc.entityRenderer != null) {
                                Object renderer = myau.access.AccessorEntityRenderer
                                        .getItemRenderer(mc.entityRenderer);
                                if (renderer != null) {
                                    AccessorItemRenderer.resetEquippedProgress(renderer);
                                }
                            }
                            this.lastPos = pos;
                            this.frameCounter = 0;
                        }
                    }
                }
            }
        }
    }

    private void resetVariables() {
        this.lastPos = null;
        this.lastTarget = null;
        this.frameCounter = 0;
    }
}
