package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.UpdateEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.ModeProperty;
import myau.util.MoveUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;

/**
 * Ported from Raven B4 BHop.
 * BedAura/Tower interlocks were dropped (no equivalents in this client) and
 * the dead rotate-yaw flag went with them.
 */
public class BHop extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final ModeProperty mode = new ModeProperty("Mode", 0,
            new String[]{"Strafe", "Ground", "Glide", "7 tick", "7 tick strafe",
                    "8 tick", "8 tick strafe", "9 tick", "9 tick strafe"});
    public final FloatProperty speed = new FloatProperty("Speed", 2.0F, 0.5F, 8.0F, 0.1F);
    public final BooleanProperty liquidDisable = new BooleanProperty("Disable in liquid", true);
    public final BooleanProperty sneakDisable = new BooleanProperty("Disable while sneaking", true);

    private int strafies;
    private boolean hopping;
    private boolean lowhop;
    private boolean didMove;
    private boolean collided;

    public BHop() {
        super("BHop", false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE
                || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }
        if (mc.thePlayer.onGround && !mc.thePlayer.capabilities.isFlying && this.hopping) {
            mc.thePlayer.movementInput.jump = false;
        }
        this.onPreMotion();
        this.onPreUpdate();
    }

    private void onPreMotion() {
        if (((mc.thePlayer.isInWater() || mc.thePlayer.isInLava()) && this.liquidDisable.getValue())
                || (mc.thePlayer.isSneaking() && this.sneakDisable.getValue())) {
            return;
        }
        Scaffold scaffold = (Scaffold) Myau.moduleManager.modules.get(Scaffold.class);
        if (scaffold.isEnabled()) {
            return;
        }
        if (!MoveUtil.isMoving()) {
            return;
        }
        if (this.mode.getValue() >= 1) {
            if (mc.thePlayer.isCollidedHorizontally) {
                this.collided = true;
            } else if (mc.thePlayer.onGround) {
                this.collided = false;
            }
            if (mc.thePlayer.onGround) {
                mc.thePlayer.jump();
                double horizontalSpeed = MoveUtil.getSpeed();
                double speedModifier = 0.48D;
                final int speedAmplifier = MoveUtil.getSpeedLevel();
                switch (speedAmplifier) {
                    case 1:
                        speedModifier = 0.5D;
                        break;
                    case 2:
                        speedModifier = 0.52D;
                        break;
                    case 3:
                        speedModifier = 0.58D;
                        break;
                    default:
                        break;
                }
                double additionalSpeed = speedModifier * ((this.speed.getValue() - 1.0D) / 3.0D + 1.0D);
                if (horizontalSpeed < additionalSpeed) {
                    horizontalSpeed = additionalSpeed;
                }
                if (MoveUtil.isMoving() && !noSlowingBackWithBow()) {
                    MoveUtil.setSpeed(horizontalSpeed);
                    this.didMove = true;
                }
                this.hopping = true;
            }
        }
        double posY = mc.thePlayer.posY;
        switch (this.mode.getValue()) {
            case 0:
                if (MoveUtil.isMoving()) {
                    if (mc.thePlayer.onGround) {
                        mc.thePlayer.jump();
                    }
                    mc.thePlayer.setSprinting(true);
                    MoveUtil.setSpeed(MoveUtil.getSpeed() + 0.005D * this.speed.getValue());
                    this.hopping = true;
                }
                break;
            case 3:
            case 4:
                if (this.didMove) {
                    int simpleY = (int) Math.round((posY % 1.0D) * 10000.0D);
                    if (mc.thePlayer.hurtTime == 0 && !this.collided) {
                        switch (simpleY) {
                            case 4200:
                                mc.thePlayer.motionY = 0.39D;
                                this.lowhop = true;
                                break;
                            case 1138:
                                mc.thePlayer.motionY = mc.thePlayer.motionY - 0.13D;
                                this.lowhop = false;
                                break;
                            case 2031:
                                mc.thePlayer.motionY = mc.thePlayer.motionY - 0.2D;
                                this.didMove = false;
                                break;
                            default:
                                break;
                        }
                    }
                }
                break;
            case 5:
            case 6:
                if (this.didMove) {
                    int simpleY = (int) Math.round((posY % 1.0D) * 10000.0D);
                    if (mc.thePlayer.hurtTime == 0 && !this.collided) {
                        switch (simpleY) {
                            case 13:
                                mc.thePlayer.motionY = mc.thePlayer.motionY - 0.02483D;
                                break;
                            case 2000:
                                mc.thePlayer.motionY = mc.thePlayer.motionY - 0.1913D;
                                this.didMove = false;
                                break;
                            default:
                                break;
                        }
                    }
                }
                break;
            case 7:
            case 8:
                if (this.didMove) {
                    int simpleY = (int) Math.round((posY % 1.0D) * 10000.0D);
                    if (mc.thePlayer.hurtTime == 0 && !this.collided) {
                        switch (simpleY) {
                            case 13:
                                mc.thePlayer.motionY = mc.thePlayer.motionY - 0.02483D;
                                break;
                            case 2000:
                                mc.thePlayer.motionY = mc.thePlayer.motionY - 0.16874D;
                                break;
                            default:
                                break;
                        }
                    }
                }
                break;
            default:
                break;
        }
    }

    private void onPreUpdate() {
        if (this.canStrafe()) {
            this.strafies = mc.thePlayer.onGround ? 0 : this.strafies + 1;
            if (mc.thePlayer.fallDistance > 1.0F || mc.thePlayer.onGround) {
                this.strafies = 0;
                return;
            }
            if (this.strafies == 1) {
                this.strafe();
            }
            if (!this.blockRelativeToPlayer(0.0D, mc.thePlayer.motionY, 0.0D)
                    .getUnlocalizedName().contains("air") && this.strafies > 2) {
                this.strafe();
            }
            Scaffold scaffold = (Scaffold) Myau.moduleManager.modules.get(Scaffold.class);
            int currentMode = this.mode.getValue();
            if (this.strafies >= 2 && (!this.blockRelativeToPlayer(0.0D, mc.thePlayer.motionY * 3.0D, 0.0D)
                    .getUnlocalizedName().contains("air") || this.strafies == 9)
                    && !scaffold.isEnabled()
                    && (currentMode == 2 || currentMode == 4 || currentMode == 6 || currentMode == 8)) {
                mc.thePlayer.motionY += 0.0754D;
                this.strafe();
            }
        }
    }

    private boolean canStrafe() {
        return mc.thePlayer.hurtTime == 0
                && !mc.thePlayer.isUsingItem()
                && mc.gameSettings.keyBindForward.isKeyDown();
    }

    private boolean noSlowingBackWithBow() {
        ItemStack held = mc.thePlayer.getHeldItem();
        return held != null && held.getItem() instanceof ItemBow && mc.thePlayer.isUsingItem();
    }

    private void strafe() {
        MoveUtil.setSpeed(MoveUtil.getSpeed());
    }

    private Block blockRelativeToPlayer(double offsetX, double offsetY, double offsetZ) {
        Vec3 pos = mc.thePlayer.getPositionVector();
        BlockPos blockPos = new BlockPos(Math.floor(pos.xCoord + offsetX),
                Math.floor(pos.yCoord + offsetY), Math.floor(pos.zCoord + offsetZ));
        return mc.theWorld.getBlockState(blockPos).getBlock();
    }

    @Override
    public void onDisabled() {
        this.hopping = false;
        this.strafies = 0;
    }

    @Override
    public String[] getSuffix() {
        String[] modes = this.mode.getModes();
        int index = this.mode.getValue();
        return new String[]{index >= 0 && index < modes.length ? modes[index] : "?"};
    }
}
