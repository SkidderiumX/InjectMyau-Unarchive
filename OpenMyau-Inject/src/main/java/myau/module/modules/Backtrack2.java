package myau.module.modules;

import java.awt.Color;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import myau.Myau;
import myau.access.AccessorRenderManager;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.PacketEvent;
import myau.events.Render3DEvent;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.util.RenderUtil;
import myau.util.RotationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S13PacketDestroyEntities;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

/**
 * Port of the original Myau "BackTrack" module.
 *
 * Delays the target's movement packets (S14/S18) so the client keeps seeing a
 * "ghost" at an older position while you attack it - the server's lag
 * compensation then registers hits on the ghost. The ghost only tracks while
 * you keep attacking ("Only during combat"), resets when the target moves too
 * fast, dies/teleports away, or after 7 ticks without an attack, and movement
 * packets are released once "Latency delay" has passed.
 */
public class Backtrack2 extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final IntProperty latency = new IntProperty("Latency delay", 200, 1, 1000);
    public final FloatProperty enemyDistance = new FloatProperty("Enemy distance", 6.0F, 3.1F, 6.0F);
    public final BooleanProperty onlyCombat = new BooleanProperty("Only during combat", true);
    public final BooleanProperty predictPosition = new BooleanProperty("Render prediction", true);
    public final BooleanProperty useThemeColor = new BooleanProperty("Use theme color", false);
    public final IntProperty boxColor = new IntProperty("Box color [H]", 0, 0, 360);
    public final BooleanProperty disableOnWorldChange = new BooleanProperty("Disable on world change", false);
    public final BooleanProperty disableOnDisconnect = new BooleanProperty("Disable on disconnect", false);

    private final Queue<TimedPacket> packetQueue = new ConcurrentLinkedQueue<TimedPacket>();
    private Vec3 vec3;
    private Vec3 lastVec3;
    private EntityPlayer target;
    private int attackTicks;
    private final LinkedList<Vec3> positionHistory = new LinkedList<Vec3>();
    private final int maxHistorySize = 10;
    private final LinkedList<Vec3> recentPositions = new LinkedList<Vec3>();
    private static final int FAST_MOVE_CHECK_TICKS = 5;
    private static final double FAST_MOVE_THRESHOLD = 5.0;
    private boolean moduleActive = false;
    private Object lastWorld = null;
    private boolean hadConnection = false;

    public Backtrack2() {
        super("Backtrack2", false);
    }

    @Override
    public void onEnabled() {
        this.packetQueue.clear();
        this.positionHistory.clear();
        this.recentPositions.clear();
        this.vec3 = this.lastVec3 = null;
        this.target = null;
        this.attackTicks = 0;
        this.moduleActive = true;
    }

    @Override
    public void onDisabled() {
        this.moduleActive = false;
        this.releaseAllImmediately();
        this.positionHistory.clear();
        this.recentPositions.clear();
        this.target = null;
        this.vec3 = this.lastVec3 = null;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() == EventType.PRE && this.moduleActive) {
            // World change / connection-loss handling (the original used Forge
            // events; these are tracked manually so both runtimes behave alike).
            if (mc.getNetHandler() != null) {
                this.hadConnection = true;
            } else if (this.hadConnection) {
                this.hadConnection = false;
                this.releaseAllImmediately();
                this.resetTarget();
                if (this.disableOnDisconnect.getValue()) {
                    this.toggle();
                    return;
                }
            }
            if (this.lastWorld != mc.theWorld) {
                boolean changed = this.lastWorld != null && mc.theWorld != null;
                this.lastWorld = mc.theWorld;
                this.releaseAllImmediately();
                this.resetTarget();
                if (changed && this.disableOnWorldChange.getValue()) {
                    this.toggle();
                    return;
                }
            }

            ++this.attackTicks;

            try {
                if (this.target != null) {
                    if (this.target.isDead || this.target.getHealth() <= 0.0F) {
                        this.resetTarget();
                        return;
                    }
                    Vec3 currentPos = this.target.getPositionVector();
                    this.recentPositions.add(
                            new Vec3(currentPos.xCoord, currentPos.yCoord, currentPos.zCoord));
                    if (this.recentPositions.size() > FAST_MOVE_CHECK_TICKS) {
                        this.recentPositions.removeFirst();
                    }

                    if (this.recentPositions.size() == FAST_MOVE_CHECK_TICKS) {
                        Vec3 oldestPos = this.recentPositions.getFirst();
                        if (oldestPos.squareDistanceTo(currentPos) > FAST_MOVE_THRESHOLD) {
                            this.resetTarget();
                            return;
                        }
                    }

                    this.positionHistory.add(
                            new Vec3(currentPos.xCoord, currentPos.yCoord, currentPos.zCoord));
                    if (this.positionHistory.size() > this.maxHistorySize) {
                        this.positionHistory.removeFirst();
                    }

                    if (this.attackTicks > 7
                            || this.vec3.squareDistanceTo(mc.thePlayer.getPositionVector())
                            > Math.pow((double) (Float) this.enemyDistance.getValue(), 2.0)) {
                        this.resetTarget();
                    }

                    this.lastVec3 = this.vec3;
                }
            } catch (Exception ignored) {
            }

            this.processDuePackets();

            if (this.packetQueue.isEmpty() && this.target != null) {
                this.vec3 = this.target.getPositionVector();
            }

        }
    }

    private void processDuePackets() {
        long maxDelay = (long) (Integer) this.latency.getValue();

        TimedPacket timedPacket;
        while ((timedPacket = this.packetQueue.peek()) != null) {
            if (System.currentTimeMillis() - timedPacket.timestamp < maxDelay) {
                break;
            }

            this.packetQueue.poll();
            this.processPacketLocally(timedPacket.packet);
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (this.moduleActive && this.predictPosition.getValue() && this.target != null
                && this.lastVec3 != null && this.vec3 != null) {
            double x = RenderUtil.lerpDouble(this.vec3.xCoord, this.lastVec3.xCoord,
                    (double) event.getPartialTicks());
            double y = RenderUtil.lerpDouble(this.vec3.yCoord, this.lastVec3.yCoord,
                    (double) event.getPartialTicks());
            double z = RenderUtil.lerpDouble(this.vec3.zCoord, this.lastVec3.zCoord,
                    (double) event.getPartialTicks());
            double renderX = x - AccessorRenderManager.getRenderPosX(mc.getRenderManager());
            double renderY = y - AccessorRenderManager.getRenderPosY(mc.getRenderManager());
            double renderZ = z - AccessorRenderManager.getRenderPosZ(mc.getRenderManager());
            float borderSize = this.target.getCollisionBorderSize();
            AxisAlignedBB aabb = new AxisAlignedBB(
                    renderX - (double) this.target.width / 2.0,
                    renderY,
                    renderZ - (double) this.target.width / 2.0,
                    renderX + (double) this.target.width / 2.0,
                    renderY + (double) this.target.height,
                    renderZ + (double) this.target.width / 2.0)
                    .expand((double) borderSize, (double) borderSize, (double) borderSize);
            int colorInt;
            if (this.useThemeColor.getValue()) {
                Module hud = Myau.moduleManager.getModule(HUD.class);
                if (hud instanceof HUD) {
                    colorInt = ((HUD) hud).getColor(System.currentTimeMillis()).getRGB();
                } else {
                    colorInt = Color.WHITE.getRGB();
                }
            } else {
                colorInt = Color.HSBtoRGB((float) (Integer) this.boxColor.getValue() / 360.0F,
                        1.0F, 1.0F);
            }

            Color color = new Color(colorInt);
            RenderUtil.enableRenderState();
            RenderUtil.drawFilledBox(aabb, color.getRed(), color.getGreen(), color.getBlue());
            RenderUtil.disableRenderState();
        }
    }

    private Vec3 getSmoothedPosition(float partialTicks) {
        if (this.positionHistory.isEmpty()) {
            return new Vec3(
                    this.lastVec3.xCoord + (this.vec3.xCoord - this.lastVec3.xCoord) * (double) partialTicks,
                    this.lastVec3.yCoord + (this.vec3.yCoord - this.lastVec3.yCoord) * (double) partialTicks,
                    this.lastVec3.zCoord + (this.vec3.zCoord - this.lastVec3.zCoord) * (double) partialTicks);
        } else {
            double totalWeight = 0.0;
            double x = 0.0;
            double y = 0.0;
            double z = 0.0;

            for (int i = 0; i < this.positionHistory.size(); ++i) {
                double weight = (double) (i + 1) / (double) this.positionHistory.size();
                Vec3 pos = this.positionHistory.get(i);
                x += pos.xCoord * weight;
                y += pos.yCoord * weight;
                z += pos.zCoord * weight;
                totalWeight += weight;
            }

            double currentWeight = 1.5;
            x += this.vec3.xCoord * currentWeight;
            y += this.vec3.yCoord * currentWeight;
            z += this.vec3.zCoord * currentWeight;
            totalWeight += currentWeight;
            x /= totalWeight;
            y /= totalWeight;
            z /= totalWeight;
            return new Vec3(x, y, z);
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.moduleActive && !event.isCancelled()) {
            Packet<?> p = event.getPacket();
            if (event.getType() == EventType.RECEIVE) {
                try {
                    if (mc.thePlayer == null || mc.thePlayer.ticksExisted < 20) {
                        this.packetQueue.clear();
                        this.positionHistory.clear();
                        this.recentPositions.clear();
                        return;
                    }

                    if (this.target == null) {
                        return;
                    }

                    boolean ghostUpdated = false;
                    Vec3 newGhost = null;
                    if (p instanceof S14PacketEntity) {
                        S14PacketEntity wrapper = (S14PacketEntity) p;
                        Entity entity = wrapper.getEntity(mc.theWorld);
                        if (entity != null && entity.getEntityId() == this.target.getEntityId()) {
                            Vec3 base = this.vec3 != null ? this.vec3 : this.target.getPositionVector();
                            newGhost = base.addVector(
                                    (double) wrapper.func_149062_c() / 32.0,
                                    (double) wrapper.func_149061_d() / 32.0,
                                    (double) wrapper.func_149064_e() / 32.0);
                            ghostUpdated = true;
                        }
                    } else if (p instanceof S18PacketEntityTeleport) {
                        S18PacketEntityTeleport wrapper = (S18PacketEntityTeleport) p;
                        if (wrapper.getEntityId() == this.target.getEntityId()) {
                            newGhost = new Vec3(
                                    (double) wrapper.getX() / 32.0,
                                    (double) wrapper.getY() / 32.0,
                                    (double) wrapper.getZ() / 32.0);
                            ghostUpdated = true;
                        }
                    } else if (p instanceof S13PacketDestroyEntities) {
                        S13PacketDestroyEntities wrapper = (S13PacketDestroyEntities) p;
                        int[] ids = wrapper.getEntityIDs();
                        for (int id : ids) {
                            if (id == this.target.getEntityId()) {
                                this.resetTarget();
                                return;
                            }
                        }
                    } else if (p instanceof S08PacketPlayerPosLook) {
                        // Server corrected our position - drop the ghost.
                        this.releaseAllImmediately();
                        return;
                    }

                    if (ghostUpdated) {
                        Vec3 previousGhost = this.vec3 != null ? this.vec3 : newGhost;
                        double previousDist = RotationUtil.distanceToBoxAt(this.target, previousGhost);
                        double newDist = RotationUtil.distanceToBoxAt(this.target, newGhost);
                        this.vec3 = newGhost;
                        if (newDist > previousDist + 0.001) {
                            // The ghost extends away from us - hold the packet so
                            // the target keeps lagging behind.
                            this.packetQueue.add(new TimedPacket(p, System.currentTimeMillis()));
                            event.setCancelled(true);
                        } else {
                            // The target is closing in or standing still - a ghost
                            // in front of the real position would make us whiff,
                            // so release everything and let this packet through.
                            this.releaseAllImmediately();
                        }
                    } else if (!this.packetQueue.isEmpty()) {
                        // While backtracking, hold every other packet too so the
                        // released order stays exactly the server order.
                        this.packetQueue.add(new TimedPacket(p, System.currentTimeMillis()));
                        event.setCancelled(true);
                    }
                } catch (Exception ignored) {
                }
            } else if (event.getType() == EventType.SEND && p instanceof C02PacketUseEntity) {
                C02PacketUseEntity wrapper = (C02PacketUseEntity) p;
                if (this.onlyCombat.getValue() && wrapper.getAction() != Action.ATTACK) {
                    return;
                }

                Entity entity = wrapper.getEntityFromWorld(mc.theWorld);
                if (entity instanceof EntityPlayer) {
                    EntityPlayer player = (EntityPlayer) entity;
                    if (this.target != null && player.getEntityId() == this.target.getEntityId()) {
                        this.attackTicks = 0;
                        return;
                    }

                    this.target = player;
                    this.vec3 = this.lastVec3 = player.getPositionVector();
                    this.positionHistory.clear();
                    this.recentPositions.clear();
                    this.positionHistory.add(
                            new Vec3(this.vec3.xCoord, this.vec3.yCoord, this.vec3.zCoord));
                    this.recentPositions.add(
                            new Vec3(this.vec3.xCoord, this.vec3.yCoord, this.vec3.zCoord));
                    this.attackTicks = 0;
                }
            }

        }
    }

    private void resetTarget() {
        this.target = null;
        this.vec3 = this.lastVec3 = null;
        this.positionHistory.clear();
        this.recentPositions.clear();
        this.releaseAllImmediately();
    }

    private void releaseAllImmediately() {
        if (!this.packetQueue.isEmpty()) {
            Queue<TimedPacket> tempQueue = new ConcurrentLinkedQueue<TimedPacket>(this.packetQueue);
            this.packetQueue.clear();
            Iterator<TimedPacket> iterator = tempQueue.iterator();

            while (iterator.hasNext()) {
                TimedPacket tp = iterator.next();

                try {
                    this.processPacketLocallyImmediately(tp.packet);
                } catch (Exception ignored) {
                }
            }
        }

    }

    @SuppressWarnings("unchecked")
    private void processPacketLocallyImmediately(Packet<?> packet) {
        if (mc.theWorld != null && mc.getNetHandler() != null) {
            try {
                ((Packet<net.minecraft.network.play.INetHandlerPlayClient>) packet)
                        .processPacket(mc.getNetHandler());
            } catch (Exception ignored) {
            }

        }
    }

    @SuppressWarnings("unchecked")
    private void processPacketLocally(Packet<?> packet) {
        if (mc.theWorld != null && mc.getNetHandler() != null) {
            try {
                ((Packet<net.minecraft.network.play.INetHandlerPlayClient>) packet)
                        .processPacket(mc.getNetHandler());
            } catch (Exception ignored) {
            }

        }
    }

    private static class TimedPacket {
        public final Packet<?> packet;
        public final long timestamp;

        public TimedPacket(Packet<?> packet, long timestamp) {
            this.packet = packet;
            this.timestamp = timestamp;
        }
    }
}
