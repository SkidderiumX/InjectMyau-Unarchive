package myau.access;

import myau.inject.MappingBridge;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class AccessorItemRenderer {
    private static final String OWNER = "net.minecraft.client.renderer.ItemRenderer";
    private static final Field F_EQUIPPED =
            MappingBridge.field(OWNER, "equippedProgress", float.class);
    private static final Field F_PREV_EQUIPPED =
            MappingBridge.field(OWNER, "prevEquippedProgress", float.class);
    private static final Field F_ITEM =
            MappingBridge.field(OWNER, "itemToRender", ItemStack.class);
    private static final Method M_TRANSFORM =
            MappingBridge.method(OWNER, "transformFirstPersonItem", float.class, float.class);
    private static final Method M_BLOCK =
            MappingBridge.method(OWNER, "doBlockTransformations");
    private AccessorItemRenderer() {
    }
    public static float getEquippedProgress(Object owner) {
        try {
            return F_EQUIPPED.getFloat(owner);
        } catch (Throwable t) {
            Access.report(OWNER, "equippedProgress", t);
            return 1.0F;
        }
    }
    public static float getPrevEquippedProgress(Object owner) {
        try {
            return F_PREV_EQUIPPED.getFloat(owner);
        } catch (Throwable t) {
            Access.report(OWNER, "prevEquippedProgress", t);
            return 1.0F;
        }
    }
    public static ItemStack getItemToRender(Object owner) {
        try {
            return (ItemStack) F_ITEM.get(owner);
        } catch (Throwable t) {
            Access.report(OWNER, "itemToRender", t);
            return null;
        }
    }
    public static void resetEquippedProgress(Object owner) {
        try {
            F_PREV_EQUIPPED.setFloat(owner, 0.0F);
            F_EQUIPPED.setFloat(owner, 0.0F);
        } catch (Throwable t) {
            Access.report(OWNER, "equippedProgress", t);
        }
    }
    public static void callTransformFirstPersonItem(Object owner, float equipProgress, float swingProgress) {
        try {
            M_TRANSFORM.invoke(owner, equipProgress, swingProgress);
        } catch (Throwable t) {
            Access.report(OWNER, "transformFirstPersonItem", t);
        }
    }
    public static void callDoBlockTransformations(Object owner) {
        try {
            M_BLOCK.invoke(owner);
        } catch (Throwable t) {
            Access.report(OWNER, "doBlockTransformations", t);
        }
    }
}
