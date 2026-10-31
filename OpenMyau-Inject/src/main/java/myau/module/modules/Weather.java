package myau.module.modules;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.PacketEvent;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.network.play.server.S03PacketTimeUpdate;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.WorldInfo;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Port of the raven bS "Weather" module.
 *
 * - "time" overrides the client world time (0 = vanilla) and cancels the
 *   server's S03 time packets so the override sticks; the real time is kept
 *   and restored on disable / world change.
 * - "rain" forces the rain render state on (also flipping the integrated
 *   server's world info when playing single player) and restores it after.
 * - "lightning" spawns client-side lightning bolts on active chunks with the
 *   chosen frequency (Vanilla = the real 1/100000 tick chance, up to Chaotic).
 *
 * The original hooks Forge's WorldEvent.Unload and re-reads settings through
 * guiUpdate(); here the world is tracked per tick instead and the mode is
 * re-read every tick, which behaves the same in both runtimes.
 */
public class Weather extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final Random RANDOM = new Random();
    private static Field activeChunkSet;
    private static Field updateLCG;

    public final FloatProperty time = new FloatProperty("time", 0.0F, 0.0F, 24.0F, 0.3F);
    public final ModeProperty lightning = new ModeProperty("lightning", 0,
            new String[]{"Disabled", "Vanilla", "Uncommon", "Common", "Frequent", "Intense", "Chaotic"});
    public final BooleanProperty rain = new BooleanProperty("rain", false);

    private WorldClient weatherWorld;
    private WorldInfo rainInfo;
    private long savedTime = -9L;
    private long savedRain = -1L;
    private boolean savedDaylightCycle;
    private boolean savedServerRain;
    private float savedRainStrength;
    private long lastThunder;

    public Weather() {
        super("Weather", false);
    }

    @Override
    public void onEnabled() {
        this.synchronizeWorld();
    }

    @Override
    public void onDisabled() {
        this.restoreTime();
        this.restoreRain();
        this.weatherWorld = null;
        this.lastThunder = 0L;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() != EventType.PRE || !this.isEnabled()) {
            return;
        }
        this.synchronizeWorld();
        if (this.weatherWorld == null) {
            return;
        }
        this.updateTime();
        this.updateRain();
        this.strike();
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || this.time.getValue() == 0.0F
                || !(event.getPacket() instanceof S03PacketTimeUpdate) || mc.theWorld == null) {
            return;
        }
        S03PacketTimeUpdate packet = (S03PacketTimeUpdate) event.getPacket();
        WorldClient world = mc.theWorld;
        event.setCancelled(true);
        Runnable update = () -> {
            if (mc.theWorld != world) {
                return;
            }
            world.setTotalWorldTime(packet.getTotalWorldTime());
            if (!Weather.this.isEnabled() || Weather.this.time.getValue() == 0.0F) {
                world.setWorldTime(packet.getWorldTime());
                Weather.this.savedTime = -9L;
                return;
            }
            Weather.this.synchronizeWorld();
            Weather.this.savedTime = packet.getWorldTime();
            Weather.this.savedDaylightCycle = Weather.this.savedTime >= 0L;
        };
        if (mc.isCallingFromMinecraftThread()) {
            update.run();
        } else {
            mc.addScheduledTask(update);
        }
    }

    private void synchronizeWorld() {
        if (this.weatherWorld != mc.theWorld) {
            this.restoreTime();
            this.restoreRain();
            this.weatherWorld = mc.theWorld;
            this.lastThunder = 0L;
        }
    }

    private void updateTime() {
        if (this.time.getValue() != 0.0F) {
            if (this.savedTime == -9L) {
                this.savedTime = this.weatherWorld.getWorldTime();
                this.savedDaylightCycle = this.weatherWorld.getGameRules().getBoolean("doDaylightCycle");
            }
            this.weatherWorld.setWorldTime((long) (-1000.0 * (16.0 + (double) this.time.getValue())));
        } else {
            this.restoreTime();
        }
    }

    private void restoreTime() {
        if (this.savedTime != -9L && this.weatherWorld != null) {
            this.weatherWorld.setWorldTime(this.savedTime);
            this.weatherWorld.getGameRules().setOrCreateGameRule("doDaylightCycle",
                    Boolean.toString(this.savedDaylightCycle));
        }
        this.savedTime = -9L;
    }

    private void updateRain() {
        if (this.rain.getValue()) {
            if (this.savedRain == -1L) {
                this.savedRain = this.weatherWorld.getWorldInfo().isRaining() ? 1L : 0L;
                this.savedRainStrength = this.weatherWorld.getRainStrength(1.0f);
                this.rainInfo = this.worldInfo();
                this.savedServerRain = this.rainInfo.isRaining();
            }
            this.rainInfo.setRaining(true);
            this.weatherWorld.getWorldInfo().setRaining(true);
            this.weatherWorld.setRainStrength(1.0f);
        } else {
            this.restoreRain();
        }
    }

    private void restoreRain() {
        if (this.savedRain != -1L) {
            if (this.rainInfo != null) {
                this.rainInfo.setRaining(this.savedServerRain);
            }
            if (this.weatherWorld != null) {
                this.weatherWorld.getWorldInfo().setRaining(this.savedRain == 1L);
                this.weatherWorld.setRainStrength(this.savedRainStrength);
            }
        }
        this.savedRain = -1L;
        this.rainInfo = null;
    }

    private WorldInfo worldInfo() {
        net.minecraft.server.MinecraftServer server = mc.getIntegratedServer();
        if (server != null && server.worldServers != null && server.worldServers.length > 0) {
            WorldServer world = server.worldServerForDimension(this.weatherWorld.provider.getDimensionId());
            if (world != null) {
                return world.getWorldInfo();
            }
        }
        return this.weatherWorld.getWorldInfo();
    }

    private boolean shouldStrike(int mode) {
        if (mode == 1) {
            // Vanilla
            return RANDOM.nextInt(100000) == 0;
        }
        double d = Math.random();
        switch (mode) {
            case 2: return d > 0.9999; // Uncommon
            case 3: return d > 0.999;  // Common
            case 4: return d > 0.99;   // Frequent
            case 5: return d > 0.9;    // Intense
            case 6: return d > 0.3;    // Chaotic
            default: return false;
        }
    }

    @SuppressWarnings("unchecked")
    private void strike() {
        int mode = this.lightning.getValue();
        if (mode == 0 || mc.isGamePaused()) {
            return;
        }
        try {
            if (activeChunkSet == null) {
                activeChunkSet = net.minecraftforge.fml.relauncher.ReflectionHelper.findField(
                        World.class, "activeChunkSet", "field_72993_I");
                updateLCG = net.minecraftforge.fml.relauncher.ReflectionHelper.findField(
                        World.class, "updateLCG", "field_73005_l");
            }
            long l = System.currentTimeMillis();
            Set<ChunkCoordIntPair> set = (Set<ChunkCoordIntPair>) activeChunkSet.get(mc.theWorld);
            for (ChunkCoordIntPair c : set) {
                if (!this.shouldStrike(mode)) {
                    continue;
                }
                int x = c.chunkXPos * 16;
                int z = c.chunkZPos * 16;
                int n3 = (Integer) updateLCG.get(mc.theWorld);
                n3 = n3 * 3 + 1013904223;
                updateLCG.set(mc.theWorld, n3);
                int n4 = n3 >> 2;
                BlockPos pos = this.target(new BlockPos(x + (n4 & 0xF), 0, z + (n4 >> 8 & 0xF)));
                if (!mc.theWorld.canSeeSky(pos)) {
                    continue;
                }
                mc.theWorld.addWeatherEffect(
                        new EntityLightningBolt(mc.theWorld, pos.getX(), pos.getY(), pos.getZ()));
                if (Math.abs(l - this.lastThunder) <= 250L) {
                    continue;
                }
                mc.theWorld.playSound(pos.getX(), pos.getY(), pos.getZ(),
                        "ambient.weather.thunder", 10.0f, 0.8f + RANDOM.nextFloat() * 0.2f, false);
                this.lastThunder = l;
            }
        } catch (Exception ignored) {
        }
    }

    private BlockPos target(BlockPos blockPos) {
        BlockPos top = mc.theWorld.getPrecipitationHeight(blockPos);
        BlockPos sky = new BlockPos(top.getX(), mc.theWorld.getHeight(), top.getZ());
        AxisAlignedBB box = new AxisAlignedBB(top, sky).expand(3.0, 3.0, 3.0);
        List<EntityLivingBase> list = mc.theWorld.getEntitiesWithinAABB(EntityLivingBase.class, box,
                Weather::canStrike);
        return !list.isEmpty() ? list.get(RANDOM.nextInt(list.size())).getPosition() : top;
    }

    private static boolean canStrike(EntityLivingBase e) {
        return e != null && e.isEntityAlive() && Minecraft.getMinecraft().theWorld.canSeeSky(e.getPosition());
    }
}
