package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.KeyEvent;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.ModeProperty;
import myau.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class MCF extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final ModeProperty action = new ModeProperty("Action", 0, new String[]{"FRIEND", "PEARL"});

    private int pearlEvent = 4;
    private int prevSlot;

    public MCF() {
        super("MCF", false, true);
    }

    @EventTarget
    public void onKey(KeyEvent event) {
        if (!this.isEnabled() || event.getKey() != -98 || mc.thePlayer == null) {
            return;
        }
        if (this.action.getValue() == 1) {
            this.throwPearl();
            return;
        }
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectType.ENTITY && mc.objectMouseOver.entityHit instanceof EntityPlayer) {
            String hitName = mc.objectMouseOver.entityHit.getName();
            if (!Myau.friendManager.isFriend(hitName)) {
                Myau.friendManager.add(hitName);
                ChatUtil.sendFormatted(String.format("%sAdded &o%s&r to your friend list&r", Myau.clientName, hitName));
            } else {
                Myau.friendManager.remove(hitName);
                ChatUtil.sendFormatted(String.format("%sRemoved &o%s&r from your friend list&r", Myau.clientName, hitName));
            }
        }
    }

    private void throwPearl() {
        for (int slot = 0; slot <= 8; slot++) {
            ItemStack inSlot = mc.thePlayer.inventory.getStackInSlot(slot);
            if (inSlot != null && inSlot.getItem() instanceof ItemEnderPearl) {
                this.prevSlot = mc.thePlayer.inventory.currentItem;
                mc.thePlayer.inventory.currentItem = slot;
                mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, inSlot);
                this.pearlEvent = 0;
                return;
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE
                || mc.thePlayer == null || this.action.getValue() != 1) {
            return;
        }
        if (this.pearlEvent < 4) {
            if (this.pearlEvent == 3) {
                mc.thePlayer.inventory.currentItem = this.prevSlot;
            }
            this.pearlEvent++;
        }
    }
}
