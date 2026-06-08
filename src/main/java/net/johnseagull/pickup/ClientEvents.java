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
import net.neoforged.neoforge.client.event.InputEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = Pickup.MODID)
public class ClientEvents {

    @SubscribeEvent
    public static void onClickInput(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isAttack()) { // Left-click / Attack key binding
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.level != null && mc.hitResult != null) {
                if (mc.hitResult.getType() == HitResult.Type.ENTITY) {
                    EntityHitResult entityHit = (EntityHitResult) mc.hitResult;
                    if (entityHit.getEntity() instanceof ItemEntity) {
                        if (PickupConfig.HIT_THROUGH_ITEMS.get()) {
                            // Perform a block raycast ignoring entities to hit through the item
                            double range = mc.player.blockInteractionRange();
                            Vec3 eyePos = mc.player.getEyePosition();
                            Vec3 end = eyePos.add(mc.player.getLookAngle().scale(range));
                            
                            BlockHitResult blockHit = mc.level.clip(new ClipContext(
                                    eyePos, end,
                                    ClipContext.Block.OUTLINE,
                                    ClipContext.Fluid.NONE,
                                    mc.player
                            ));
                            
                            // Redirect the targeted hit result to the block (or a MISS if empty space is behind it)
                            mc.hitResult = blockHit;
                        }
                    }
                }
            }
        }
    }
}
