package myau.module.modules;

import myau.events.PrePlayerInteractEvent;
import myau.event.EventTarget;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.access.AccessorMinecraft;
import myau.util.BlockUtil;
import myau.util.RotationUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Mouse;

public class AutoTool extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private final FloatProperty activationTime = new FloatProperty("Activation time", 0.0F, 0.0F, 1000.0F, 25.0F);
    private final FloatProperty hoverDelay = new FloatProperty("Hover delay", 0.0F, 0.0F, 1000.0F, 25.0F);
    private final BooleanProperty onlyWhileCrouching = new BooleanProperty("Only while crouching", false);
    private final BooleanProperty requireLeftMouse = new BooleanProperty("Require left mouse", true);
    private final BooleanProperty switchBackWhenDone = new BooleanProperty("Switch back when done", true);

    private boolean hasSwapped;
    private int previousSlot = -1;
    private int tickCounter;
    private int leftMouseDownSinceTick = -1;
    private int hoverStartTick = -1;

    public AutoTool() {
        super("Auto Tool", true, true);
    }

    @Override
    public void onEnabled() {
        resetState(true);
    }

    @Override
    public void onDisabled() {
        resetState(true);
    }

    @EventTarget
    public void onPrePlayerInteract(PrePlayerInteractEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            resetState(true);
            return;
        }

        int currentTick = ++tickCounter;
        boolean leftMouseDown = Mouse.isButtonDown(0);
        updateLeftMouseState(leftMouseDown, currentTick);

        if (!mc.inGameHasFocus || mc.currentScreen != null || mc.thePlayer.isDead || !mc.thePlayer.capabilities.allowEdit) {
            resetState(true);
            return;
        }

        MovingObjectPosition hoverResult = RotationUtil.rayTrace(
                mc.thePlayer.rotationYaw,
                mc.thePlayer.rotationPitch,
                mc.playerController.getBlockReachDistance(),
                AccessorMinecraft.getTimer(mc).renderPartialTicks
        );
        BlockPos hoverPos = hoverResult != null
                && hoverResult.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                ? hoverResult.getBlockPos()
                : null;
        updateHoverState(hoverPos, currentTick);

        if (hoverPos == null) {
            resetSlot();
            return;
        }

        if (onlyWhileCrouching.getValue() && !mc.thePlayer.isSneaking()) {
            resetSlot();
            return;
        }

        if (requireLeftMouse.getValue()) {
            if (!leftMouseDown) {
                resetSlot();
                return;
            }
            if (!hasElapsed(leftMouseDownSinceTick, activationTime.getValue(), currentTick)) {
                resetSlot();
                return;
            }
        }

        if (!hasElapsed(hoverStartTick, hoverDelay.getValue(), currentTick)) {
            resetSlot();
            return;
        }

        if (mc.thePlayer.isUsingItem()) {
            resetSlot();
            return;
        }

        MovingObjectPosition swapResult = mc.objectMouseOver;
        BlockPos swapPos = swapResult != null
                && swapResult.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                ? swapResult.getBlockPos()
                : null;
        if (swapPos == null) {
            resetSlot();
            return;
        }

        int slot = getToolSlot(mc.theWorld.getBlockState(swapPos).getBlock());
        if (slot == -1) {
            return;
        }

        if (previousSlot == -1 && slot != mc.thePlayer.inventory.currentItem) {
            previousSlot = mc.thePlayer.inventory.currentItem;
        }

        if (!hasSwapped) {
            setSlot(slot);
            return;
        }

        if (slot != mc.thePlayer.inventory.currentItem) {
            setSlot(slot);
        }
    }

    private void updateLeftMouseState(boolean leftMouseDown, int currentTick) {
        if (leftMouseDown) {
            if (leftMouseDownSinceTick == -1) {
                leftMouseDownSinceTick = currentTick;
            }
        } else {
            leftMouseDownSinceTick = -1;
        }
    }

    private void updateHoverState(BlockPos hoverPos, int currentTick) {
        if (hoverPos == null) {
            hoverStartTick = -1;
            return;
        }
        if (hoverStartTick == -1) {
            hoverStartTick = currentTick;
        }
    }

    private int getToolSlot(Block block) {
        if (block == null) return -1;
        int bestSlot = -1;
        float bestSpeed = 0.0F;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack == null) continue;
            float speed = stack.getStrVsBlock(block);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private boolean hasElapsed(int startTick, double requiredMs, int currentTick) {
        int requiredTicks = getRequiredTicks(requiredMs);
        if (requiredTicks <= 0) return true;
        return startTick != -1 && currentTick - startTick >= requiredTicks;
    }

    private int getRequiredTicks(double requiredMs) {
        if (requiredMs <= 0.0) return 0;
        return (int) Math.ceil(requiredMs / 50.0);
    }

    private void resetState(boolean resetTimers) {
        if (resetTimers) {
            tickCounter = 0;
            leftMouseDownSinceTick = -1;
            hoverStartTick = -1;
        }
        resetSlot();
    }

    private void resetSlot() {
        if (previousSlot != -1 && switchBackWhenDone.getValue()) {
            setSlot(previousSlot);
        }
        previousSlot = -1;
        hasSwapped = false;
    }

    private void setSlot(int currentItem) {
        if (currentItem == -1 || currentItem == mc.thePlayer.inventory.currentItem) return;
        mc.thePlayer.inventory.currentItem = currentItem;
        hasSwapped = true;
    }
}
