package myau.access;

import myau.inject.MappingBridge;
import net.minecraft.network.play.server.S27PacketExplosion;

import java.lang.reflect.Field;

public final class AccessorExplosion {
    private static final String OWNER = "net.minecraft.network.play.server.S27PacketExplosion";
    private static final Field F_X =
            MappingBridge.field(OWNER, "posX", double.class);
    private static final Field F_Y =
            MappingBridge.field(OWNER, "posY", double.class);
    private static final Field F_Z =
            MappingBridge.field(OWNER, "posZ", double.class);
    private AccessorExplosion() {
    }
    public static void scale(S27PacketExplosion owner, double horizontal, double vertical) {
        try {
            F_X.setDouble(owner, F_X.getDouble(owner) * horizontal);
            F_Y.setDouble(owner, F_Y.getDouble(owner) * vertical);
            F_Z.setDouble(owner, F_Z.getDouble(owner) * horizontal);
        } catch (Throwable t) {
            Access.report(OWNER, "pos", t);
        }
    }
}
