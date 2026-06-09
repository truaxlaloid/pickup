package net.johnseagull.pickup;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = Pickup.MODID)
public class ClientEvents {

    private static void redirectHitResultIfNeeded() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null && mc.hitResult != null) {
            if (mc.hitResult.getType() == HitResult.Type.ENTITY) {
                EntityHitResult entityHit = (EntityHitResult) mc.hitResult;
                if (entityHit.getEntity() instanceof ItemEntity) {
                    if (PickupConfig.HIT_THROUGH_ITEMS.get()) {
                        // Perform a block raycast ignoring entities to target straight through the item
                        double range = mc.player.blockInteractionRange();
                        Vec3 eyePos = mc.player.getEyePosition();
                        Vec3 end = eyePos.add(mc.player.getLookAngle().scale(range));
                        
                        BlockHitResult blockHit = mc.level.clip(new ClipContext(
                                eyePos, end,
                                ClipContext.Block.OUTLINE,
                                ClipContext.Fluid.NONE,
                                mc.player
                        ));
                        
                        // Redirect target to the block behind the item
                        mc.hitResult = blockHit;
                    }
                }
            }
        }
    }

    // Handles the initial attack key press (Click / Key Bind)
    @SubscribeEvent
    public static void onClickInput(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isAttack()) { 
            redirectHitResultIfNeeded();
        }
    }

    // Handles continuous holding of the attack key (e.g., holding left-click to mine)
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.keyAttack.isDown()) {
            redirectHitResultIfNeeded();
        }
    }
}
