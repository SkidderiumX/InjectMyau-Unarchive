// Original logic by syuto/animations-1.6, integrated into Uzi
package myau.mixin;

import myau.util.ItemAnimations;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@SideOnly(Side.CLIENT)
@Mixin(value = EntityLivingBase.class, priority = 999)
public abstract class MixinEntityLivingBaseAnimations {

    /**
     * @author animations-1.6 (syuto), integrated by Uzi
     * @reason Custom swing speed
     */
    @Overwrite
    private int getArmSwingAnimationEnd() {
        return ItemAnimations.swingAnimationEnd();
    }
}
