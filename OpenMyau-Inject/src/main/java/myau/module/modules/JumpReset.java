package myau.module.modules;

import myau.event.EventTarget;
import myau.events.MoveInputEvent;
import myau.module.Module;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import net.minecraft.client.Minecraft;

/**
 * Ported from Raven B4 JumpReset.
 * Raven's JumpEvent.setMotionY is replicated by overwriting motionY right
 * after the jump call, since this client's JumpEvent only carries yaw.
 */
public class JumpReset extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final float DEFAULT_MOTION = 0.42F;
    public final IntProperty chance = new IntProperty("Chance", 80, 0, 100);
    public final FloatProperty motion = new FloatProperty("Jump motion", DEFAULT_MOTION, 0.0F, 1.0F, 0.01F);

    private boolean jump;

    public JumpReset() {
        super("Jump Reset", false);
    }

    @Override
    public void onDisabled() {
        this.jump = false;
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) {
            return;
        }
        if (this.chance.getValue() == 0) {
            return;
        }
        if (mc.thePlayer.maxHurtTime <= 0) {
            this.jump = false;
            return;
        }
        if (mc.thePlayer.hurtTime == mc.thePlayer.maxHurtTime) {
            this.jump = true;
        }
        if (!this.jump || mc.thePlayer.hurtTime == 0) {
            this.jump = false;
            return;
        }
        if (this.chance.getValue() != 100 && Math.random() >= this.chance.getValue() / 100.0D) {
            return;
        }
        if (this.jump && mc.thePlayer.onGround) {
            mc.thePlayer.jump();
            if (this.motion.getValue() != DEFAULT_MOTION) {
                mc.thePlayer.motionY = this.motion.getValue();
            }
            this.jump = false;
        }
    }
}
