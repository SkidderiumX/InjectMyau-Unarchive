package myau.module.modules;

import myau.access.AccessorC03PacketPlayer;
import myau.access.AccessorC0DPacketCloseWindow;
import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.LoadWorldEvent;
import myau.events.MoveInputEvent;
import myau.events.PacketEvent;
import myau.events.SprintEvent;
import myau.events.StrafeEvent;
import myau.events.TickEvent;
import myau.events.UpdateEvent;
import myau.management.blockage.BlockHolder;
import myau.management.blockage.OutboundNetworkBlockage;
import myau.management.blockage.PacketTransformer;
import myau.management.blockage.PacketValidator;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.TextProperty;
import myau.util.PacketUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.Packet;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.PacketBuffer;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition;
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0CPacketInput;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C13PacketPlayerAbilities;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.play.client.C18PacketSpectate;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public class Disabler extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final int MODE_WATCHDOG = 0;
    private static final int MODE_ADVANCED = 1;

    private static final String SPOOFED_ADDRESS = "1iro.cc/spoofing";

    private static final long TIMER_SPIKE_INTERVAL_MS = 700L;

    private static final long TIMER_HOLD_WINDOW_MS = 350L;

    private static final int CLICK_MODE_QUICK_MOVE = 1;

    private static final int CLICK_MODE_SWAP = 2;

    private static final int CLICK_MODE_THROW = 4;

    private static final PacketTransformer INV_TRANSFORMER = packet -> packet;

    private static final PacketValidator INV_VALIDATOR = packet -> !(packet instanceof C0EPacketClickWindow
            || packet instanceof C0DPacketCloseWindow
            || packet instanceof C0FPacketConfirmTransaction
            || packet instanceof C00PacketKeepAlive);

    private static final String BRAND_CHANNEL = "MC|Brand";

    private static final String MOD_CONTROL_CHANNEL = "badlion:mods";

    private static final String[] HYPIXEL_PREFIXES = {"hypixel:", "hyevent:"};

    public final ModeProperty mode = new ModeProperty("mode", MODE_WATCHDOG, new String[]{"Watchdog", "Advanced"});
    public final TextProperty brand = new TextProperty("brand", "vanilla",
            () -> this.mode.getValue() == MODE_WATCHDOG);

    public final BooleanProperty hypixelBrand = new BooleanProperty("Hypixel Brand", true,
            () -> this.mode.getValue() == MODE_WATCHDOG);

    public final BooleanProperty watchdogInv = new BooleanProperty("Watchdog Inv", false,
            () -> this.mode.getValue() == MODE_WATCHDOG);

    public final BooleanProperty spoofIp = new BooleanProperty("Spoof IP", false,
            () -> this.mode.getValue() == MODE_WATCHDOG);

    public final BooleanProperty duplicateRotPlace = new BooleanProperty("Duplicate Rot Place", false);

    // Advanced (multiply) mode: each toggle below mirrors one Rise disabler from
    // module/impl/exploit/disabler so several can run at the same time.
    public final BooleanProperty advCancelTransactions = new BooleanProperty("Cancel Transactions", false,
            () -> this.mode.getValue() == MODE_ADVANCED);
    public final BooleanProperty advCancelKeepAlive = new BooleanProperty("Cancel KeepAlive", false,
            () -> this.mode.getValue() == MODE_ADVANCED);
    public final BooleanProperty advConvertMove = new BooleanProperty("Convert Move", false,
            () -> this.mode.getValue() == MODE_ADVANCED);
    public final BooleanProperty advSpectate = new BooleanProperty("Spectate Spoof", false,
            () -> this.mode.getValue() == MODE_ADVANCED);
    public final BooleanProperty advInput = new BooleanProperty("Input Spoof", false,
            () -> this.mode.getValue() == MODE_ADVANCED);
    public final BooleanProperty advVehicle = new BooleanProperty("Vehicle Packets", false,
            () -> this.mode.getValue() == MODE_ADVANCED);
    public final BooleanProperty advAbilities = new BooleanProperty("Abilities Spoof", false,
            () -> this.mode.getValue() == MODE_ADVANCED);
    public final BooleanProperty advGrimSpectate = new BooleanProperty("Grim Spectate", false,
            () -> this.mode.getValue() == MODE_ADVANCED);
    public final BooleanProperty advGrimInventory = new BooleanProperty("Grim Inventory", false,
            () -> this.mode.getValue() == MODE_ADVANCED);
    public final BooleanProperty advVerusSprint = new BooleanProperty("Verus Sprint", false,
            () -> this.mode.getValue() == MODE_ADVANCED);
    public final BooleanProperty advVulcanSprint = new BooleanProperty("Vulcan Sprint", false,
            () -> this.mode.getValue() == MODE_ADVANCED);
    public final BooleanProperty advMMCReach = new BooleanProperty("MMC Reach", false,
            () -> this.mode.getValue() == MODE_ADVANCED);
    public final BooleanProperty advTest = new BooleanProperty("Test Payload", false,
            () -> this.mode.getValue() == MODE_ADVANCED);
    public final BooleanProperty advSprint = new BooleanProperty("Cancel Sprint", false,
            () -> this.mode.getValue() == MODE_ADVANCED);
    public final BooleanProperty advOmniSprint = new BooleanProperty("OmniSprint", false,
            () -> this.mode.getValue() == MODE_ADVANCED);
    public final BooleanProperty advVerusMovement = new BooleanProperty("Verus Movement", false,
            () -> this.mode.getValue() == MODE_ADVANCED);

    private static final float DUPLICATE_ROT_MIN_DELTA = 2.0F;
    private static final float DUPLICATE_ROT_EPSILON = 0.0001F;
    private static final float DUPLICATE_ROT_NUDGE = 0.0002F;
    private float duplicateLastYaw = Float.NaN;
    private float duplicateDeltaYaw = 0.0F;
    private float duplicateLastPlacedDeltaYaw = 0.0F;
    private boolean duplicateRotated = false;

    private volatile float omniForward;
    private volatile float omniStrafe;

    private volatile ServerData originalServerData = null;

    private volatile ServerData spoofedServerData = null;

    private final CopyOnWriteArrayList<Packet<?>> timerHeldPackets = new CopyOnWriteArrayList<Packet<?>>();

    private volatile long timerHoldStartMs = System.currentTimeMillis();

    private volatile long timerSpikeStartMs = System.currentTimeMillis();

    private final BlockHolder invBlockHolder = new BlockHolder(OutboundNetworkBlockage.get());

    private volatile boolean invShouldBlink;

    private final ConcurrentLinkedQueue<Packet<?>> grimSpectateHeld = new ConcurrentLinkedQueue<Packet<?>>();
    private volatile boolean grimSpectateFlagged;
    private volatile boolean grimInvResending;
    private volatile boolean grimInvSprinting;
    private volatile boolean grimInvKnown;

    public Disabler() {
        super("Disabler", false);
    }

    @Override
    public String[] getSuffix() {
        if (this.mode.getValue() == MODE_ADVANCED) {
            List<String> active = new ArrayList<String>();
            if (this.advCancelTransactions.getValue()) {
                active.add("Trans");
            }
            if (this.advCancelKeepAlive.getValue()) {
                active.add("KeepAlive");
            }
            if (this.advConvertMove.getValue()) {
                active.add("Convert");
            }
            if (this.advSpectate.getValue()) {
                active.add("Spectate");
            }
            if (this.advInput.getValue()) {
                active.add("Input");
            }
            if (this.advVehicle.getValue()) {
                active.add("Vehicle");
            }
            if (this.advAbilities.getValue()) {
                active.add("Abilities");
            }
            if (this.advGrimSpectate.getValue()) {
                active.add("GrimSpec");
            }
            if (this.advGrimInventory.getValue()) {
                active.add("GrimInv");
            }
            if (this.advVerusSprint.getValue()) {
                active.add("Verus");
            }
            if (this.advVulcanSprint.getValue()) {
                active.add("Vulcan");
            }
            if (this.advMMCReach.getValue()) {
                active.add("MMCReach");
            }
            if (this.advTest.getValue()) {
                active.add("Test");
            }
            if (this.advSprint.getValue()) {
                active.add("Sprint");
            }
            if (this.advOmniSprint.getValue()) {
                active.add("Omni");
            }
            if (this.advVerusMovement.getValue()) {
                active.add("VerusMove");
            }
            if (active.isEmpty()) {
                return new String[]{"Advanced"};
            }
            return active.toArray(new String[0]);
        }
        return new String[]{this.mode.getModeString()};
    }

    @Override
    public void onDisabled() {
        this.restoreServerAddress();
        this.flushTimerPackets();
        this.flushGrimSpectateHeld();
        this.grimInvResending = false;
        this.grimInvKnown = false;
        this.grimInvSprinting = false;
        this.invShouldBlink = false;
        this.invBlockHolder.release();
    }

    private boolean isWatchdogActive() {
        return this.isEnabled() && this.mode.getValue() == MODE_WATCHDOG;
    }

    private boolean isAdvancedActive() {
        return this.isEnabled() && this.mode.getValue() == MODE_ADVANCED;
    }

    private void updateServerAddressSpoof() {
        if (!this.isWatchdogActive() || !this.spoofIp.getValue()) {
            this.restoreServerAddress();
            return;
        }
        ServerData current = mc.getCurrentServerData();
        if (current == null || current == this.spoofedServerData) {
            return;
        }
        ServerData copy = new ServerData(current.serverName, SPOOFED_ADDRESS, current.isOnLAN());
        copy.copyFrom(current);
        copy.serverIP = SPOOFED_ADDRESS;
        this.originalServerData = current;
        this.spoofedServerData = copy;
        mc.setServerData(copy);
    }

    private void restoreServerAddress() {
        ServerData spoofed = this.spoofedServerData;
        ServerData original = this.originalServerData;
        this.spoofedServerData = null;
        this.originalServerData = null;
        if (spoofed != null && original != null && mc.getCurrentServerData() == spoofed) {
            mc.setServerData(original);
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() != EventType.PRE) {
            return;
        }
        if (this.isWatchdogActive()) {
            long now = System.currentTimeMillis();
            if (now - this.timerSpikeStartMs > TIMER_SPIKE_INTERVAL_MS) {
                this.timerSpikeStartMs = now;
                this.timerHoldStartMs = now;
            }
            return;
        }
        if (!this.isAdvancedActive() || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }
        if (this.advVehicle.getValue()) {
            PacketUtil.sendPacketNoEvent(new C0CPacketInput());
        }
        if (this.advAbilities.getValue() && mc.thePlayer.ticksExisted % 5 == 0) {
            PlayerCapabilities capabilities = new PlayerCapabilities();
            capabilities.isFlying = true;
            PacketUtil.sendPacketNoEvent(new C13PacketPlayerAbilities(capabilities));
        }
        if (this.advVerusSprint.getValue()
                && mc.thePlayer.ticksExisted > 50
                && mc.thePlayer.ticksExisted % 5 == 0) {
            if (mc.thePlayer.ticksExisted % 10 == 0) {
                PacketUtil.sendPacketNoEvent(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
            } else {
                PacketUtil.sendPacketNoEvent(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
            }
        }
        if (this.advVulcanSprint.getValue()) {
            PacketUtil.sendPacketNoEvent(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
            PacketUtil.sendPacketNoEvent(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
            if (mc.thePlayer.ticksExisted % 5 == 0) {
                PacketUtil.sendPacketNoEvent(new C07PacketPlayerDigging(
                        C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK,
                        new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ),
                        EnumFacing.UP));
            }
            if (mc.thePlayer.ticksExisted % 100 == 0) {
                PacketUtil.sendPacketNoEvent(new C07PacketPlayerDigging(
                        C07PacketPlayerDigging.Action.START_DESTROY_BLOCK,
                        new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ),
                        EnumFacing.UP));
            }
            if (mc.thePlayer.ticksExisted % 9 == 0) {
                if (!mc.thePlayer.onGround && Math.random() <= 0.7) {
                    PacketUtil.sendPacketNoEvent(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SNEAKING));
                }
                if (mc.thePlayer.onGround && !mc.gameSettings.keyBindJump.isKeyDown() && Math.random() <= 0.2) {
                    PacketUtil.sendPacketNoEvent(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SNEAKING));
                }
            }
        }
        if (this.advOmniSprint.getValue() && mc.thePlayer.ticksExisted >= 50) {
            if (mc.thePlayer.movementInput != null) {
                float forward = mc.thePlayer.movementInput.moveForward;
                float strafe = mc.thePlayer.movementInput.moveStrafe;
                if ((Math.abs(forward) > 0.0F || Math.abs(strafe) > 0.0F)
                        && !mc.gameSettings.keyBindForward.isKeyDown()) {
                    double angle = Math.toDegrees(Math.atan2(strafe, forward));
                    float yaw = mc.thePlayer.rotationYaw + (float) angle;
                    event.setRotation(yaw, mc.thePlayer.rotationPitch, 10);
                }
            }
        }
        if (this.advVerusMovement.getValue() && mc.thePlayer.ticksExisted > 180) {
            try {
                myau.module.modules.Speed speed = (myau.module.modules.Speed) Myau.moduleManager.modules.get(myau.module.modules.Speed.class);
                myau.module.modules.Fly fly = (myau.module.modules.Fly) Myau.moduleManager.modules.get(myau.module.modules.Fly.class);
                if (mc.thePlayer.motionX * mc.thePlayer.motionX + mc.thePlayer.motionZ * mc.thePlayer.motionZ > 0.09) {
                    if (speed != null && speed.isEnabled()) {
                        speed.toggle();
                        myau.util.ChatUtil.sendFormatted("&cAuto Toggle: disabled Speed to prevent flags");
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }

    @EventTarget(Priority.HIGH)
    public void onTick(TickEvent event) {
        if (event.getType() != EventType.PRE) {
            return;
        }
        this.updateServerAddressSpoof();
        if (!this.isWatchdogActive() || !this.watchdogInv.getValue()) {
            this.invShouldBlink = false;
            this.invBlockHolder.release();
            return;
        }
        if (mc.currentScreen == null) {
            this.invShouldBlink = false;
        }
        if (this.invShouldBlink) {
            this.invBlockHolder.block(INV_TRANSFORMER, INV_VALIDATOR);
        } else {
            this.invBlockHolder.release();
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.updateServerAddressSpoof();
        this.timerHeldPackets.clear();
        this.timerHoldStartMs = System.currentTimeMillis();
        this.invShouldBlink = false;
        this.grimSpectateHeld.clear();
        this.grimSpectateFlagged = false;
        this.grimInvResending = false;
        this.grimInvKnown = false;
        this.grimInvSprinting = false;
    }

    @EventTarget(Priority.LOW)
    public void onDuplicateRotPlace(PacketEvent event) {
        if (event.getType() != EventType.SEND || event.isCancelled()
                || !this.isEnabled() || !this.duplicateRotPlace.getValue()) {
            return;
        }
        Packet<?> packet = event.getPacket();
        if (packet instanceof C03PacketPlayer) {
            C03PacketPlayer movement = (C03PacketPlayer) packet;
            if (!movement.getRotating()) {
                return;
            }
            float yaw = movement.getYaw();
            float previous = this.duplicateLastYaw;
            this.duplicateLastYaw = yaw;
            if (Float.isNaN(previous)) {
                return;
            }
            this.duplicateDeltaYaw = Math.abs(yaw - previous);
            this.duplicateRotated = true;
            if (this.duplicateDeltaYaw > DUPLICATE_ROT_MIN_DELTA
                    && Math.abs(this.duplicateDeltaYaw - this.duplicateLastPlacedDeltaYaw)
                            < DUPLICATE_ROT_EPSILON) {
                float nudged = yaw + DUPLICATE_ROT_NUDGE;
                AccessorC03PacketPlayer.setYaw(movement, nudged);
                this.duplicateLastYaw = nudged;
                this.duplicateDeltaYaw = Math.abs(nudged - previous);
            }
        } else if (packet instanceof C08PacketPlayerBlockPlacement && this.duplicateRotated) {
            this.duplicateLastPlacedDeltaYaw = this.duplicateDeltaYaw;
            this.duplicateRotated = false;
        }
    }

    @EventTarget(Priority.HIGHEST)
    public void onPacketSend(PacketEvent event) {
        if (event.getType() != EventType.SEND) {
            return;
        }
        if (event.getPacket() instanceof C00Handshake
                && ((C00Handshake) event.getPacket()).getRequestedState() == EnumConnectionState.LOGIN) {
            this.updateServerAddressSpoof();
            return;
        }
        if (this.isAdvancedActive() && mc.thePlayer != null) {
            this.handleAdvancedSend(event, event.getPacket());
            if (event.isCancelled()) {
                return;
            }
        }
        if (!this.isWatchdogActive() || mc.thePlayer == null) {
            this.flushTimerPackets();
            return;
        }
        Packet<?> packet = event.getPacket();
        if (this.watchdogInv.getValue()) {
            this.handleInvPacket(packet);
        }
        if (event.isCancelled()) {
            return;
        }
    }

    private void handleAdvancedSend(PacketEvent event, Packet<?> packet) {
        if (this.advCancelTransactions.getValue()
                && packet instanceof C0FPacketConfirmTransaction) {
            event.setCancelled(true);
            return;
        }
        if (this.advCancelKeepAlive.getValue()
                && packet instanceof C00PacketKeepAlive) {
            event.setCancelled(true);
            return;
        }
        if (this.advMMCReach.getValue()
                && packet instanceof C0FPacketConfirmTransaction
                && mc.thePlayer != null
                && mc.thePlayer.ticksExisted % 6 != 0) {
            event.setCancelled(true);
            return;
        }
        if (this.advTest.getValue() && packet instanceof C17PacketCustomPayload) {
            C17PacketCustomPayload payload = (C17PacketCustomPayload) packet;
            try {
                PacketBuffer buf = payload.getBufferData();
                buf.clear();
                buf.writeBytes("eyser".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            } catch (Throwable ignored) {
            }
        }
        if (this.advConvertMove.getValue()
                && packet instanceof C06PacketPlayerPosLook) {
            C06PacketPlayerPosLook posLook = (C06PacketPlayerPosLook) packet;
            event.setCancelled(true);
            PacketUtil.sendPacketNoEvent(new C04PacketPlayerPosition(
                    posLook.getPositionX(), posLook.getPositionY(), posLook.getPositionZ(),
                    posLook.isOnGround()));
            return;
        }
        if (this.advGrimInventory.getValue() && this.handleGrimInventorySend(event, packet)) {
            return;
        }
        if (this.advVerusSprint.getValue() && packet instanceof C0BPacketEntityAction) {
            C0BPacketEntityAction.Action action = ((C0BPacketEntityAction) packet).getAction();
            if (action == C0BPacketEntityAction.Action.START_SPRINTING
                    || action == C0BPacketEntityAction.Action.STOP_SPRINTING) {
                event.setCancelled(true);
            }
        }
    }

    private boolean handleGrimInventorySend(PacketEvent event, Packet<?> packet) {
        if (packet instanceof C0BPacketEntityAction) {
            C0BPacketEntityAction action = (C0BPacketEntityAction) packet;
            if (action.getAction() == C0BPacketEntityAction.Action.START_SPRINTING) {
                if (this.grimInvKnown && this.grimInvSprinting) {
                    event.setCancelled(true);
                    return true;
                }
                this.grimInvSprinting = true;
                this.grimInvKnown = true;
            } else if (action.getAction() == C0BPacketEntityAction.Action.STOP_SPRINTING) {
                if (this.grimInvKnown && !this.grimInvSprinting) {
                    event.setCancelled(true);
                    return true;
                }
                this.grimInvSprinting = false;
                this.grimInvKnown = true;
            }
            return false;
        }
        if (this.grimInvResending) {
            return false;
        }
        if (!(packet instanceof C0EPacketClickWindow) && !(packet instanceof C0DPacketCloseWindow)) {
            return false;
        }
        if (!this.grimInvKnown || !this.grimInvSprinting) {
            return false;
        }
        event.setCancelled(true);
        if (this.grimInvSprinting) {
            PacketUtil.sendPacketNoEvent(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
            this.grimInvSprinting = false;
            this.grimInvKnown = true;
        }
        this.grimInvResending = true;
        try {
            PacketUtil.sendPacket(packet);
        } finally {
            this.grimInvResending = false;
        }
        if (mc.thePlayer.isSprinting() && !this.grimInvSprinting) {
            PacketUtil.sendPacketNoEvent(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
            this.grimInvSprinting = true;
            this.grimInvKnown = true;
        }
        return true;
    }

    private void flushGrimSpectateHeld() {
        if (this.grimSpectateHeld.isEmpty()) {
            this.grimSpectateFlagged = false;
            return;
        }
        if (mc.getNetHandler() == null) {
            this.grimSpectateHeld.clear();
            this.grimSpectateFlagged = false;
            return;
        }
        for (Packet<?> held : this.grimSpectateHeld) {
            if (!this.grimSpectateHeld.remove(held)) {
                continue;
            }
            try {
                if (held instanceof S08PacketPlayerPosLook) {
                    mc.getNetHandler().handlePlayerPosLook((S08PacketPlayerPosLook) held);
                } else if (held instanceof S32PacketConfirmTransaction) {
                    mc.getNetHandler().handleConfirmTransaction((S32PacketConfirmTransaction) held);
                }
            } catch (Throwable ignored) {
            }
        }
        this.grimSpectateFlagged = false;
    }

    @EventTarget
    public void onSprintEvent(SprintEvent event) {
        if (!this.isAdvancedActive() || mc.thePlayer == null) {
            return;
        }
        if (this.advSprint.getValue()) {
            mc.thePlayer.setSprinting(false);
        }
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (!this.isAdvancedActive() || mc.thePlayer == null) {
            return;
        }
        if (this.advSpectate.getValue()) {
            PacketUtil.sendPacketNoEvent(new C18PacketSpectate(mc.thePlayer.getUniqueID()));
        }
        if (this.advInput.getValue()) {
            PacketUtil.sendPacketNoEvent(new C0CPacketInput(0.98F, 0.0F, true, true));
        }
    }

    private void handleTimerPacket(PacketEvent event, Packet<?> packet) {
        if (packet instanceof C03PacketPlayer) {
            C03PacketPlayer c03 = (C03PacketPlayer) packet;
            if (!c03.isMoving() && !c03.getRotating()) {
                event.setCancelled(true);
                return;
            }
        }
        if (System.currentTimeMillis() - this.timerHoldStartMs <= TIMER_HOLD_WINDOW_MS) {
            if (packet instanceof C0FPacketConfirmTransaction || packet instanceof C00PacketKeepAlive) {
                event.setCancelled(true);
                this.timerHeldPackets.add(packet);
            }
        } else {
            this.flushTimerPackets();
        }
    }

    private void flushTimerPackets() {
        if (this.timerHeldPackets.isEmpty()) {
            return;
        }
        if (mc.getNetHandler() == null) {
            this.timerHeldPackets.clear();
            return;
        }
        for (Packet<?> held : this.timerHeldPackets) {
            if (this.timerHeldPackets.remove(held)) {
                PacketUtil.sendPacketNoEvent(held);
            }
        }
    }

    private void handleInvPacket(Packet<?> packet) {
        if (packet instanceof C0EPacketClickWindow) {
            C0EPacketClickWindow click = (C0EPacketClickWindow) packet;
            int clickMode = click.getMode();
            boolean allowed = clickMode == CLICK_MODE_QUICK_MOVE
                    || clickMode == CLICK_MODE_SWAP
                    || clickMode == CLICK_MODE_THROW;
            if (click.getWindowId() == mc.thePlayer.inventoryContainer.windowId && allowed) {
                PacketUtil.sendPacket(new C0DPacketCloseWindow(click.getWindowId()));
            } else {
                this.invShouldBlink = true;
            }
        } else if (packet instanceof C0DPacketCloseWindow
                && AccessorC0DPacketCloseWindow.getWindowId((C0DPacketCloseWindow) packet)
                == mc.thePlayer.inventoryContainer.windowId) {
            this.invShouldBlink = false;
        }
    }

    private boolean handleGrimSpectateReceive(PacketEvent event) {
        Packet<?> packet = event.getPacket();
        if (mc.thePlayer == null) {
            return false;
        }
        if (mc.thePlayer.ticksExisted < 20) {
            this.grimSpectateHeld.clear();
            return false;
        }
        if (packet instanceof S08PacketPlayerPosLook) {
            if (mc.thePlayer.capabilities.isFlying && !this.grimSpectateFlagged) {
                this.grimSpectateFlagged = true;
            } else if (this.grimSpectateFlagged) {
                this.grimSpectateHeld.add(packet);
                event.setCancelled(true);
                return true;
            }
        } else if (this.grimSpectateFlagged && packet instanceof S32PacketConfirmTransaction) {
            this.grimSpectateHeld.add(packet);
            event.setCancelled(true);
            PacketUtil.sendPacketNoEvent(new C0FPacketConfirmTransaction(0, (short) 0, true));
            return true;
        }
        return false;
    }

    @EventTarget(Priority.HIGHEST)
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || event.isCancelled()) {
            return;
        }
        if (event.getType() == EventType.RECEIVE
                && this.isAdvancedActive()
                && this.advGrimSpectate.getValue()
                && this.handleGrimSpectateReceive(event)) {
            return;
        }
        if (this.mode.getValue() != MODE_WATCHDOG) {
            return;
        }
        if (event.getType() != EventType.RECEIVE
                || !(event.getPacket() instanceof S3FPacketCustomPayload)) {
            return;
        }

        S3FPacketCustomPayload payload = (S3FPacketCustomPayload) event.getPacket();
        String channel = payload.getChannelName();
        if (channel == null) {
            return;
        }

        if (this.hypixelBrand.getValue()) {
            if (MOD_CONTROL_CHANNEL.equalsIgnoreCase(channel)) {
                event.setCancelled(true);
                return;
            }
            String lower = channel.toLowerCase();
            for (String prefix : HYPIXEL_PREFIXES) {
                if (lower.startsWith(prefix)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        if (!BRAND_CHANNEL.equals(channel)) {
            return;
        }

        String replacement = this.brand.getValue();
        if (replacement == null || replacement.isEmpty()) {
            return;
        }
        try {

            PacketBuffer data = payload.getBufferData();
            data.clear();
            data.writeString(replacement);
        } catch (Throwable readOnlyOrTooSmall) {

            event.setCancelled(true);
        }
    }
}
