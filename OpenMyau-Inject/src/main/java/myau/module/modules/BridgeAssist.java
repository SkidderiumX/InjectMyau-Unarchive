package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.MoveInputEvent;
import myau.events.PacketEvent;
import myau.events.UpdateEvent;
import myau.management.EdgeAssist;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;

public class BridgeAssist extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final BooleanProperty prePlace = new BooleanProperty("pre-place", false);
    public final FloatProperty edgeOffset = new FloatProperty("edge-offset", 0.0F, 0.0F, 0.3F);
    public final IntProperty unsneakDelay = new IntProperty("unsneak-delay", 50, 50, 300);
    public final IntProperty sneakOnJump = new IntProperty("sneak-on-jump", 0, 0, 500);
    public final BooleanProperty sneakKeyPressed = new BooleanProperty("sneak-key-pressed", false);
    public final BooleanProperty holdingBlocks = new BooleanProperty("holding-blocks", false);
    public final BooleanProperty lookingDown = new BooleanProperty("looking-down", false);
    public final BooleanProperty notMovingForward = new BooleanProperty("not-moving-forward", false);
    private final EdgeAssist edgeAssist = new EdgeAssist(new EdgeAssist.Settings() {
        @Override
        public boolean prePlace() {
            return BridgeAssist.this.prePlace.getValue();
        }
        @Override
        public float edgeOffset() {
            return BridgeAssist.this.edgeOffset.getValue();
        }
        @Override
        public int unsneakDelay() {
            return BridgeAssist.this.unsneakDelay.getValue();
        }
        @Override
        public int sneakOnJump() {
            return BridgeAssist.this.sneakOnJump.getValue();
        }
        @Override
        public boolean sneakKeyPressed() {
            return BridgeAssist.this.sneakKeyPressed.getValue();
        }
        @Override
        public boolean holdingBlocks() {
            return BridgeAssist.this.holdingBlocks.getValue();
        }
        @Override
        public boolean lookingDown() {
            return BridgeAssist.this.lookingDown.getValue();
        }
        @Override
        public boolean notMovingForward() {
            return BridgeAssist.this.notMovingForward.getValue();
        }
    });
    public BridgeAssist() {
        super("BridgeAssist", false);
    }
    @Override
    public String[] getSuffix() {
        float offset = this.edgeOffset.getValue();
        return new String[]{offset == Math.rint(offset)
                ? String.valueOf((int) offset)
                : String.format("%.2f", offset)};
    }
    @Override
    public void onDisabled() {
        this.edgeAssist.reset();
    }
    /**
     * While Scaffold's sneak-mode is NORMAL, its own EdgeAssist instance drives the
     * sneak key, so this module stays idle to avoid fighting over the same inputs.
     */
    private boolean scaffoldOwnsSneak() {
        Scaffold scaffold = (Scaffold) Myau.moduleManager.modules.get(Scaffold.class);
        return scaffold != null && scaffold.isEnabled() && scaffold.sneakMode.getValue() == 1;
    }
    @EventTarget(Priority.LOWEST)
    public void onMoveInput(MoveInputEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }
        if (this.scaffoldOwnsSneak()) {
            return;
        }
        if (mc.currentScreen != null || mc.thePlayer.capabilities.isFlying) {
            return;
        }
        this.edgeAssist.handleMoveInput();
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.SEND) {
            return;
        }
        if (this.scaffoldOwnsSneak()) {
            return;
        }
        if (!(event.getPacket() instanceof C08PacketPlayerBlockPlacement)) {
            return;
        }
        this.edgeAssist.handlePlacement((C08PacketPlayerBlockPlacement) event.getPacket());
    }
    @EventTarget(Priority.LOW)
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE) {
            return;
        }
        if (this.scaffoldOwnsSneak()) {
            return;
        }
        this.edgeAssist.handlePrePlace(event);
    }
}
