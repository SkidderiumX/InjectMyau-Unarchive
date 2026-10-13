// Original logic by syuto/animations-1.6, integrated into Uzi
package myau.mixin;

import myau.config.AnimationConfig;
import myau.util.ItemAnimations;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(value = ItemRenderer.class, priority = 999)
public abstract class MixinItemRendererAnimations {

    @Redirect(
            method = "renderItemInFirstPerson",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemRenderer;transformFirstPersonItem(FF)V",
                    ordinal = 2)
    )
    private void skipTransform(ItemRenderer instance, float f1, float f2) {
        // Suppressed — animations replaces this below
    }

    @Inject(
            method = "renderItemInFirstPerson",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemRenderer;doBlockTransformations()V")
    )
    public void applyAnimTransform(float partialTicks, CallbackInfo ci) {
        if (!AnimationConfig.isEnabled()) {
            return;
        }
        ItemAnimations.applyFirstPersonTransforms(ItemAnimations.wrap(this), partialTicks);
    }

    @Redirect(
            method = "renderItemInFirstPerson",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemRenderer;transformFirstPersonItem(FF)V",
                    ordinal = 4)
    )
    private void skipNormalTransform(ItemRenderer instance, float f1, float f2) {
        if (!AnimationConfig.isEnabled()
                || !ItemAnimations.shouldApplyNormalAnimations(ItemAnimations.wrap(instance))) {
            ItemAnimations.wrap(instance).transformFirstPersonItem(f1, f2);
        }
    }

    @Inject(
            method = "renderItemInFirstPerson",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemRenderer;" +
                            "renderItem(Lnet/minecraft/entity/EntityLivingBase;" +
                            "Lnet/minecraft/item/ItemStack;" +
                            "Lnet/minecraft/client/renderer/block/model/ItemCameraTransforms$TransformType;)V",
                    shift = At.Shift.BEFORE)
    )
    public void applyScale(float partialTicks, CallbackInfo ci) {
        if (!AnimationConfig.isEnabled()) {
            return;
        }
        ItemAnimations.applyFirstPersonScale(ItemAnimations.wrap(this), partialTicks);
    }
}
