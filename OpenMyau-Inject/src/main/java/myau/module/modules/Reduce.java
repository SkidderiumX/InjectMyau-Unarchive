package myau.module.modules;

import myau.Myau;
import myau.module.Module;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

/**
 * Ported from Raven B4 Reduce.
 * Takes over KeepSprint's attack-motion handling while enabled.
 */
public class Reduce extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final FloatProperty reduction = new FloatProperty("Attack reduction", 60.0F, 60.0F, 100.0F, 0.5F);
    public final IntProperty chance = new IntProperty("Chance", 100, 0, 100);

    public Reduce() {
        super("Reduce", false);
    }

    public static void reduce(Entity entity) {
        Reduce reduce = (Reduce) Myau.moduleManager.modules.get(Reduce.class);
        if (reduce == null || !reduce.isEnabled() || mc.thePlayer == null) {
            return;
        }
        if (reduce.chance.getValue() == 0) {
            return;
        }
        if (reduce.chance.getValue() != 100
                && Math.random() >= reduce.chance.getValue() / 100.0D) {
            mc.thePlayer.motionX *= 0.6D;
            mc.thePlayer.motionZ *= 0.6D;
            return;
        }
        double factor = (100.0D - reduce.reduction.getValue()) / 100.0D;
        mc.thePlayer.motionX *= factor;
        mc.thePlayer.motionZ *= factor;
    }
}
