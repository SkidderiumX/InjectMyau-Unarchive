package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.UpdateEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.util.ItemUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

/**
 * Swaps to the hotbar slot holding the most blocks. Modes:
 * "Out of blocks" triggers on the held stack running low (blocks threshold);
 * "Highest amount" keeps the biggest block stack in hand at all times.
 * "Swap delay" throttles consecutive swaps so a stack run does not cycle
 * through every slot at once. "Scaffold-only" restricts all of that to while
 * the Scaffold module is actively scaffolding.
 */
public class AutoSwap extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final int MODE_OUT_OF_BLOCKS = 0;
    private static final int MODE_HIGHEST_AMOUNT = 1;

    public final ModeProperty mode = new ModeProperty("mode", MODE_OUT_OF_BLOCKS,
            new String[]{"Out of blocks", "Highest amount"});
    public final IntProperty blocks = new IntProperty("blocks", 1, 1, 10,
            () -> this.mode.getValue() == MODE_OUT_OF_BLOCKS);
    public final BooleanProperty swapDelay = new BooleanProperty("swap-delay", false);
    public final IntProperty delayMs = new IntProperty("delay-ms", 25, 1, 50,
            () -> this.swapDelay.getValue());
    public final BooleanProperty scaffoldOnly = new BooleanProperty("scaffold-only", false);

    private long lastSwapMs = 0L;

    public AutoSwap() {
        super("AutoSwap", false);
    }

    @Override
    public void onEnabled() {
        this.lastSwapMs = 0L;
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE
                || mc.thePlayer == null || mc.theWorld == null
                || mc.currentScreen != null) {
            return;
        }
        if (this.scaffoldOnly.getValue()) {
            Scaffold scaffold = (Scaffold) Myau.moduleManager.modules.get(Scaffold.class);
            if (scaffold == null || !scaffold.isScaffolding()) {
                return;
            }
        }
        if (this.mode.getValue() == MODE_OUT_OF_BLOCKS) {
            ItemStack held = mc.thePlayer.getHeldItem();
            int heldCount = held != null && ItemUtil.isBlock(held) ? held.stackSize : 0;
            if (heldCount > this.blocks.getValue()) {
                return;
            }
        }
        int slot = this.bestBlockSlot();
        if (slot < 0) {
            return;
        }
        long now = System.currentTimeMillis();
        if (this.swapDelay.getValue() && now - this.lastSwapMs < this.delayMs.getValue()) {
            return;
        }
        this.lastSwapMs = now;
        mc.thePlayer.inventory.currentItem = slot;
    }

    /**
     * Hotbar slot (0-8) holding strictly more blocks than the current hand, or
     * -1 when nothing better exists. In "Out of blocks" mode this only fires
     * once the held stack is below the threshold; in "Highest amount" mode it
     * also takes over for a non-block item in hand.
     */
    private int bestBlockSlot() {
        ItemStack held = mc.thePlayer.getHeldItem();
        int heldCount = held != null && ItemUtil.isBlock(held) ? held.stackSize : 0;
        int current = mc.thePlayer.inventory.currentItem;
        int best = -1;
        int bestCount = heldCount;
        for (int i = 0; i < 9; i++) {
            if (i == current) {
                continue;
            }
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && ItemUtil.isBlock(stack) && stack.stackSize > bestCount) {
                bestCount = stack.stackSize;
                best = i;
            }
        }
        return best;
    }
}
